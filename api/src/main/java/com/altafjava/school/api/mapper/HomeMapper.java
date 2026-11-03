package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.home.ParentHomeResponse;
import com.altafjava.school.api.dto.response.home.StudentHomeResponse;
import com.altafjava.school.api.dto.response.home.TeacherHomeResponse;
import com.altafjava.school.application.home.ParentHome;
import com.altafjava.school.application.home.StudentHome;
import com.altafjava.school.application.home.TeacherHome;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface HomeMapper {

	TeacherHomeResponse toResponse(TeacherHome home);

	ParentHomeResponse toResponse(ParentHome home);

	StudentHomeResponse toResponse(StudentHome home);
}
