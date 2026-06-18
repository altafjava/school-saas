package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.domain.student.model.Student;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentMapper {

    @Mapping(target = "enrollmentStatus", expression = "java(student.getEnrollmentStatus().name())")
    StudentResponse toResponse(Student student);
}
