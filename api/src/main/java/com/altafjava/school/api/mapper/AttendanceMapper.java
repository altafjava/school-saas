package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.domain.attendance.model.Attendance;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttendanceMapper {

	@Mapping(target = "publicId", expression = "java(attendance.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(attendance.getStatus().name())")
	AttendanceResponse toResponse(Attendance attendance);
}
