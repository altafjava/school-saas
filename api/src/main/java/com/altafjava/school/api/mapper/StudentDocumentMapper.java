package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import com.altafjava.school.api.dto.response.StudentDocumentResponse;
import com.altafjava.school.domain.document.model.StudentDocument;

@Mapper(componentModel = "spring")
public interface StudentDocumentMapper {

	// studentPublicId is caller-supplied (already resolved from the request path), matching
	// StudentClassroomLinkMapper's pattern — the entity itself only carries the raw Long FK.
	default StudentDocumentResponse toResponse(StudentDocument document, String studentPublicId) {
		return new StudentDocumentResponse(
				document.getPublicId().toString(),
				studentPublicId,
				document.getDocumentType(),
				document.getFilePublicId().toString(),
				document.getVerificationStatus().name(),
				document.getVerifiedAt(),
				document.getRejectionReason());
	}
}
