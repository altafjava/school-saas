package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TeacherResponse;
import com.altafjava.school.domain.teacher.model.Teacher;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = { AddressMapper.class,
		SchoolPublicIdMapping.class })
public interface TeacherMapper {

	@Mapping(target = "publicId", expression = "java(teacher.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(teacher.getStatus().name())")
	@Mapping(target = "photoFilePublicId", expression = "java(teacher.getPhotoFilePublicId() != null ? teacher.getPhotoFilePublicId().toString() : null)")
	@Mapping(target = "departmentPublicId", source = "departmentId", qualifiedByName = "department")
	TeacherResponse toResponse(Teacher teacher);
}
