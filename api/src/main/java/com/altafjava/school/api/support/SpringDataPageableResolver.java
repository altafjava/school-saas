package com.altafjava.school.api.support;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import com.altafjava.platform.api.support.PageableParamResolver;
import lombok.RequiredArgsConstructor;

/**
 * Adapts platform's {@link PageableParamResolver} — which resolves {@code page}/{@code size} into
 * the platform-generic {@code core.model.Pageable} record, bounds-checked against
 * {@code PlatformConfigurer#maxPageSize()}/{@code defaultPageSize()} — into Spring Data's
 * {@link Pageable}, since every school-saas repository still queries via Spring Data JPA.
 * Controllers call this instead of hand-rolling {@code PageRequest.of(page, Math.min(size, 100))}.
 *
 * <p>
 * The request's {@code sort} values are applied too, restricted to the audit columns plus whatever
 * the endpoint declares with {@link SortableBy}.
 */
@Component
@RequiredArgsConstructor
public class SpringDataPageableResolver {

	private final PageableParamResolver pageableParamResolver;

	public Pageable resolve(int page, int size) {
		var resolved = pageableParamResolver.resolve(page, size);
		return PageRequest.of(resolved.page(), resolved.size(), requestedSort());
	}

	private Sort requestedSort() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return Sort.unsorted();
		}
		HttpServletRequest request = attributes.getRequest();
		String[] requested = request.getParameterValues("sort");
		if (requested == null) {
			return Sort.unsorted();
		}
		return SortParser.parse(List.of(requested), endpointAttributes(request));
	}

	private Set<String> endpointAttributes(HttpServletRequest request) {
		return Optional.ofNullable(request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE))
				.filter(HandlerMethod.class::isInstance)
				.map(handler -> ((HandlerMethod) handler).getMethodAnnotation(SortableBy.class))
				.map(sortable -> Set.copyOf(Arrays.asList(sortable.value())))
				.orElse(Set.of());
	}
}
