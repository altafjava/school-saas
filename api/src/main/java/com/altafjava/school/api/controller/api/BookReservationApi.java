package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.ReserveBookRequest;
import com.altafjava.school.api.dto.response.BookReservationResponse;
import com.altafjava.school.domain.library.model.ReservationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Book Reservation", description = "APIs for managing Book Reservation operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface BookReservationApi {

	@Operation(summary = "List", description = "Reservations of one book (its queue, oldest first) or of one student — pass exactly "
			+ "one of bookPublicId or studentPublicId.")
	public ApiResponse<com.altafjava.platform.core.model.Page<BookReservationResponse>> list(
			@RequestParam(required = false) String bookPublicId,
			@RequestParam(required = false) String studentPublicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) ReservationStatus status);

	@Operation(summary = "Reserve", description = "Queues an active student for a title with no copy on the shelf. When a copy is returned "
			+ "it is held for the longest-waiting reservation until the hold period (library.reservation.hold-days, "
			+ "default 3) runs out.")
	public ApiResponse<BookReservationResponse> reserve(@Valid @RequestBody ReserveBookRequest request);

	@Operation(summary = "Cancel", description = "Withdraws a reservation. A copy being held for it goes to the next member in the queue, "
			+ "or back on the shelf.")
	public ApiResponse<BookReservationResponse> cancel(@PathVariable String publicId);
}
