package io.miragon.training;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.miragon.training", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

    /**
     * Same guardrail as in the process-application: the remote adapter replaces the external task
     * client and the typed REST client, so no production class touches CIB seven directly anymore.
     */
    @ArchTest
    static final ArchRule no_class_should_depend_on_cibseven = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("org.cibseven..");
}
