package com.altafjava.school.api.dto.response;

public record CertificateTypeResponse(String publicId, String code, String documentType, String name, String wording,
		boolean active) {
}
