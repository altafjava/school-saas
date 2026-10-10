package com.altafjava.school.api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.StudentGuardianResponse;
import com.altafjava.school.application.student.StudentGuardian;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface StudentGuardianMapper {

	@Mapping(target = "relationshipType", expression = "java(studentGuardian.relationshipType().name())")
	StudentGuardianResponse toResponse(StudentGuardian studentGuardian);

	List<StudentGuardianResponse> toResponses(List<StudentGuardian> studentGuardians);
}
