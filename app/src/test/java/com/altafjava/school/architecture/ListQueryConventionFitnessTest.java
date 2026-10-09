package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * List convention every paged endpoint follows, so a frontend builds one data-table hook: {@code page}
 * and {@code size} query parameters, {@code sort} handled by the shared sort customizer, and — where the
 * list has a free-text search — a parameter named exactly {@code q}.
 */
@AnalyzeClasses(packages = "com.altafjava.school.api.controller")
class ListQueryConventionFitnessTest {

	@ArchTest
	static final ArchRule pagedEndpointsTakePageAndSize = methods()
			.that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
			.and().arePublic()
			.and(returnAPage())
			.should(takePageAndSize())
			.because("every paged list endpoint uses the same page/size parameters");

	@ArchTest
	static final ArchRule searchParameterIsCalledQ = methods()
			.that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Controller")
			.and().arePublic()
			.and(returnAPage())
			.should(notUseAnAlternativeSearchParameterName())
			.because("free-text search is always the q parameter");

	private static com.tngtech.archunit.base.DescribedPredicate<JavaMethod> returnAPage() {
		return new com.tngtech.archunit.base.DescribedPredicate<>("return a Page") {
			@Override
			public boolean test(JavaMethod method) {
				return method.reflect().getGenericReturnType().getTypeName().contains(".model.Page<");
			}
		};
	}

	private static ArchCondition<JavaMethod> takePageAndSize() {
		return new ArchCondition<>("declare page and size parameters") {
			@Override
			public void check(JavaMethod method, ConditionEvents events) {
				var names = Arrays.stream(method.reflect().getParameters()).map(Parameter::getName).toList();
				if (!names.contains("page") || !names.contains("size")) {
					events.add(SimpleConditionEvent.violated(method,
							method.getFullName() + " returns a Page but lacks page/size parameters " + names));
				}
			}
		};
	}

	private static ArchCondition<JavaMethod> notUseAnAlternativeSearchParameterName() {
		return new ArchCondition<>("call its free-text search parameter q") {
			@Override
			public void check(JavaMethod method, ConditionEvents events) {
				for (Parameter parameter : method.reflect().getParameters()) {
					if (Arrays.asList("search", "query", "keyword", "term", "text").contains(parameter.getName())) {
						events.add(SimpleConditionEvent.violated(method,
								method.getFullName() + " names its search parameter '" + parameter.getName()
										+ "'; use q"));
					}
				}
			}
		};
	}
}
