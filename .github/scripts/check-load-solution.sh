#!/usr/bin/env bash
#
# CI checks for the load-solution task of services/process-application (catch-up, GitHub issue #95).
#
#   check-load-solution.sh baseline
#       templates/process-application-starter/ (src/ + pom.xml) must be a byte copy of the starter
#       state of services/process-application/, because load-solution resets the module to it.
#
#   check-load-solution.sh load NN [--no-smoke]
#       Loads solution NN (00 = back to the starter) into a pristine copy of the repository, checks
#       src/ and pom.xml against an independent oracle (before building: bpmn-to-code writes into
#       src/main/java), runs ./mvnw verify on the module and starts the application once on Maven's
#       classpath (random port, in-memory H2). 00 is not started: the starter has
#       @SpringBootApplication commented out.
#
#   check-load-solution.sh sequence A B [C ...] [--no-smoke]
#       Loads A, B, ... one after another into the same copy. Before every further load it builds the
#       current state and leaves participant edits in src/ and pom.xml behind. The final state must
#       equal a fresh load of the last solution; then verify (+ smoke start).
#
# The caller's checkout is never modified: every run works on a copy of the working tree (without
# .git, target/, node_modules/, .load-solution-backup/) in a temporary directory, so in CI the
# "pristine" state is the fresh checkout. KEEP_WORKDIR=1 keeps that directory for debugging,
# SMOKE_TIMEOUT (seconds, default 180) bounds the smoke start.
#
# Needs bash (3.2 is enough), a JDK (JAVA_HOME or java on PATH), tar, diff, awk; curl for the HTTP
# part of the smoke start. On Windows (Git Bash) Maven runs through mvnw.cmd.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
MODULE=services/process-application
STARTER=templates/process-application-starter
# The paths below src/ that load-solution resets and fills; everything else (ArchitectureTest) stays.
MANAGED_RE='^(main/|test/java/io/miragon/training/process/|test/resources/)'
SMOKE_TIMEOUT=${SMOKE_TIMEOUT:-180}
SYNC_CMD='rm -rf templates/process-application-starter/src && cp -R services/process-application/src templates/process-application-starter/ && cp services/process-application/pom.xml templates/process-application-starter/'

case "$(uname -s)" in
    MINGW* | MSYS* | CYGWIN*) MVNW=mvnw.cmd ;;
    *) MVNW=mvnw ;;
esac
JAVA=java
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVA="$JAVA_HOME/bin/java"; fi

WORK=""
SMOKE_PID=""
SMOKE=1

log() { printf '\n==> %s\n' "$*"; }
info() { printf '    %s\n' "$*"; }
die() {
    if [ -n "${GITHUB_ACTIONS:-}" ]; then echo "::error::$*" >&2; else echo "ERROR: $*" >&2; fi
    exit 1
}
usage() {
    sed -n '3,19p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//' >&2
    exit 2
}

cleanup() {
    if [ -n "$SMOKE_PID" ]; then
        kill "$SMOKE_PID" 2>/dev/null || true
        wait "$SMOKE_PID" 2>/dev/null || true
    fi
    if [ -n "$WORK" ]; then
        if [ "${KEEP_WORKDIR:-0}" = 1 ]; then echo "Work directory kept: $WORK"; else rm -rf "$WORK"; fi
    fi
}
trap cleanup EXIT

# ---------------------------------------------------------------------------------------------------
# Work copies and Maven
# ---------------------------------------------------------------------------------------------------

make_workdir() {
    local base=${RUNNER_TEMP:-${TMPDIR:-/tmp}}
    if command -v cygpath >/dev/null 2>&1; then base=$(cygpath -u "$base"); fi
    WORK=$(mktemp -d "${base%/}/load-solution.XXXXXX")
}

copy_repo() { # dest
    mkdir -p "$1"
    (cd "$REPO_ROOT" && tar -cf - --exclude=.git --exclude=target --exclude=node_modules \
        --exclude=.load-solution-backup --exclude=.DS_Store .) | (cd "$1" && tar -xf -)
}

load_solution() { # repo NN [root|module]: the two documented invocations
    local repo=$1 nn=$2 from=${3:-root}
    log "load-solution $nn (run from the $from directory)"
    if [ "$from" = module ]; then
        (cd "$repo/$MODULE" && "../../$MVNW" -B -ntp antrun:run@load-solution "-Dsolution=$nn")
    else
        (cd "$repo" && "./$MVNW" -B -ntp -pl "$MODULE" antrun:run@load-solution "-Dsolution=$nn")
    fi
}

maven_module() { # repo goal...
    local repo=$1
    shift
    log "./mvnw $* ($MODULE)"
    (cd "$repo" && "./$MVNW" -B -ntp -pl "$MODULE" "$@")
}

# ---------------------------------------------------------------------------------------------------
# The oracle: what load-solution NN must produce, derived from the contract, not from the Ant code
# ---------------------------------------------------------------------------------------------------

validate_nn() {
    [[ $1 =~ ^(0[0-9]|10)$ ]] || die "invalid solution '$1' (expected two digits, 00-10)"
}

solution_dir() {
    if [ "$1" = 10 ]; then echo solutions/exercise-10/process-application; else echo "solutions/exercise-$1"; fi
}

# ERE for a "TODO Exercise M" marker with 1 <= M <= NN (empty for 00). The number must not run on
# into another word character, so "TODO Exercise 1" never matches "TODO Exercise 10".
marker_re() {
    local n=$((10#$1)) m alts=""
    for ((m = 1; m <= n; m++)); do alts="$alts${alts:+|}$m"; done
    if [ -n "$alts" ]; then printf 'TODO Exercise (%s)([^[:alnum:]_]|$)' "$alts"; fi
}

list_files() { (cd "$1" && find . -type f ! -name .DS_Store | sed 's|^\./||' | LC_ALL=C sort); }

put() { mkdir -p "$(dirname "$2")" && cp "$1" "$2"; }

# Expected src/ after loading NN onto a starter module:
#   managed paths: every file of solution NN, plus every starter file the solution does not bring
#                  that carries no "TODO Exercise M" marker with M <= NN;
#   anything else (ArchitectureTest): the starter's file, untouched.
DROPPED=""
build_expected_src() { # repo NN dest
    local repo=$1 nn=$2 dest=$3 re sol="" rel
    re=$(marker_re "$nn")
    if [ "$nn" != 00 ]; then sol="$repo/$(solution_dir "$nn")/src"; fi
    DROPPED=""
    mkdir -p "$dest"
    while IFS= read -r rel; do
        if [[ $rel =~ $MANAGED_RE ]]; then
            if [ -n "$sol" ] && [ -f "$sol/$rel" ]; then continue; fi
            if [ -n "$re" ] && grep -qE "$re" "$repo/$STARTER/src/$rel"; then
                DROPPED="$DROPPED $rel"
                continue
            fi
        fi
        put "$repo/$STARTER/src/$rel" "$dest/$rel"
    done < <(list_files "$repo/$STARTER/src")
    if [ -z "$sol" ]; then return 0; fi
    while IFS= read -r rel; do
        [[ $rel =~ $MANAGED_RE ]] || die "solution $nn contains src/$rel, outside the paths load-solution copies" \
            "(src/main, src/test/java/io/miragon/training/process, src/test/resources): it never reaches the participant"
        put "$sol/$rel" "$dest/$rel"
    done < <(list_files "$sol")
}

# Expected pom.xml: the starter pom with the "<!--" line after and the "-->" line before the body of
# every prepared block <!-- @exercise-N[-suffix]:begin ... --> ... <!-- @exercise-N[-suffix]:end -->
# with N <= NN removed. Keeps the line endings of the starter (CRLF on a Windows checkout).
EXPECTED_POM_AWK='
function trim(s) { sub(/\r$/, "", s); gsub(/^[ \t]+|[ \t]+$/, "", s); return s }
{ t = trim($0) }
state == 0 {
    print
    if (t ~ /^<!-- @exercise-[0-9]+(-[a-z]+)?:begin/) {
        n = t; sub(/^<!-- @exercise-/, "", n)
        if (n + 0 <= nn) state = 1
    }
    next
}
state == 1 {
    if (t != "<!--") { printf "pom.xml:%d: expected \"<!--\" right after an active begin marker\n", NR > "/dev/stderr"; exit 2 }
    state = 2; cnt = 0; next
}
t ~ /^<!-- @exercise-[0-9]+(-[a-z]+)?:end/ {
    if (cnt == 0 || trim(buf[cnt]) != "-->") { printf "pom.xml:%d: expected \"-->\" right before the end marker\n", NR > "/dev/stderr"; exit 3 }
    for (i = 1; i < cnt; i++) print buf[i]
    print; state = 0; next
}
{ buf[++cnt] = $0 }
END { if (state != 0) { print "pom.xml: prepared block without end marker" > "/dev/stderr"; exit 4 } }
'

# For every begin marker: the next line is "<!--" (block still commented out) exactly when N > NN.
BLOCK_STATE_AWK='
function trim(s) { sub(/\r$/, "", s); gsub(/^[ \t]+|[ \t]+$/, "", s); return s }
{ t = trim($0) }
want != "" {
    off = (t == "<!--")
    if (off && want == "on") { printf "pom.xml:%d: block @%s must be switched on for solution %02d, but is still commented out\n", NR, id, nn > "/dev/stderr"; bad = 1 }
    if (!off && want == "off") { printf "pom.xml:%d: block @%s must stay commented out for solution %02d\n", NR, id, nn > "/dev/stderr"; bad = 1 }
    want = ""
}
t ~ /^<!-- @exercise-[0-9]+(-[a-z]+)?:begin/ {
    id = t; sub(/^<!-- @/, "", id); sub(/:begin.*$/, "", id)
    n = id; sub(/^exercise-/, "", n)
    want = (n + 0 <= nn) ? "on" : "off"; blocks++
}
END {
    if (blocks == 0) { print "pom.xml: no <!-- @exercise-N:begin --> markers found" > "/dev/stderr"; bad = 1 }
    exit bad
}
'

# "dependency g:a" / "plugin g:a" for every <dependency> and <plugin> outside XML comments (the
# <parent> and <exclusion>s are skipped). Enough for the hand-written poms of this repository.
COORDS_AWK='
{
    line = $0; out = ""
    while (line != "") {
        if (inc) {
            p = index(line, "-->"); if (!p) { line = ""; continue }
            line = substr(line, p + 3); inc = 0
        } else {
            p = index(line, "<!--"); if (!p) { out = out line; line = ""; continue }
            out = out substr(line, 1, p - 1); line = substr(line, p + 4); inc = 1
        }
    }
    if (out ~ /<plugin>/) { kind = "plugin"; g = ""; a = "" }
    else if (out ~ /<dependency>/ && kind != "plugin") { kind = "dependency"; g = ""; a = "" }
    if (out ~ /<exclusion>/) excl = 1
    if (kind != "" && !excl) {
        if (g == "" && match(out, /<groupId>[^<]*<\/groupId>/)) g = substr(out, RSTART + 9, RLENGTH - 19)
        if (a == "" && match(out, /<artifactId>[^<]*<\/artifactId>/)) a = substr(out, RSTART + 12, RLENGTH - 25)
    }
    if (out ~ /<\/exclusion>/) excl = 0
    if (kind == "dependency" && out ~ /<\/dependency>/) { print "dependency " g ":" a; kind = "" }
    if (kind == "plugin" && out ~ /<\/plugin>/) { print "plugin " (g == "" ? "org.apache.maven.plugins" : g) ":" a; kind = "" }
}
'

check_pom() { # repo NN
    local repo=$1 nn=$2 pom="$repo/$MODULE/pom.xml" expected="$WORK/expected-pom-$2.xml" missing
    awk -v nn=$((10#$nn)) "$BLOCK_STATE_AWK" "$pom" || die "pom.xml: prepared blocks in the wrong state after load-solution $nn"
    awk -v nn=$((10#$nn)) "$EXPECTED_POM_AWK" "$repo/$STARTER/pom.xml" >"$expected" \
        || die "$STARTER/pom.xml: malformed prepared block"
    diff "$expected" "$pom" || die "pom.xml after load-solution $nn is not the starter pom with the blocks <= $nn switched on" \
        "(left: expected, right: loaded)"
    if [ "$nn" != 00 ]; then
        # Independent of the markers: everything solution NN's own pom declares must be active.
        missing=$(LC_ALL=C comm -23 \
            <(awk "$COORDS_AWK" "$repo/$(solution_dir "$nn")/pom.xml" | LC_ALL=C sort -u) \
            <(awk "$COORDS_AWK" "$pom" | LC_ALL=C sort -u))
        [ -z "$missing" ] || die "pom.xml after load-solution $nn lacks what $(solution_dir "$nn")/pom.xml declares:" \
            "$(echo "$missing" | tr '\n' ' ')- add a prepared @exercise-N block to the starter pom"
    fi
}

check_loaded_state() { # repo NN
    local repo=$1 nn=$2 mod="$repo/$MODULE" expected="$WORK/expected-src-$2" arch=test/java/io/miragon/training/ArchitectureTest.java
    log "Checking src/ and pom.xml after load-solution $nn"
    rm -rf "$expected"
    build_expected_src "$repo" "$nn" "$expected"
    diff -r -x .DS_Store "$expected" "$mod/src" \
        || die "src/ after load-solution $nn differs from the expected tree (left: expected, right: loaded)"
    cmp "$repo/$STARTER/src/$arch" "$mod/src/$arch" || die "load-solution $nn changed $arch"
    if [ "$nn" = 00 ]; then
        diff -r -x .DS_Store "$repo/$STARTER/src" "$mod/src" && cmp "$repo/$STARTER/pom.xml" "$mod/pom.xml" \
            || die "load-solution 00 must give back the starter (src/ + pom.xml)"
    elif [ $((10#$nn)) -ge 4 ]; then
        # From 04 on every starter file is either part of the solution or marked as a solved stub.
        diff -r -x .DS_Store "$repo/$(solution_dir "$nn")/src/main" "$mod/src/main" \
            || die "src/main after load-solution $nn must equal $(solution_dir "$nn")/src/main (a starter file without a TODO marker leaked in?)"
    fi
    if [ -n "$DROPPED" ]; then
        info "left out on purpose (marked TODO Exercise <= $nn, not in solution $nn):"
        for rel in $DROPPED; do info "  src/$rel"; done
    fi
    check_pom "$repo" "$nn"
    info "OK: src/ ($(list_files "$mod/src" | wc -l | tr -d ' ') files) and pom.xml as expected"
}

snapshot() { # repo dest: the module's src/ and pom.xml as they are now
    mkdir -p "$2"
    cp -R "$1/$MODULE/src" "$2/src"
    cp "$1/$MODULE/pom.xml" "$2/pom.xml"
}

check_backup() { # repo snapshot: the newest backup holds the state from before the load
    local newest
    newest=$(find "$1/$MODULE/.load-solution-backup" -mindepth 1 -maxdepth 1 -type d 2>/dev/null | LC_ALL=C sort | tail -n 1)
    [ -n "$newest" ] || die "load-solution wrote no backup to $MODULE/.load-solution-backup/"
    diff -r "$2/src" "$newest/src" && cmp "$2/pom.xml" "$newest/pom.xml" \
        || die "backup $newest does not hold the src/ and pom.xml from before the load"
    info "OK: previous state backed up to ${newest#"$1/"}"
}

check_target_clean() { # repo: nothing but target/antrun may survive a load
    local left
    left=$(cd "$1/$MODULE" && if [ -d target ]; then find target -mindepth 1 -maxdepth 1 ! -name antrun; fi)
    [ -z "$left" ] || die "load-solution left build output behind: $(echo "$left" | tr '\n' ' ')"
}

smoke_start() { # repo label
    local repo=$1 label=$2 sep=: cpfile logfile port i code deployed expected
    case "$(uname -s)" in MINGW* | MSYS* | CYGWIN*) sep=';' ;; esac
    cpfile="$WORK/classpath-$label.txt"
    logfile="$WORK/smoke-$label.log"
    # Start on Maven's classpath order, like spring-boot:run and the IDE do. The repackaged jar orders
    # its libraries alphabetically and then loads the Boot-3 cibseven-webclient-web before the Boot-4
    # variant, so `java -jar` fails (not related to load-solution).
    maven_module "$repo" -q dependency:build-classpath -Dmdep.includeScope=runtime "-Dmdep.outputFile=$cpfile"
    log "Smoke start of TrainingApplication (random port, in-memory H2)"
    "$JAVA" -Dspring.devtools.restart.enabled=false -cp "$repo/$MODULE/target/classes$sep$(cat "$cpfile")" \
        io.miragon.training.TrainingApplication \
        --server.port=0 "--spring.datasource.url=jdbc:h2:mem:smoke-$label;DB_CLOSE_DELAY=-1" \
        >"$logfile" 2>&1 &
    SMOKE_PID=$!
    for ((i = 0; i < SMOKE_TIMEOUT; i += 2)); do
        if grep -q 'Started TrainingApplication' "$logfile" && grep -q 'ENGINE-00001' "$logfile"; then break; fi
        kill -0 "$SMOKE_PID" 2>/dev/null || break
        sleep 2
    done
    if ! grep -q 'Started TrainingApplication' "$logfile" || ! grep -q 'ENGINE-00001' "$logfile"; then
        echo "----- last 100 lines of the application log -----" >&2
        tail -n 100 "$logfile" >&2
        if kill -0 "$SMOKE_PID" 2>/dev/null; then
            die "the application did not come up with an engine within ${SMOKE_TIMEOUT}s ('Started TrainingApplication' + 'ENGINE-00001')"
        fi
        die "the application exited during start-up (no 'Started TrainingApplication' + 'ENGINE-00001'); log tail above"
    fi
    info "OK: 'Started TrainingApplication' and 'ENGINE-00001' after ~${i}s"

    port=$(grep -m 1 -oE 'Tomcat started on port[^0-9]*[0-9]+' "$logfile" | grep -oE '[0-9]+$' || true)
    if [ -n "$port" ] && command -v curl >/dev/null 2>&1; then
        code=$(curl -fsS "http://localhost:$port/engine-rest/engine" || true)
        [[ $code == *'"default"'* ]] || die "GET /engine-rest/engine did not list the default engine: $code"
        code=$(curl -s -o /dev/null -w '%{http_code}' "http://localhost:$port/webapp/")
        [ "$code" = 200 ] || die "GET /webapp/ returned $code"
        # Only the BPMN files of the loaded solution are deployed: nothing stale on the classpath.
        deployed=$(curl -fsS "http://localhost:$port/engine-rest/process-definition" \
            | grep -oE '"resource":"[^"]*"' | sed 's/"resource":"//; s/"$//; s|.*/||' | LC_ALL=C sort -u | tr '\n' ' ') \
            || die "GET /engine-rest/process-definition failed or listed no process definition"
        expected=$(find "$repo/$MODULE/src/main/resources" -name '*.bpmn' | sed 's|.*/||' | LC_ALL=C sort -u | tr '\n' ' ')
        [ "$deployed" = "$expected" ] || die "deployed BPMN resources [$deployed] differ from src/main/resources [$expected]"
        info "OK: /engine-rest/engine, /webapp/ 200, deployed BPMN: $deployed"
    else
        info "HTTP checks skipped (no port in the log or no curl)"
    fi
    kill "$SMOKE_PID" 2>/dev/null || true
    wait "$SMOKE_PID" 2>/dev/null || true
    SMOKE_PID=""
}

# ---------------------------------------------------------------------------------------------------
# Commands
# ---------------------------------------------------------------------------------------------------

check_baseline() { # root
    local root=$1 ok=1
    log "Comparing $STARTER/ with the starter state of $MODULE/ (src/ + pom.xml)"
    diff -r -x .DS_Store "$root/$STARTER/src" "$root/$MODULE/src" || ok=0
    diff "$root/$STARTER/pom.xml" "$root/$MODULE/pom.xml" || ok=0
    if [ "$ok" = 0 ]; then
        cat >&2 <<EOF

$STARTER/ must be a byte copy of $MODULE/ (src/ + pom.xml):
load-solution resets the module to it before it copies a solution on top.
- Changed the starter on purpose? Sync the copy, from the repository root:
    $SYNC_CMD
- Loaded a solution in this checkout? Reset the module first:
    ./mvnw -pl $MODULE antrun:run@load-solution -Dsolution=00
EOF
        die "$STARTER/ is out of sync with $MODULE/"
    fi
    info "OK: starter baseline in sync"
}

cmd_load() {
    [ $# -eq 1 ] || usage
    local nn=$1 repo
    validate_nn "$nn"
    make_workdir
    repo="$WORK/repo"
    copy_repo "$repo"
    check_baseline "$repo"
    snapshot "$repo" "$WORK/before"
    load_solution "$repo" "$nn" root
    check_loaded_state "$repo" "$nn"
    check_backup "$repo" "$WORK/before"
    check_target_clean "$repo"
    maven_module "$repo" verify
    if [ "$nn" = 00 ]; then
        log "Smoke start skipped: the starter has @SpringBootApplication commented out"
    elif [ "$SMOKE" = 1 ]; then
        smoke_start "$repo" "$nn"
    fi
    log "OK: load-solution $nn"
}

cmd_sequence() {
    [ $# -ge 2 ] || usage
    local nn repo fresh i=0 last="" from
    for nn in "$@"; do validate_nn "$nn"; done
    make_workdir
    repo="$WORK/repo"
    fresh="$WORK/fresh"
    copy_repo "$repo"
    check_baseline "$repo"
    for nn in "$@"; do
        if [ $i -gt 0 ]; then
            log "Participant on solution $last: build it, then leave edits in src/ and pom.xml"
            maven_module "$repo" -q compile
            echo "participant edit" >"$repo/$MODULE/src/main/resources/participant-edit.txt"
            echo "<!-- participant edit -->" >>"$repo/$MODULE/pom.xml"
        fi
        snapshot "$repo" "$WORK/before-$i"
        # Alternate the documented invocations: repository root (-pl) and module directory.
        if [ $((i % 2)) -eq 0 ]; then from=root; else from=module; fi
        load_solution "$repo" "$nn" "$from"
        check_backup "$repo" "$WORK/before-$i"
        check_target_clean "$repo"
        last=$nn
        i=$((i + 1))
    done
    check_loaded_state "$repo" "$last"

    log "Comparing with a fresh load of $last"
    copy_repo "$fresh"
    load_solution "$fresh" "$last" root
    diff -r "$fresh/$MODULE/src" "$repo/$MODULE/src" \
        || die "sequence $*: src/ differs from a fresh load of $last (left: fresh, right: sequence)"
    cmp "$fresh/$MODULE/pom.xml" "$repo/$MODULE/pom.xml" \
        || die "sequence $*: pom.xml differs from a fresh load of $last"
    info "OK: identical to a fresh load of $last"

    maven_module "$repo" verify
    if [ "$last" != 00 ] && [ "$SMOKE" = 1 ]; then smoke_start "$repo" "seq-$last"; fi
    log "OK: sequence $*"
}

main() {
    local cmd=${1:-} arg
    local -a args=()
    [ $# -gt 0 ] && shift
    for arg in "$@"; do
        case $arg in
            --no-smoke) SMOKE=0 ;;
            -*) usage ;;
            *) args+=("$arg") ;;
        esac
    done
    case $cmd in
        baseline) [ ${#args[@]} -eq 0 ] || usage; check_baseline "$REPO_ROOT" ;;
        load) cmd_load ${args[@]+"${args[@]}"} ;;
        sequence) cmd_sequence ${args[@]+"${args[@]}"} ;;
        *) usage ;;
    esac
}

main "$@"
