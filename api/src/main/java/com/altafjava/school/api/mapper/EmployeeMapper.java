package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.EmployeeResponse;
import com.altafjava.school.domain.employee.model.Employee;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = { AddressMapper.class,
		PublicIdMapping.class })
public interface EmployeeMapper {

	@Mapping(target = "publicId", expression = "java(employee.getPublicId().toString())")
	@Mapping(target = "staffCategory", expression = "java(employee.getStaffCategory().name())")
	@Mapping(target = "status", expression = "java(employee.getStatus().name())")
	@Mapping(target = "photoFilePublicId", expression = "java(employee.getPhotoFilePublicId() != null ? employee.getPhotoFilePublicId().toString() : null)")
	@Mapping(target = "departmentPublicId", source = "departmentId", qualifiedByName = "department")
	EmployeeResponse toResponse(Employee employee);
}
