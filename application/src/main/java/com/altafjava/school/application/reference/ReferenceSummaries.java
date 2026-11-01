package com.altafjava.school.application.reference;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

/**
 * Labels references on API responses with one query per kind of record, however many rows a page
 * holds. {@link #mapPage} runs the mapper twice: the first pass only notes which records the rows
 * point at, the second reads the loaded labels. Mappers are pure, so the extra pass has no effect.
 */
@Component
@RequiredArgsConstructor
public class ReferenceSummaries {

	private static final ScopedValue<Batch> BATCH = ScopedValue.newInstance();

	private static final List<String> PERSON_NAME = List.of("first_name", "last_name");

	private static final Map<EntityRef, Label> LABELS = Map.ofEntries(
			Map.entry(EntityRef.ACADEMIC_YEAR, Label.named("name")),
			Map.entry(EntityRef.ASSET, new Label(List.of("name"), "asset_code")),
			Map.entry(EntityRef.ASSIGNMENT, Label.named("title")),
			Map.entry(EntityRef.BOARD, new Label(List.of("name"), "code")),
			Map.entry(EntityRef.BOOK, Label.named("title")),
			Map.entry(EntityRef.BOOK_COPY, Label.named("copy_code")),
			Map.entry(EntityRef.CLASSROOM, new Label(List.of("grade", "section"), "class_code")),
			Map.entry(EntityRef.CURRICULUM, new Label(List.of("name"), "code")),
			Map.entry(EntityRef.DEPARTMENT, new Label(List.of("name"), "code")),
			Map.entry(EntityRef.EMPLOYEE, new Label(PERSON_NAME, "employee_code")),
			Map.entry(EntityRef.EVENT, Label.named("title")),
			Map.entry(EntityRef.EXAM, Label.named("title")),
			Map.entry(EntityRef.EXAM_TYPE, new Label(List.of("name"), "code")),
			Map.entry(EntityRef.FEE_STRUCTURE, Label.named("name")),
			Map.entry(EntityRef.GRADING_SCALE, Label.named("name")),
			Map.entry(EntityRef.GUARDIAN, Label.named(PERSON_NAME)),
			Map.entry(EntityRef.HOSTEL_BUILDING, Label.named("name")),
			Map.entry(EntityRef.LEAVE_TYPE, Label.named("name")),
			Map.entry(EntityRef.PERIOD, Label.named("name")),
			Map.entry(EntityRef.ROOM, Label.named("room_number")),
			Map.entry(EntityRef.ROUTE, new Label(List.of("name"), "code")),
			Map.entry(EntityRef.ROUTE_STOP, Label.named("stop_name")),
			Map.entry(EntityRef.STUDENT, new Label(PERSON_NAME, "student_code")),
			Map.entry(EntityRef.SUBJECT, new Label(List.of("name"), "code")),
			Map.entry(EntityRef.TERM, Label.named("name")),
			Map.entry(EntityRef.USER, new Label(List.of("first_name", "last_name", "email"), null)),
			Map.entry(EntityRef.VEHICLE, Label.named("registration_number")),
			Map.entry(EntityRef.VENUE, new Label(List.of("name"), "code")));

	private final NamedParameterJdbcTemplate jdbcTemplate;

	/** Null for a null id, and for an id that names no record of the current tenant. */
	public ReferenceSummary of(EntityRef ref, Long id) {
		if (id == null) {
			return null;
		}
		return BATCH.isBound() ? BATCH.get().summary(ref, id) : load(ref, Set.of(id)).get(id);
	}

	public <T, R> Page<R> mapPage(Page<T> page, Function<T, R> mapper) {
		Batch batch = collect(page.getContent(), mapper);
		return ScopedValue.where(BATCH, batch).call(() -> page.map(mapper));
	}

	public <T, R> List<R> mapList(List<T> rows, Function<T, R> mapper) {
		Batch batch = collect(rows, mapper);
		return ScopedValue.where(BATCH, batch).call(() -> rows.stream().map(mapper).toList());
	}

	private <T, R> Batch collect(List<T> rows, Function<T, R> mapper) {
		Batch batch = new Batch();
		ScopedValue.where(BATCH, batch).run(() -> rows.forEach(mapper::apply));
		batch.wanted.forEach((ref, ids) -> batch.loaded.put(ref, load(ref, ids)));
		batch.collecting = false;
		return batch;
	}

	// Plain SQL on purpose: a reference to a soft-deleted record must still be labelled. Names are
	// joined here because string functions differ between database engines.
	private Map<Long, ReferenceSummary> load(EntityRef ref, Set<Long> ids) {
		Label label = LABELS.get(ref);
		String sql = "SELECT id, public_id, " + String.join(", ", label.columns()) + " FROM " + ref.table()
				+ " WHERE tenant_id = :tenantId AND id IN (:ids)";
		MapSqlParameterSource parameters = new MapSqlParameterSource()
				.addValue("tenantId", TenantContext.getCurrentTenantId())
				.addValue("ids", ids);
		Map<Long, ReferenceSummary> summaries = new HashMap<>();
		jdbcTemplate.query(sql, parameters, row -> {
			summaries.put(row.getLong("id"), label.summaryOf(row));
		});
		return summaries;
	}

	/** The last name column is a fallback when it follows a person's two name columns. */
	private record Label(List<String> nameColumns, String codeColumn) {

		static Label named(String nameColumn) {
			return new Label(List.of(nameColumn), null);
		}

		static Label named(List<String> nameColumns) {
			return new Label(nameColumns, null);
		}

		List<String> columns() {
			List<String> columns = new ArrayList<>(nameColumns);
			if (codeColumn != null) {
				columns.add(codeColumn);
			}
			return columns;
		}

		ReferenceSummary summaryOf(ResultSet row) throws SQLException {
			return new ReferenceSummary(row.getString("public_id"), nameOf(row),
					codeColumn == null ? null : row.getString(codeColumn));
		}

		private String nameOf(ResultSet row) throws SQLException {
			boolean hasFallback = nameColumns.size() > 2;
			List<String> parts = new ArrayList<>();
			for (String column : hasFallback ? nameColumns.subList(0, 2) : nameColumns) {
				String part = row.getString(column);
				if (part != null && !part.isBlank()) {
					parts.add(part);
				}
			}
			if (parts.isEmpty() && hasFallback) {
				return row.getString(nameColumns.get(2));
			}
			return String.join(" ", parts);
		}
	}

	private static final class Batch {

		private final Map<EntityRef, Set<Long>> wanted = new EnumMap<>(EntityRef.class);
		private final Map<EntityRef, Map<Long, ReferenceSummary>> loaded = new EnumMap<>(EntityRef.class);
		private boolean collecting = true;

		private ReferenceSummary summary(EntityRef ref, Long id) {
			if (collecting) {
				wanted.computeIfAbsent(ref, key -> new HashSet<>()).add(id);
				return null;
			}
			return loaded.get(ref).get(id);
		}
	}
}
