package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.RoomResponse;
import com.altafjava.school.domain.hostel.model.Room;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RoomMapper {

	@Mapping(target = "publicId", expression = "java(room.getPublicId().toString())")
	@Mapping(target = "hostelBuildingPublicId", source = "hostelBuildingId", qualifiedByName = "hostelBuilding")
	RoomResponse toResponse(Room room);
}
