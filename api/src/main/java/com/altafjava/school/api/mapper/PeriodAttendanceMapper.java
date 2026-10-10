package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.PeriodAttendanceResponse;
import com.altafjava.school.domain.attendance.model.PeriodAttendance;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PeriodAttendanceMapper {

	@Mapping(target = "publicId", expression = "java(periodAttendance.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(periodAttendance.getStatus().name())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "timetableEntryPublicId", source = "timetableEntryId", qualifiedByName = "timetableEntry")
	PeriodAttendanceResponse toResponse(PeriodAttendance periodAttendance);
}
