package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.RoomAllocationResponse;
import com.altafjava.school.domain.hostel.model.RoomAllocation;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoomAllocationMapper {

	@Mapping(target = "publicId", expression = "java(allocation.getPublicId().toString())")
	@Mapping(target = "active", expression = "java(allocation.isActive())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "roomPublicId", source = "roomId", qualifiedByName = "room")
	RoomAllocationResponse toResponse(RoomAllocation allocation);
}
