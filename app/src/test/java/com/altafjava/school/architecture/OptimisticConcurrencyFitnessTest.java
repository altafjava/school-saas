package com.altafjava.school.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.platform.core.concurrency.Versioned;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Two people editing the same record must not silently overwrite each other. Every PATCH/PUT with
 * a body is therefore one of: a form edit whose body carries the {@code version} the client read
 * and whose response returns the new one, a {@link Command}, or a stated {@link LastWriteWins}.
 */
class OptimisticConcurrencyFitnessTest {

	@Test
	void everyEditEndpointDeclaresHowItHandlesConcurrentEdits() {
		Set<String> violations = new TreeSet<>();
		for (JavaClass javaClass : new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("com.altafjava.school.api.controller")) {
			if (!javaClass.isAnnotatedWith(RestController.class)) {
				continue;
			}
			for (Method method : javaClass.reflect().getDeclaredMethods()) {
				check(method, violations);
			}
		}

		assertTrue(violations.isEmpty(), "Edit endpoints without a concurrency decision:\n"
				+ String.join("\n", violations));
	}

	private static void check(Method method, Set<String> violations) {
		boolean edits = method.isAnnotationPresent(PatchMapping.class) || method.isAnnotationPresent(PutMapping.class);
		Class<?> body = Arrays.stream(method.getParameters())
				.filter(parameter -> parameter.isAnnotationPresent(RequestBody.class))
				.map(Parameter::getType)
				.findFirst().orElse(null);
		if (!edits || body == null) {
			return;
		}
		String endpoint = method.getDeclaringClass().getSimpleName() + "#" + method.getName();
		boolean exempt = method.isAnnotationPresent(Command.class) || method.isAnnotationPresent(LastWriteWins.class);
		boolean versioned = Versioned.class.isAssignableFrom(body);
		if (versioned == exempt) {
			violations.add(endpoint + (versioned ? " is both versioned and exempt"
					: " takes " + body.getSimpleName() + ", which is not Versioned; mark it @Command or @LastWriteWins"
							+ " if that is intended"));
		}
		if (versioned && !returnsVersion(method.getGenericReturnType())) {
			violations.add(endpoint + " takes a version but its response has none for the next edit");
		}
	}

	// Unwraps ApiResponse<X> / ResponseEntity<ApiResponse<X>> down to X and looks for X.version.
	private static boolean returnsVersion(Type type) {
		if (type instanceof ParameterizedType parameterized) {
			return Arrays.stream(parameterized.getActualTypeArguments())
					.anyMatch(OptimisticConcurrencyFitnessTest::returnsVersion);
		}
		if (type instanceof Class<?> clazz && clazz.isRecord()) {
			return Arrays.stream(clazz.getRecordComponents()).map(RecordComponent::getName)
					.anyMatch("version"::equals);
		}
		return false;
	}
}
