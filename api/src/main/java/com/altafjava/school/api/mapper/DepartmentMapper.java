package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.DepartmentResponse;
import com.altafjava.school.domain.department.model.Department;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface DepartmentMapper {

	@Mapping(target = "publicId", expression = "java(department.getPublicId().toString())")
	@Mapping(target = "headEmployeePublicId", source = "headEmployeeId", qualifiedByName = "employee")
	DepartmentResponse toResponse(Department department);
}
