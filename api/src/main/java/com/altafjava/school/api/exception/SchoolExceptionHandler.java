package com.altafjava.school.api.exception;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.altafjava.platform.api.dto.response.ApiResponse;

/**
 * Answers 400 for an {@link IllegalArgumentException} — school services and domain calculators use
 * it for a rejected input (a duplicate receipt number, marks above the maximum) and the id parsers
 * for a malformed identifier — instead of letting it reach the platform's catch-all as a 500 that
 * is logged as an unexpected failure. Ordered ahead of the platform handler, which would otherwise
 * claim it through its {@code Exception} handler.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchoolExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
		String requestId = MDC.get("request_id");
		String traceId = requestId != null ? requestId : UUID.randomUUID().toString();
		return new ResponseEntity<>(ApiResponse.error("INVALID_ARGUMENT", ex.getMessage(), null, traceId),
				HttpStatus.BAD_REQUEST);
	}
}
