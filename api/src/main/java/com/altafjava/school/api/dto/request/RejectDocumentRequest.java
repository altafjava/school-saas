package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDocumentRequest(@NotBlank @Size(max = 500) String rejectionReason) {
}
