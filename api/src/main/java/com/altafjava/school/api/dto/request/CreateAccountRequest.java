package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import io.swagger.v3.oas.annotations.media.Schema;

/** Send {@code userPublicId} to link an existing login, or {@code email} and {@code password} to create one. */
public record CreateAccountRequest(
		String userPublicId,
		@Email String email,
		@Schema(accessMode = Schema.AccessMode.WRITE_ONLY) String password,
		@Schema(description = "Role for a new staff login; a teacher's or student's role is fixed") String role) {

	@AssertTrue(message = "Send either userPublicId, or email and password")
	@Schema(hidden = true)
	public boolean isModeValid() {
		boolean linking = userPublicId != null;
		boolean creating = email != null && password != null;
		return linking != creating;
	}
}
