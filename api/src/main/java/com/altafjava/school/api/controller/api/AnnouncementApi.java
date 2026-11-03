package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.CreateAnnouncementRequest;
import com.altafjava.school.api.dto.request.ScheduleAnnouncementRequest;
import com.altafjava.school.api.dto.request.SendEmergencyAlertRequest;
import com.altafjava.school.api.dto.request.UpdateAnnouncementRequest;
import com.altafjava.school.api.dto.response.AnnouncementDeliveryResponse;
import com.altafjava.school.api.dto.response.AnnouncementFeedItemResponse;
import com.altafjava.school.api.dto.response.AnnouncementRecipientResponse;
import com.altafjava.school.api.dto.response.AnnouncementResponse;
import com.altafjava.school.api.dto.response.UnreadCountResponse;
import com.altafjava.school.domain.announcement.model.AnnouncementStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Announcement", description = "Notices and circulars: drafted, scheduled, published to a chosen audience and tracked per channel.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface AnnouncementApi {

	@Operation(summary = "List", description = "Managers see every announcement, teachers their own.")
	public ApiResponse<com.altafjava.platform.core.model.Page<AnnouncementResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) AnnouncementStatus status,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Get")
	public ApiResponse<AnnouncementResponse> get(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Create", description = "Creates a draft. Teachers can target only classrooms they teach. Classroom and guardian targets are public ids; grade and role targets are names.")
	public ApiResponse<AnnouncementResponse> create(@Valid @RequestBody CreateAnnouncementRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Update", description = "Only a draft or scheduled announcement can change.")
	public ApiResponse<AnnouncementResponse> update(@PathVariable String publicId,
			@Valid @RequestBody UpdateAnnouncementRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Schedule", description = "Publishes automatically at the given time, within about a minute.")
	public ApiResponse<AnnouncementResponse> schedule(@PathVariable String publicId,
			@Valid @RequestBody ScheduleAnnouncementRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Publish", description = "Resolves the audience now and starts sending. Fails if nobody in the audience has an account.")
	public ApiResponse<AnnouncementResponse> publish(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Cancel")
	public ApiResponse<AnnouncementResponse> cancel(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Delete", description = "Only a draft can be deleted.")
	public void delete(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Send Emergency Alert", description = "Publishes at once on every channel, ignoring quiet hours and optional preferences. A recipient's WhatsApp opt-out still applies. Always audited.")
	public ApiResponse<AnnouncementResponse> sendEmergency(@Valid @RequestBody SendEmergencyAlertRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Delivery Report", description = "Read counts, and per-channel counts including what was skipped and why.")
	public ApiResponse<AnnouncementDeliveryResponse> delivery(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Recipients", description = "Everyone the announcement reached, with their read state.")
	public ApiResponse<com.altafjava.platform.core.model.Page<AnnouncementRecipientResponse>> recipients(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "My Feed", description = "Published, unexpired announcements addressed to the caller. Emergency alerts come first.")
	public ApiResponse<com.altafjava.platform.core.model.Page<AnnouncementFeedItemResponse>> feed(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Unread Count")
	public ApiResponse<UnreadCountResponse> unreadCount(
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Get From Feed", description = "An announcement not addressed to the caller answers 404.")
	public ApiResponse<AnnouncementFeedItemResponse> feedItem(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Mark Read")
	public void markRead(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);
}
