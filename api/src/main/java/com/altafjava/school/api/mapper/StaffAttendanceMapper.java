package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.staffattendance.StaffAttendanceResponse;
import com.altafjava.school.api.dto.response.staffattendance.StaffRegisterEntryResponse;
import com.altafjava.school.api.dto.response.staffattendance.StaffWorkdayResponse;
import com.altafjava.school.application.staffattendance.RegisterEntry;
import com.altafjava.school.domain.staffattendance.model.StaffAttendance;
import com.altafjava.school.domain.staffattendance.model.StaffWorkday;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StaffAttendanceMapper {

	@Mapping(target = "publicId", expression = "java(day.getPublicId().toString())")
	@Mapping(target = "employeePublicId", source = "employeeId", qualifiedByName = "employee")
	@Mapping(target = "employee", source = "employeeId", qualifiedByName = "employeeSummary")
	@Mapping(target = "date", source = "attendanceDate")
	@Mapping(target = "workedMinutes", expression = "java(day.workedMinutes())")
	StaffAttendanceResponse toResponse(StaffAttendance day);

	@Mapping(target = "employeePublicId", expression = "java(entry.employee().getPublicId().toString())")
	@Mapping(target = "employee", expression = "java(new com.altafjava.school.application.reference.ReferenceSummary("
			+ "entry.employee().getPublicId().toString(), entry.employee().getFirstName() + \" \" + "
			+ "entry.employee().getLastName(), entry.employee().getEmployeeCode()))")
	@Mapping(target = "attendancePublicId", expression = "java(entry.attendance() == null ? null : "
			+ "entry.attendance().getPublicId().toString())")
	@Mapping(target = "checkInAt", expression = "java(entry.attendance() == null ? null : entry.attendance().getCheckInAt())")
	@Mapping(target = "checkOutAt", expression = "java(entry.attendance() == null ? null : "
			+ "entry.attendance().getCheckOutAt())")
	@Mapping(target = "late", expression = "java(entry.attendance() != null && entry.attendance().isLate())")
	@Mapping(target = "leftEarly", expression = "java(entry.attendance() != null && entry.attendance().isLeftEarly())")
	@Mapping(target = "leaveRequestPublicId", expression = "java(entry.leave() == null ? null : "
			+ "entry.leave().getPublicId().toString())")
	StaffRegisterEntryResponse toResponse(RegisterEntry entry);

	StaffWorkdayResponse toResponse(StaffWorkday workday);
}
