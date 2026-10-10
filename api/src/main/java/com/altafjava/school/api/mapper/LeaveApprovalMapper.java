package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LeaveApprovalResponse;
import com.altafjava.school.domain.leave.model.LeaveApproval;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeaveApprovalMapper {

	@Mapping(target = "publicId", expression = "java(approval.getPublicId().toString())")
	@Mapping(target = "stage", expression = "java(approval.getStage().name())")
	@Mapping(target = "decision", expression = "java(approval.getDecision().name())")
	@Mapping(target = "decidedByUserPublicId", source = "decidedByUserId", qualifiedByName = "user")
	LeaveApprovalResponse toResponse(LeaveApproval approval);
}
