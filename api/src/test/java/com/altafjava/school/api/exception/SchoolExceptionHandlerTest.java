package com.altafjava.school.api.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.altafjava.platform.api.dto.response.ApiResponse;

class SchoolExceptionHandlerTest {

	private final SchoolExceptionHandler handler = new SchoolExceptionHandler();

	@AfterEach
	void clearMdc() {
		MDC.clear();
	}

	@Test
	void illegalArgument_isABadRequestCarryingItsMessageAndTheRequestId() {
		MDC.put("request_id", "req-1234-abcd");

		ResponseEntity<ApiResponse<Object>> response = handler
				.handleIllegalArgument(new IllegalArgumentException("Receipt number already exists: R-1"));

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertFalse(response.getBody().success());
		assertEquals("INVALID_ARGUMENT", response.getBody().error().code());
		assertEquals("Receipt number already exists: R-1", response.getBody().error().message());
		assertEquals("req-1234-abcd", response.getBody().error().traceId());
	}

	@Test
	void anIdThatIsNotAUuid_isABadRequestToo() {
		try {
			java.util.UUID.fromString("not-a-uuid");
		} catch (IllegalArgumentException ex) {
			assertEquals(HttpStatus.BAD_REQUEST, handler.handleIllegalArgument(ex).getStatusCode());
		}
	}
}
