package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.StudentLeaveRequestResponse;
import com.altafjava.school.domain.leave.model.StudentLeaveRequest;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentLeaveRequestMapper {

	@Mapping(target = "publicId", expression = "java(request.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(request.getStatus().name())")
	@Mapping(target = "attachmentFilePublicId", expression = "java(request.getAttachmentFilePublicId() == null ? null : request.getAttachmentFilePublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "student", source = "studentId", qualifiedByName = "studentSummary")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "classroom", source = "classroomId", qualifiedByName = "classroomSummary")
	@Mapping(target = "decidedByUserPublicId", source = "decidedByUserId", qualifiedByName = "user")
	@Mapping(target = "decidedByUser", source = "decidedByUserId", qualifiedByName = "userSummary")
	StudentLeaveRequestResponse toResponse(StudentLeaveRequest request);
}
