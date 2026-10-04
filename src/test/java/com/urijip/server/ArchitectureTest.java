package com.urijip.server;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

@AnalyzeClasses(packages = "com.urijip.server", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	private static final String ROOT = "com.urijip.server.";

	@ArchTest
	static final ArchRule controllers_do_not_use_repositories = noClasses()
			.that().resideInAPackage("..controller..")
			.should().dependOnClassesThat().resideInAPackage("..repository..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule controllers_do_not_expose_entities = noClasses()
			.that().resideInAPackage("..controller..")
			.should().dependOnClassesThat().resideInAPackage("..entity..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule services_do_not_use_controllers = noClasses()
			.that().resideInAPackage("..service..")
			.should().dependOnClassesThat().resideInAPackage("..controller..")
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule repositories_do_not_use_upper_layers = noClasses()
			.that().resideInAPackage("..repository..")
			.should().dependOnClassesThat().resideInAnyPackage("..controller..", "..service..")
			.allowEmptyShould(true);

}
