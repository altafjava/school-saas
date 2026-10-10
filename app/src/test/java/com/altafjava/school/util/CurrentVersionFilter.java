package com.altafjava.school.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;

/**
 * Plays the well-behaved client for every test that is not about concurrency: on a PATCH/PUT whose
 * JSON body carries no {@code version} (absent or null), it sends the record's current one, as a form that just
 * loaded the record would. A test that sets a version itself is left alone; one that must send
 * none calls {@code noFilters()}.
 */
@Component
public class CurrentVersionFilter implements Filter {

	private static volatile CurrentVersionFilter instance;
	private static final Pattern UUID_IN_PATH = Pattern
			.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");
	private static final Pattern SENDS_A_VERSION = Pattern.compile("\"version\"\\s*:\\s*\\d");
	// A serialized request object carries "version":null when the test left it unset.
	private static final Pattern NULL_VERSION = Pattern.compile(",?\\s*\"version\"\\s*:\\s*null");
	// Upserts addressed by their owner: the edited record is found through the owner's id.
	private static final Map<Pattern, String> OWNER_ADDRESSED = Map.of(
			Pattern.compile("/api/v1/health-records/students/[^/]+$"),
			"SELECT h.version FROM health_records h JOIN students s ON s.id = h.student_id WHERE s.public_id = ?");

	// Static and refreshed per context: the filter is registered once, but test contexts come and go.
	private static volatile JdbcTemplate jdbcTemplate;
	private final Map<String, String> tableByPathShape = new ConcurrentHashMap<>();
	private volatile List<String> versionedTables;

	public CurrentVersionFilter(JdbcTemplate jdbcTemplate) {
		CurrentVersionFilter.jdbcTemplate = jdbcTemplate;
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
		String method = request.getMethod();
		Object body = request.getBody();
		if (("PATCH".equals(method) || "PUT".equals(method)) && body instanceof String json
				&& json.trim().startsWith("{") && !SENDS_A_VERSION.matcher(json).find()) {
			Long version = currentVersion(request.getDerivedPath());
			if (version != null) {
				String rest = NULL_VERSION.matcher(json.trim().substring(1)).replaceAll("").trim();
				rest = rest.startsWith(",") ? rest.substring(1).trim() : rest;
				request.body("{\"version\":" + version + (rest.startsWith("}") ? "" : ",") + rest);
			}
		}
		return context.next(request, response);
	}

	private Long currentVersion(String path) {
		String lastId = null;
		Matcher matcher = UUID_IN_PATH.matcher(path);
		while (matcher.find()) {
			lastId = matcher.group();
		}
		if (lastId == null) {
			return null;
		}
		for (Map.Entry<Pattern, String> owned : OWNER_ADDRESSED.entrySet()) {
			if (owned.getKey().matcher(path).find()) {
				return first(jdbcTemplate.queryForList(owned.getValue(), Long.class, lastId));
			}
		}
		String shape = UUID_IN_PATH.matcher(path).replaceAll("{id}");
		String known = tableByPathShape.get(shape);
		if (known != null) {
			return versionIn(known, lastId);
		}
		for (String table : versionedTables()) {
			Long version = versionIn(table, lastId);
			if (version != null) {
				tableByPathShape.put(shape, table);
				return version;
			}
		}
		return null;
	}

	private Long versionIn(String table, String publicId) {
		return first(jdbcTemplate.queryForList("SELECT version FROM " + table + " WHERE public_id = ?", Long.class,
				publicId));
	}

	private static Long first(List<Long> versions) {
		return versions.isEmpty() ? null : versions.get(0);
	}

	private List<String> versionedTables() {
		if (versionedTables == null) {
			versionedTables = jdbcTemplate.queryForList("""
					SELECT p.table_name FROM information_schema.columns p
					JOIN information_schema.columns v
					  ON v.table_schema = p.table_schema AND v.table_name = p.table_name AND v.column_name = 'version'
					WHERE p.table_schema = DATABASE() AND p.column_name = 'public_id'
					""", String.class);
		}
		return versionedTables;
	}
}
