package com.altafjava.school.architecture;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.altafjava.school.api.support.NoLabel;
import com.altafjava.school.application.reference.ReferenceSummary;

/** A list must render from one call, so a reference carries a label beside its public id. */
class ReferenceLabelFitnessTest {

	@Test
	void everyReferenceOnAResponseCarriesALabel() {
		List<String> unlabelled = new ArrayList<>();
		for (JavaClass type : new ClassFileImporter().importPackages("com.altafjava.school.api.dto.response")) {
			if (type.reflect().isRecord()) {
				unlabelled.addAll(unlabelledReferences(type.reflect()));
			}
		}
		assertThat(unlabelled).isEmpty();
	}

	private static List<String> unlabelledReferences(Class<?> response) {
		Set<String> names = Arrays.stream(response.getRecordComponents()).map(RecordComponent::getName)
				.collect(Collectors.toSet());
		return Arrays.stream(response.getRecordComponents())
				.filter(component -> component.getName().matches(".+PublicId"))
				.filter(component -> !component.isAnnotationPresent(NoLabel.class))
				.map(component -> component.getName().replaceFirst("PublicId$", ""))
				.filter(reference -> !isLabelled(response, reference) && !names.contains(reference + "Name"))
				.map(reference -> response.getSimpleName() + "." + reference)
				.toList();
	}

	private static boolean isLabelled(Class<?> response, String reference) {
		return Arrays.stream(response.getRecordComponents()).anyMatch(
				component -> component.getName().equals(reference) && component.getType() == ReferenceSummary.class);
	}
}
