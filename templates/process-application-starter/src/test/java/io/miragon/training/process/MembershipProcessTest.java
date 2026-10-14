package io.miragon.training.process;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Placeholder for the process test from Exercise 6.
 *
 * <p>In Exercise 6 you grow this test into a real process test: an in-memory engine
 * (h2, job executor off), the use cases behind the delegates are mocked, and you drive the
 * process with the helpers from {@code process/util} up to its wait states. The helper
 * {@code ProcessEngineTestUtils} is already provided in the module under {@code process/util};
 * you write the two tests (happy path + rejection) yourself. The exact instructions (dependency,
 * {@code application-test.yaml}, helpers, assertions) are in {@code docs/en/exercise-06.md};
 * the full solution lives under {@code solutions/exercise-06/}.
 *
 * <p>Until then the test is disabled so the starter module compiles even without an active engine.
 */
@Disabled("TODO Exercise 6: Implement the process test – see docs/en/exercise-06.md")
class MembershipProcessTest {

    @Test
    void happyPath_membershipIsConfirmedAndWelcomeMailIsSent() {
        throw new UnsupportedOperationException("Exercise 6: Implement this process test");
    }
}
