package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CurrentClassroomResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.application.student.StudentPlacement;
import com.altafjava.school.domain.student.model.Student;

/** Maps what the student record holds; {@code StudentResponseAssembler} adds where the student sits. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR, uses = AddressMapper.class)
public interface StudentMapper {

	@Mapping(target = "publicId", expression = "java(student.getPublicId().toString())")
	@Mapping(target = "enrollmentStatus", expression = "java(student.getEnrollmentStatus().name())")
	@Mapping(target = "photoFilePublicId", expression = "java(student.getPhotoFilePublicId() != null ? student.getPhotoFilePublicId().toString() : null)")
	@Mapping(target = "currentClassroom", ignore = true)
	StudentResponse toResponseWithoutPlacement(Student student);

	@Mapping(target = "publicId", source = "classroomPublicId")
	@Mapping(target = "name", source = "classroomName")
	CurrentClassroomResponse toCurrentClassroom(StudentPlacement placement);
}
