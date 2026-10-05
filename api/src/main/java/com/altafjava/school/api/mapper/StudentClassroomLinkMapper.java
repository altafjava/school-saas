package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import com.altafjava.school.api.dto.response.StudentClassroomLinkResponse;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;

@Mapper(componentModel = "spring")
public interface StudentClassroomLinkMapper {

	// Publicly-known IDs are supplied by the caller (already resolved from the request) rather
	// than re-fetched from the entity's raw Long FKs — the entity itself never carries publicIds
	// for other aggregates. Written as a plain default method rather than a MapStruct-generated
	// multi-param mapping: MapStruct's generated null-guard for multi-source mappings only covers
	// fields resolved via implicit property matching, not @Mapping(expression = ...) fields, so
	// it left link.getPublicId() dereferenced unguarded (SpotBugs NP_NULL_ON_SOME_PATH).
	default StudentClassroomLinkResponse toResponse(StudentClassroomLink link, String studentPublicId,
			String classroomPublicId, String academicYearPublicId) {
		return new StudentClassroomLinkResponse(link.getPublicId().toString(), studentPublicId, classroomPublicId,
				academicYearPublicId, link.getEnrolledAt(), link.getRollNumber());
	}
}
