package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// filePublicId is the UUID publicId of an already-uploaded, confirmed platform file (see
// FileStorageService#confirmUpload), same convention as UpdatePhotoRequest.
public record UploadStudentDocumentRequest(
		@NotBlank @Size(max = 100) String documentType,
		@NotBlank @Size(max = 36) String filePublicId) {
}
