package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.AnnouncementApi;
import com.altafjava.school.api.dto.request.CreateAnnouncementRequest;
import com.altafjava.school.api.dto.request.ScheduleAnnouncementRequest;
import com.altafjava.school.api.dto.request.SendEmergencyAlertRequest;
import com.altafjava.school.api.dto.request.UpdateAnnouncementRequest;
import com.altafjava.school.api.dto.response.AnnouncementDeliveryResponse;
import com.altafjava.school.api.dto.response.AnnouncementFeedItemResponse;
import com.altafjava.school.api.dto.response.AnnouncementRecipientResponse;
import com.altafjava.school.api.dto.response.AnnouncementResponse;
import com.altafjava.school.api.dto.response.UnreadCountResponse;
import com.altafjava.school.api.mapper.AnnouncementMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.announcement.AnnouncementService;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.domain.announcement.model.AnnouncementStatus;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/announcements")
@RequiredArgsConstructor
public class AnnouncementController implements AnnouncementApi {

	private final AnnouncementService announcementService;
	private final AnnouncementMapper announcementMapper;
	private final SpringDataPageableResolver pageableResolver;
	private final ReferenceSummaries referenceSummaries;

	@Override
	@GetMapping
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	@SortableBy({ "publishedAt", "publishAt", "title", "status" })
	public ApiResponse<com.altafjava.platform.core.model.Page<AnnouncementResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) AnnouncementStatus status,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				announcementService.list(status, user.getId(), pageableResolver.resolve(page, size)),
				announcementMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementResponse> get(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper.toResponse(announcementService.get(publicId, user.getId())));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementResponse> create(@Valid @RequestBody CreateAnnouncementRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper.toResponse(announcementService.create(
				new AnnouncementService.Draft(request.title(), request.body(), request.audienceType(),
						request.targets(), request.includeStudents(), request.channels(),
						request.attachmentFilePublicIds(), request.expiresAt()),
				user.getId())));
	}

	@Override
	@PutMapping("/{publicId}")
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementResponse> update(@PathVariable String publicId,
			@Valid @RequestBody UpdateAnnouncementRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper.toResponse(announcementService.update(publicId,
				new AnnouncementService.Draft(request.title(), request.body(), request.audienceType(),
						request.targets(), request.includeStudents(), request.channels(),
						request.attachmentFilePublicIds(), request.expiresAt()),
				request.expectedVersion(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/schedule")
	@Command
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementResponse> schedule(@PathVariable String publicId,
			@Valid @RequestBody ScheduleAnnouncementRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper
				.toResponse(announcementService.schedule(publicId, request.publishAt(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/publish")
	@Command
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementResponse> publish(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper.toResponse(announcementService.publish(publicId, user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/cancel")
	@Command
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementResponse> cancel(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper.toResponse(announcementService.cancel(publicId, user.getId())));
	}

	@Override
	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public void delete(@PathVariable String publicId, @AuthenticationPrincipal AuthenticatedUser user) {
		announcementService.delete(publicId, user.getId());
	}

	@Override
	@PostMapping("/emergency")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.EMERGENCY_ALERT_SEND)
	public ApiResponse<AnnouncementResponse> sendEmergency(@Valid @RequestBody SendEmergencyAlertRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(announcementMapper.toResponse(announcementService.sendEmergency(
				new AnnouncementService.Draft(request.title(), request.body(), request.audienceType(),
						request.targets(), request.includeStudents(), null, null, null),
				user.getId())));
	}

	@Override
	@GetMapping("/{publicId}/delivery")
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<AnnouncementDeliveryResponse> delivery(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse
				.success(announcementMapper.toDelivery(announcementService.deliveryReport(publicId, user.getId())));
	}

	@Override
	@GetMapping("/{publicId}/recipients")
	@RequiresPermission({ SchoolPermissions.ANNOUNCEMENT_MANAGE, SchoolPermissions.ANNOUNCEMENT_CLASS_POST })
	public ApiResponse<com.altafjava.platform.core.model.Page<AnnouncementRecipientResponse>> recipients(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				announcementService.recipients(publicId, user.getId(), pageableResolver.resolve(page, size)),
				announcementMapper::toRecipient)));
	}

	@Override
	@GetMapping("/feed")
	@RequiresPermission(SchoolPermissions.ANNOUNCEMENT_READ)
	public ApiResponse<com.altafjava.platform.core.model.Page<AnnouncementFeedItemResponse>> feed(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(defaultValue = "false") boolean unreadOnly,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				announcementService.feed(user.getId(), unreadOnly, pageableResolver.resolve(page, size))
						.map(announcementMapper::toFeedItem)));
	}

	@Override
	@GetMapping("/feed/unread-count")
	@RequiresPermission(SchoolPermissions.ANNOUNCEMENT_READ)
	public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(new UnreadCountResponse(announcementService.unreadCount(user.getId())));
	}

	@Override
	@GetMapping("/feed/{publicId}")
	@RequiresPermission(SchoolPermissions.ANNOUNCEMENT_READ)
	public ApiResponse<AnnouncementFeedItemResponse> feedItem(@PathVariable String publicId,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse
				.success(announcementMapper.toFeedItem(announcementService.readFromFeed(publicId, user.getId())));
	}

	@Override
	@PatchMapping("/feed/{publicId}/read")
	@Command
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresPermission(SchoolPermissions.ANNOUNCEMENT_READ)
	public void markRead(@PathVariable String publicId, @AuthenticationPrincipal AuthenticatedUser user) {
		announcementService.markRead(publicId, user.getId());
	}
}
