package com.logistica.demo.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.logistica.demo", importOptions = ImportOption.DoNotIncludeTests.class)
class ModularArchitectureTest {

    private static final String ROOT = "com.logistica.demo.";

    @ArchTest
    static final ArchRule sharedKernelIsIndependent = noClasses()
            .that().resideInAPackage("..sharedkernel..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "platform..",
                    ROOT + "cuadronecesidades..",
                    ROOT + "presupuesto..",
                    ROOT + "logistica..")
            .because("sharedkernel must remain independent from business modules");

    @ArchTest
    static final ArchRule platformDependencyDirection = noClasses()
            .that().resideInAPackage("..platform..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "cuadronecesidades..",
                    ROOT + "presupuesto..",
                    ROOT + "logistica..")
            .because("platform is the first business module in the dependency graph");

    @ArchTest
    static final ArchRule needsDependencyDirection = noClasses()
            .that().resideInAPackage("..cuadronecesidades..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    ROOT + "presupuesto..",
                    ROOT + "logistica..")
            .because("cuadronecesidades may only depend on platform APIs and sharedkernel");

    @ArchTest
    static final ArchRule budgetDependencyDirection = noClasses()
            .that().resideInAPackage("..presupuesto..")
            .should().dependOnClassesThat().resideInAPackage(ROOT + "logistica..")
            .because("presupuesto cannot depend on its downstream logistics consumer");

    @ArchTest
    static final ArchRule platformInternalsArePrivate = moduleInternalsArePrivate("platform");

    @ArchTest
    static final ArchRule needsInternalsArePrivate = moduleInternalsArePrivate("cuadronecesidades");

    @ArchTest
    static final ArchRule budgetInternalsArePrivate = moduleInternalsArePrivate("presupuesto");

    @ArchTest
    static final ArchRule logisticsInternalsArePrivate = moduleInternalsArePrivate("logistica");

    @ArchTest
    static final ArchRule moduleApisAreFrameworkIndependent = noClasses()
            .that().resideInAnyPackage(
                    "..platform.api..",
                    "..cuadronecesidades.api..",
                    "..presupuesto.api..",
                    "..logistica.api..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..")
            .because("module APIs are framework-independent contracts");

    private static ArchRule moduleInternalsArePrivate(String module) {
        return noClasses()
                .that().resideOutsideOfPackage(ROOT + module + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + module + ".domain..",
                        ROOT + module + ".application..",
                        ROOT + module + ".infrastructure..")
                .allowEmptyShould(true)
                .because("other modules must consume only the public API of " + module);
    }
}
