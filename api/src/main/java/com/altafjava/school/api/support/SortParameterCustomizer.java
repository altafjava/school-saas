package com.altafjava.school.api.support;

import java.util.Set;
import java.util.TreeSet;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;

/**
 * Documents the {@code sort} query parameter on every paged endpoint, naming the fields that
 * endpoint accepts — the resolver reads the parameter from the request, so it is not a controller
 * argument springdoc would discover on its own.
 */
@Component
public class SortParameterCustomizer implements OperationCustomizer {

	@Override
	public Operation customize(Operation operation, HandlerMethod handlerMethod) {
		if (!isPaged(operation)) {
			return operation;
		}
		Set<String> fields = new TreeSet<>(SortParser.AUDIT_ATTRIBUTES);
		SortableBy sortable = handlerMethod.getMethodAnnotation(SortableBy.class);
		if (sortable != null) {
			fields.addAll(Set.of(sortable.value()));
		}
		operation.addParametersItem(new Parameter()
				.name("sort")
				.in("query")
				.required(false)
				.description("Repeatable, `field` or `field,asc|desc`, at most " + SortParser.MAX_SORT_KEYS
						+ " fields. Sortable: " + String.join(", ", fields) + ".")
				.schema(new ArraySchema().items(new StringSchema())));
		return operation;
	}

	private boolean isPaged(Operation operation) {
		return operation.getParameters() != null
				&& operation.getParameters().stream().anyMatch(parameter -> "page".equals(parameter.getName()))
				&& operation.getParameters().stream().anyMatch(parameter -> "size".equals(parameter.getName()));
	}
}
