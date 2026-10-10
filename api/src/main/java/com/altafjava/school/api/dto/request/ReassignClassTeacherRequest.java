package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import com.altafjava.platform.core.concurrency.Versioned;

public record ReassignClassTeacherRequest(String teacherPublicId,
		@NotNull Long version) implements Versioned {
}
