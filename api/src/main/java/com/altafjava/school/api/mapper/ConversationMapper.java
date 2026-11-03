package com.altafjava.school.api.mapper;

import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.ConversationMessageResponse;
import com.altafjava.school.api.dto.response.ConversationResponse;
import com.altafjava.school.api.dto.response.MessageReportResponse;
import com.altafjava.school.application.messaging.ConversationSummary;
import com.altafjava.school.domain.messaging.model.Conversation;
import com.altafjava.school.domain.messaging.model.ConversationMessage;
import com.altafjava.school.domain.messaging.model.MessageReport;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ConversationMapper {

	@Mapping(target = "publicId", expression = "java(conversation.getPublicId().toString())")
	@Mapping(target = "guardianPublicId", source = "guardianId", qualifiedByName = "guardian")
	@Mapping(target = "guardian", source = "guardianId", qualifiedByName = "guardianSummary")
	@Mapping(target = "teacherPublicId", source = "teacherId", qualifiedByName = "employee")
	@Mapping(target = "teacher", source = "teacherId", qualifiedByName = "employeeSummary")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "student", source = "studentId", qualifiedByName = "studentSummary")
	@Mapping(target = "unreadCount", constant = "0L")
	ConversationResponse toResponse(Conversation conversation);

	@Mapping(target = "publicId", expression = "java(summary.conversation().getPublicId().toString())")
	@Mapping(target = "guardianPublicId", source = "conversation.guardianId", qualifiedByName = "guardian")
	@Mapping(target = "guardian", source = "conversation.guardianId", qualifiedByName = "guardianSummary")
	@Mapping(target = "teacherPublicId", source = "conversation.teacherId", qualifiedByName = "employee")
	@Mapping(target = "teacher", source = "conversation.teacherId", qualifiedByName = "employeeSummary")
	@Mapping(target = "studentPublicId", source = "conversation.studentId", qualifiedByName = "student")
	@Mapping(target = "student", source = "conversation.studentId", qualifiedByName = "studentSummary")
	@Mapping(target = "lastMessageAt", source = "conversation.lastMessageAt")
	ConversationResponse toResponse(ConversationSummary summary);

	@Mapping(target = "publicId", expression = "java(message.getPublicId().toString())")
	@Mapping(target = "senderUserPublicId", source = "senderUserId", qualifiedByName = "user")
	@Mapping(target = "senderUser", source = "senderUserId", qualifiedByName = "userSummary")
	@Mapping(target = "erased", expression = "java(message.getErasedAt() != null)")
	@Mapping(target = "attachmentFilePublicIds", source = "attachmentFilePublicIds", qualifiedByName = "messageFileIds")
	@Mapping(target = "sentAt", source = "createdAt")
	ConversationMessageResponse toResponse(ConversationMessage message);

	@Mapping(target = "publicId", expression = "java(report.getPublicId().toString())")
	@Mapping(target = "conversationPublicId", source = "conversationId", qualifiedByName = "conversation")
	@Mapping(target = "messagePublicId", source = "messageId", qualifiedByName = "conversationMessage")
	@Mapping(target = "reportedByUserPublicId", source = "reportedByUserId", qualifiedByName = "user")
	@Mapping(target = "reportedByUser", source = "reportedByUserId", qualifiedByName = "userSummary")
	@Mapping(target = "resolvedByUserPublicId", source = "resolvedByUserId", qualifiedByName = "user")
	@Mapping(target = "resolvedByUser", source = "resolvedByUserId", qualifiedByName = "userSummary")
	@Mapping(target = "status", expression = "java(report.getStatus().name())")
	@Mapping(target = "reportedAt", source = "createdAt")
	MessageReportResponse toResponse(MessageReport report);

	@Named("messageFileIds")
	default List<String> fileIds(List<UUID> ids) {
		return ids.stream().map(UUID::toString).toList();
	}
}
