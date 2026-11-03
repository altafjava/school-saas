package com.altafjava.school.util;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Plays the real client for tests that hold a tenant's internal id: a numeric {@code X-Tenant-ID}
 * is sent as the tenant's public id, the only id the API accepts. An unknown id is left as it is.
 */
@Component
public class PublicTenantHeaderFilter implements Filter {

	private static final String TENANT_HEADER = "X-Tenant-ID";
	private static final Pattern INTERNAL_ID = Pattern.compile("\\d+");
	private static volatile PublicTenantHeaderFilter instance;
	// Static and refreshed per context: the filter is registered once, but test contexts come and go.
	private static volatile JdbcTemplate jdbcTemplate;

	public PublicTenantHeaderFilter(JdbcTemplate jdbcTemplate) {
		PublicTenantHeaderFilter.jdbcTemplate = jdbcTemplate;
		instance = this;
		install();
	}

	/** Idempotent; a test base that calls {@code RestAssured.reset()} calls this again afterwards. */
	public static void install() {
		if (instance != null && !RestAssured.filters().contains(instance)) {
			RestAssured.filters(instance);
		}
	}

	@Override
	public Response filter(FilterableRequestSpecification request, FilterableResponseSpecification response,
			FilterContext context) {
		List<Header> tenantHeaders = request.getHeaders().getList(TENANT_HEADER);
		if (tenantHeaders.size() == 1 && INTERNAL_ID.matcher(tenantHeaders.get(0).getValue()).matches()) {
			String publicId = publicIdOf(Long.valueOf(tenantHeaders.get(0).getValue()));
			if (publicId != null) {
				request.removeHeader(tenantHeaders.get(0).getName());
				request.header(TENANT_HEADER, publicId);
			}
		}
		return context.next(request, response);
	}

	private static String publicIdOf(Long tenantId) {
		List<String> ids = jdbcTemplate.queryForList("SELECT public_id FROM tenants WHERE id = ?", String.class,
				tenantId);
		return ids.isEmpty() ? null : ids.get(0);
	}
}
