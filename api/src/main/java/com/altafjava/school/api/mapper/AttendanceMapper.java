package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.domain.attendance.model.Attendance;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttendanceMapper {

	@Mapping(target = "publicId", expression = "java(attendance.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(attendance.getStatus().name())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	AttendanceResponse toResponse(Attendance attendance);
}
