package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AttendanceScanResponse;
import com.altafjava.school.application.service.AttendanceScanResult;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AttendanceScanMapper {

	@Mapping(target = "attendancePublicId", expression = "java(result.attendance().getPublicId().toString())")
	@Mapping(target = "studentPublicId", expression = "java(result.student().getPublicId().toString())")
	@Mapping(target = "studentName", expression = "java(result.student().getFirstName() + \" \" + result.student().getLastName())")
	@Mapping(target = "className", expression = "java(result.classroom().getGrade() + \" \" + result.classroom().getSection())")
	@Mapping(target = "attendanceDate", source = "attendance.attendanceDate")
	@Mapping(target = "status", expression = "java(result.attendance().getStatus().name())")
	@Mapping(target = "alreadyMarked", source = "alreadyMarked")
	AttendanceScanResponse toResponse(AttendanceScanResult result);
}
