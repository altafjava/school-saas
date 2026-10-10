package com.altafjava.school.application.support;

import static org.mockito.Mockito.RETURNS_DEFAULTS;
import static org.mockito.Mockito.mock;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.altafjava.platform.core.model.BaseEntity;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.teacher.model.Teacher;

/** Stand-ins for the records a service looks up by public id before working on their internal ids. */
public final class TestEntities {

	private TestEntities() {
	}

	/** A stable public id for "the classroom the test calls 10", so stubs and calls agree. */
	public static UUID publicId(String kind, long number) {
		return UUID.nameUUIDFromBytes((kind + number).getBytes(StandardCharsets.UTF_8));
	}

	// Answers come from the mock's default answer, not when(): these are built inside thenReturn(...).
	public static <T extends BaseEntity> T withId(Class<T> type, Long id) {
		return mock(type, invocation -> "getId".equals(invocation.getMethod().getName()) ? id
				: RETURNS_DEFAULTS.answer(invocation));
	}

	public static Teacher activeTeacher(Long id) {
		return mock(Teacher.class, invocation -> switch (invocation.getMethod().getName()) {
			case "getId" -> id;
			case "getStatus" -> EmployeeStatus.ACTIVE;
			default -> RETURNS_DEFAULTS.answer(invocation);
		});
	}
}
