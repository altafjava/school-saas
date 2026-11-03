package com.altafjava.school.api.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.controller.api.ConversationApi;
import com.altafjava.school.api.dto.request.ReportMessageRequest;
import com.altafjava.school.api.dto.request.ResolveMessageReportRequest;
import com.altafjava.school.api.dto.request.SendMessageRequest;
import com.altafjava.school.api.dto.request.StartConversationRequest;
import com.altafjava.school.api.dto.response.ConversationMessageResponse;
import com.altafjava.school.api.dto.response.ConversationResponse;
import com.altafjava.school.api.dto.response.MessageReportResponse;
import com.altafjava.school.api.dto.response.UnreadCountResponse;
import com.altafjava.school.api.mapper.ConversationMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.messaging.ConversationReviewService;
import com.altafjava.school.application.messaging.ConversationService;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.domain.messaging.model.Conversation;
import com.altafjava.school.domain.messaging.model.MessageReportStatus;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController implements ConversationApi {

	private final ConversationService conversationService;
	private final ConversationReviewService reviewService;
	private final ConversationMapper conversationMapper;
	private final SpringDataPageableResolver pageableResolver;
	private final ReferenceSummaries referenceSummaries;

	@Override
	@GetMapping
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				conversationService.list(user.getId(), pageableResolver.resolve(page, size)),
				conversationMapper::toResponse)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public ApiResponse<ConversationResponse> start(@Valid @RequestBody StartConversationRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		Conversation conversation = conversationService.start(new ConversationService.NewConversation(
				request.studentPublicId(), request.teacherPublicId(), request.guardianPublicId(), request.body(),
				request.attachmentFilePublicIds()), user.getId());
		return ApiResponse.success(conversationMapper.toResponse(conversation));
	}

	@Override
	@GetMapping("/unread-count")
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public ApiResponse<UnreadCountResponse> unreadCount(@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(new UnreadCountResponse(conversationService.unreadCount(user.getId())));
	}

	@Override
	@GetMapping("/{publicId}/messages")
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationMessageResponse>> messages(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				conversationService.messages(publicId, user.getId(), pageableResolver.resolve(page, size)),
				conversationMapper::toResponse)));
	}

	@Override
	@PostMapping("/{publicId}/messages")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public ApiResponse<ConversationMessageResponse> send(@PathVariable String publicId,
			@Valid @RequestBody SendMessageRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(conversationMapper.toResponse(conversationService.send(publicId, request.body(),
				request.attachmentFilePublicIds(), user.getId())));
	}

	@Override
	@PatchMapping("/{publicId}/read")
	@Command
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public void markRead(@PathVariable String publicId, @AuthenticationPrincipal AuthenticatedUser user) {
		conversationService.markRead(publicId, user.getId());
	}

	@Override
	@PostMapping("/{publicId}/messages/{messagePublicId}/report")
	@ResponseStatus(HttpStatus.CREATED)
	@RequiresPermission(SchoolPermissions.MESSAGING_PARTICIPATE)
	public ApiResponse<MessageReportResponse> report(@PathVariable String publicId,
			@PathVariable String messagePublicId, @Valid @RequestBody ReportMessageRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(conversationMapper.toResponse(
				conversationService.report(publicId, messagePublicId, request.reason(), user.getId())));
	}

	@Override
	@GetMapping("/review")
	@RequiresPermission(SchoolPermissions.MESSAGING_REVIEW)
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationResponse>> reviewList(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				reviewService.list(pageableResolver.resolve(page, size)), conversationMapper::toResponse)));
	}

	@Override
	@GetMapping("/review/{publicId}/messages")
	@RequiresPermission(SchoolPermissions.MESSAGING_REVIEW)
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationMessageResponse>> reviewMessages(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				reviewService.messages(publicId, pageableResolver.resolve(page, size)),
				conversationMapper::toResponse)));
	}

	@Override
	@GetMapping("/review/reports")
	@RequiresPermission(SchoolPermissions.MESSAGING_REVIEW)
	public ApiResponse<com.altafjava.platform.core.model.Page<MessageReportResponse>> reports(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) MessageReportStatus status) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(referenceSummaries.mapPage(
				reviewService.reports(status, pageableResolver.resolve(page, size)),
				conversationMapper::toResponse)));
	}

	@Override
	@PatchMapping("/review/reports/{publicId}/resolve")
	@Command
	@RequiresPermission(SchoolPermissions.MESSAGING_REVIEW)
	public ApiResponse<MessageReportResponse> resolve(@PathVariable String publicId,
			@Valid @RequestBody ResolveMessageReportRequest request,
			@AuthenticationPrincipal AuthenticatedUser user) {
		return ApiResponse.success(
				conversationMapper.toResponse(reviewService.resolve(publicId, request.note(), user.getId())));
	}
}
