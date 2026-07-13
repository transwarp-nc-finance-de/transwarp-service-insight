package com.transwarp.serviceinsight.precheck;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class PrecheckArchitectureTest {
  private final com.tngtech.archunit.core.domain.JavaClasses classes =
      new ClassFileImporter()
          .withImportOption(new ImportOption.DoNotIncludeTests())
          .importPackages("com.transwarp.serviceinsight");

  @Test
  void domainIsIndependent() {
    noClasses()
        .that()
        .resideInAPackage("..precheck.domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "org.springframework..",
            "..precheck.api..",
            "..precheck.dto..",
            "..precheck.infrastructure..")
        .check(classes);
  }

  @Test
  void applicationOwnsWorkflow() {
    noClasses()
        .that()
        .resideInAPackage("..precheck.application..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..precheck.api..", "..precheck.dto..", "..precheck.infrastructure..")
        .check(classes);
  }

  @Test
  void infrastructureHasNoHttpDto() {
    noClasses()
        .that()
        .resideInAPackage("..precheck.infrastructure..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..precheck.api..", "..precheck.dto..")
        .check(classes);
  }

  @Test
  void apiDoesNotDependOnConcreteInfrastructure() {
    noClasses()
        .that()
        .resideInAPackage("..precheck.api..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("..precheck.infrastructure..")
        .check(classes);
  }
}
