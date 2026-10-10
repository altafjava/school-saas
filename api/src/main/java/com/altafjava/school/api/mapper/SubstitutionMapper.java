package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AvailableTeacherResponse;
import com.altafjava.school.api.dto.response.SubstitutionResponse;
import com.altafjava.school.domain.teacher.model.Teacher;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SubstitutionMapper {

	@Mapping(target = "publicId", expression = "java(details.substitution().getPublicId().toString())")
	@Mapping(target = "date", source = "substitution.substitutionDate")
	@Mapping(target = "active", expression = "java(details.substitution().isActive())")
	@Mapping(target = "timetableEntryPublicId", expression = "java(details.entry().getPublicId().toString())")
	@Mapping(target = "dayOfWeek", expression = "java(details.entry().getDayOfWeek().name())")
	@Mapping(target = "periodPublicId", source = "entry.periodId", qualifiedByName = "period")
	@Mapping(target = "classroomPublicId", source = "entry.classroomId", qualifiedByName = "classroom")
	@Mapping(target = "subjectPublicId", source = "entry.subjectId", qualifiedByName = "subject")
	@Mapping(target = "regularTeacherPublicId", expression = "java(details.regularTeacher().getPublicId().toString())")
	@Mapping(target = "regularTeacherName", expression = "java(details.regularTeacher().getFirstName() + \" \" + details.regularTeacher().getLastName())")
	@Mapping(target = "substituteTeacherPublicId", expression = "java(details.substituteTeacher().getPublicId().toString())")
	@Mapping(target = "substituteTeacherName", expression = "java(details.substituteTeacher().getFirstName() + \" \" + details.substituteTeacher().getLastName())")
	@Mapping(target = "reason", source = "substitution.reason")
	@Mapping(target = "cancelledAt", source = "substitution.cancelledAt")
	@Mapping(target = "cancellationReason", source = "substitution.cancellationReason")
	SubstitutionResponse toResponse(com.altafjava.school.application.service.SubstitutionDetails details);

	@Mapping(target = "publicId", expression = "java(teacher.getPublicId().toString())")
	@Mapping(target = "name", expression = "java(teacher.getFirstName() + \" \" + teacher.getLastName())")
	AvailableTeacherResponse toAvailableTeacher(Teacher teacher);
}
