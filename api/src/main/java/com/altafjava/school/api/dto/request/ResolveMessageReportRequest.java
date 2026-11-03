package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.Size;

public record ResolveMessageReportRequest(@Size(max = 500) String note) {
}
