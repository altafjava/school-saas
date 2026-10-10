package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.FeeAssignmentResponse;
import com.altafjava.school.domain.fee.model.FeeAssignment;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeAssignmentMapper {

	@Mapping(target = "publicId", expression = "java(feeAssignment.getPublicId().toString())")
	@Mapping(target = "scope", expression = "java(feeAssignment.getScope().name())")
	@Mapping(target = "feeStructurePublicId", source = "feeStructureId", qualifiedByName = "feeStructure")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	FeeAssignmentResponse toResponse(FeeAssignment feeAssignment);
}
