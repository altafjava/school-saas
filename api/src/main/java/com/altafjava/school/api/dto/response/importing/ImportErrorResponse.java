package com.altafjava.school.api.dto.response.importing;

import com.altafjava.school.domain.importing.model.ImportPhase;

public record ImportErrorResponse(int lineNumber, String field, String message, ImportPhase phase) {
}
