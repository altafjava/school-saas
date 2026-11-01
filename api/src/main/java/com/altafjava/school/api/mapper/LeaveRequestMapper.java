package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LeaveRequestResponse;
import com.altafjava.school.domain.leave.model.LeaveRequest;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LeaveRequestMapper {

	@Mapping(target = "publicId", expression = "java(leaveRequest.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(leaveRequest.getStatus().name())")
	@Mapping(target = "awaitingStage", expression = "java(leaveRequest.awaitingStage().map(Enum::name).orElse(null))")
	@Mapping(target = "employeePublicId", source = "employeeId", qualifiedByName = "employee")
	@Mapping(target = "employee", source = "employeeId", qualifiedByName = "employeeSummary")
	@Mapping(target = "leaveTypePublicId", source = "leaveTypeId", qualifiedByName = "leaveType")
	@Mapping(target = "leaveType", source = "leaveTypeId", qualifiedByName = "leaveTypeSummary")
	@Mapping(target = "approvedByUserPublicId", source = "approvedByUserId", qualifiedByName = "user")
	@Mapping(target = "approvedByUser", source = "approvedByUserId", qualifiedByName = "userSummary")
	LeaveRequestResponse toResponse(LeaveRequest leaveRequest);
}
