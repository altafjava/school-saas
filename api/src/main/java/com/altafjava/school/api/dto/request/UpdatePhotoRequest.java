package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

// filePublicId is the UUID publicId of an already-uploaded, confirmed platform file (see
// FileStorageService#confirmUpload) — never a raw URL, so the existing quota/virus-scan/ownership
// controls apply. Nullable to support clearing a photo, mirroring UpdatePhoneRequest.
public record UpdatePhotoRequest(@Size(max = 36) String filePublicId,
		@NotNull Long version) implements Versioned {
}
