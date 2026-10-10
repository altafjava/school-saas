package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * The API names every record by its public id. A numeric database id in a request, response, path
 * or query parameter is one a client either cannot obtain or should never have seen.
 */
@AnalyzeClasses(packages = "com.altafjava.school.api")
class PublicIdOnlyContractFitnessTest {

	@ArchTest
	static final ArchRule dtosCarryNoInternalIds = classes()
			.that()
			.resideInAnyPackage("com.altafjava.school.api.dto.request..", "com.altafjava.school.api.dto.response..")
			.should(haveNoNumericIdComponent())
			.because("references in the API contract are public ids (UUID strings), never database ids");

	@ArchTest
	static final ArchRule controllersTakeNoInternalIds = classes()
			.that()
			.resideInAPackage("com.altafjava.school.api.controller")
			.should(haveNoNumericIdParameter())
			.because("path and query parameters identify records by public id, never by database id");

	private static ArchCondition<JavaClass> haveNoNumericIdComponent() {
		return new ArchCondition<>("have no numeric id component") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				RecordComponent[] components = javaClass.reflect().getRecordComponents();
				if (components == null) {
					return;
				}
				for (RecordComponent component : components) {
					if (isIdName(component.getName()) && isNumeric(component.getGenericType())) {
						events.add(SimpleConditionEvent.violated(javaClass, javaClass.getSimpleName() + "."
								+ component.getName() + " exposes a database id; use a public id instead"));
					}
				}
			}
		};
	}

	private static ArchCondition<JavaClass> haveNoNumericIdParameter() {
		return new ArchCondition<>("take no numeric id path or query parameter") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				for (Method method : javaClass.reflect().getDeclaredMethods()) {
					for (Parameter parameter : method.getParameters()) {
						boolean fromRequest = parameter.isAnnotationPresent(PathVariable.class)
								|| parameter.isAnnotationPresent(RequestParam.class);
						if (fromRequest && isIdName(parameter.getName())
								&& isNumeric(parameter.getParameterizedType())) {
							events.add(SimpleConditionEvent.violated(javaClass, javaClass.getSimpleName() + "#"
									+ method.getName() + "(" + parameter.getName()
									+ ") takes a database id; use a public id instead"));
						}
					}
				}
			}
		};
	}

	private static boolean isIdName(String name) {
		return name.equals("id") || name.equals("ids") || name.endsWith("Id") || name.endsWith("Ids");
	}

	// Covers Long, long, Integer, int and collections of them, e.g. List<Long>.
	private static boolean isNumeric(Type type) {
		String typeName = type.getTypeName();
		return typeName.matches(".*\\b(java\\.lang\\.)?(Long|Integer)\\b.*") || typeName.equals("long")
				|| typeName.equals("int");
	}
}
