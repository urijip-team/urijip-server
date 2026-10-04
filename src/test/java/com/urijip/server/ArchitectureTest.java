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

	@ArchTest
	static final ArchRule global_does_not_depend_on_domains = classes()
			.that().resideInAPackage(ROOT + "global..")
			.should(dependOnlyOnOwnDomainOrGlobal())
			.allowEmptyShould(true);

	@ArchTest
	static final ArchRule repositories_are_used_only_within_their_domain = classes()
			.that().resideInAPackage("..repository..")
			.should(beAccessedOnlyFromOwnDomain())
			.allowEmptyShould(true);

	private static ArchCondition<JavaClass> beAccessedOnlyFromOwnDomain() {
		return new ArchCondition<>("be accessed only from their own domain") {
			@Override
			public void check(JavaClass repository, ConditionEvents events) {
				String domain = domainOf(repository);
				for (Dependency dependency : repository.getDirectDependenciesToSelf()) {
					JavaClass origin = dependency.getOriginClass();
					if (!domain.equals(domainOf(origin))) {
						events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
					}
				}
			}
		};
	}

	private static ArchCondition<JavaClass> dependOnlyOnOwnDomainOrGlobal() {
		return new ArchCondition<>("not depend on domain packages") {
			@Override
			public void check(JavaClass globalClass, ConditionEvents events) {
				for (Dependency dependency : globalClass.getDirectDependenciesFromSelf()) {
					JavaClass target = dependency.getTargetClass();
					if (target.getPackageName().startsWith(ROOT) && !domainOf(target).equals("global")) {
						events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription()));
					}
				}
			}
		};
	}

	/** {@code com.urijip.server.member.service} → {@code member}; classes outside a domain package → "". */
	private static String domainOf(JavaClass javaClass) {
		String packageName = javaClass.getPackageName();
		if (!packageName.startsWith(ROOT)) {
			return "";
		}
		String rest = packageName.substring(ROOT.length());
		int dot = rest.indexOf('.');
		return dot < 0 ? rest : rest.substring(0, dot);
	}

}
