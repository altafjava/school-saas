package com.altafjava.school.api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.SiblingResponse;
import com.altafjava.school.domain.student.model.Student;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface SiblingMapper {

	@Mapping(target = "publicId", expression = "java(student.getPublicId().toString())")
	@Mapping(target = "enrollmentStatus", expression = "java(student.getEnrollmentStatus().name())")
	@Mapping(target = "photoFilePublicId", expression = "java(student.getPhotoFilePublicId() != null ? student.getPhotoFilePublicId().toString() : null)")
	SiblingResponse toResponse(Student student);

	List<SiblingResponse> toResponseList(List<Student> students);
}
