package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CounselingSessionResponse;
import com.altafjava.school.domain.counseling.model.CounselingSession;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CounselingSessionMapper {

	@Mapping(target = "publicId", expression = "java(session.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "counselorTeacherPublicId", source = "counselorTeacherId", qualifiedByName = "employee")
	CounselingSessionResponse toResponse(CounselingSession session);
}
