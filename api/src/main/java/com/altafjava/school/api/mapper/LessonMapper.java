package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LessonResponse;
import com.altafjava.school.domain.lms.model.Lesson;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LessonMapper {

	@Mapping(target = "publicId", expression = "java(lesson.getPublicId().toString())")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "subjectPublicId", source = "subjectId", qualifiedByName = "subject")
	@Mapping(target = "teacherPublicId", source = "teacherId", qualifiedByName = "employee")
	LessonResponse toResponse(Lesson lesson);
}
