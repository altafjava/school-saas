package com.altafjava.school.api.controller.api;

import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.ReportMessageRequest;
import com.altafjava.school.api.dto.request.ResolveMessageReportRequest;
import com.altafjava.school.api.dto.request.SendMessageRequest;
import com.altafjava.school.api.dto.request.StartConversationRequest;
import com.altafjava.school.api.dto.response.ConversationMessageResponse;
import com.altafjava.school.api.dto.response.ConversationResponse;
import com.altafjava.school.api.dto.response.MessageReportResponse;
import com.altafjava.school.api.dto.response.UnreadCountResponse;
import com.altafjava.school.domain.messaging.model.MessageReportStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Conversation", description = "One-to-one messages between a guardian and a teacher of their child, read-only to the school's named reviewers.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface ConversationApi {

	@Operation(summary = "List Mine", description = "The caller's conversations, newest first, with an unread count on each.")
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Start", description = "A guardian names a teacher of their child, or a teacher names a guardian of a child they teach. Sends the first message, reusing the thread if one exists.")
	public ApiResponse<ConversationResponse> start(@Valid @RequestBody StartConversationRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Unread Count")
	public ApiResponse<UnreadCountResponse> unreadCount(
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Messages", description = "Newest first. Participants only.")
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationMessageResponse>> messages(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Send", description = "Refused while messaging is off, outside the school's hours, or once the guardian or teacher no longer has this child.")
	public ApiResponse<ConversationMessageResponse> send(@PathVariable String publicId,
			@Valid @RequestBody SendMessageRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Mark Read")
	public void markRead(@PathVariable String publicId,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Report Message", description = "Flags the other person's message for the school's reviewers.")
	public ApiResponse<MessageReportResponse> report(@PathVariable String publicId,
			@PathVariable String messagePublicId, @Valid @RequestBody ReportMessageRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);

	@Operation(summary = "Review: List")
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationResponse>> reviewList(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Review: Messages", description = "Read-only. Each call is written to the audit log with the reviewer's name.")
	public ApiResponse<com.altafjava.platform.core.model.Page<ConversationMessageResponse>> reviewMessages(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Review: Reports")
	public ApiResponse<com.altafjava.platform.core.model.Page<MessageReportResponse>> reports(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) MessageReportStatus status);

	@Operation(summary = "Review: Resolve Report")
	public ApiResponse<MessageReportResponse> resolve(@PathVariable String publicId,
			@Valid @RequestBody ResolveMessageReportRequest request,
			@Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user);
}
