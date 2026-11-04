package com.altafjava.school.application.service;

import java.util.Map;
import com.altafjava.platform.application.service.approval.ApprovalAction;
import com.altafjava.platform.application.service.approval.ApprovalHandler;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs once a tenant's {@code ADMISSION_DECISION} approval workflow's final stage approves —
 * {@code entityId} is the admission's public ID ({@code AdmissionService#requestApproval}'s
 * {@code entityIdExpression}), {@code payload} the JSON object built from its
 * {@code payloadExpression}.
 */
@ApprovalHandler("ADMISSION_DECISION")
@RequiredArgsConstructor
public class AdmissionApprovalHandler implements ApprovalAction {

	private final AdmissionService admissionService;
	private final ObjectMapper objectMapper;

	@Override
	public void execute(String entityId, String payload) {
		Map<String, Object> decoded = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
		});
		String studentCode = stringValue(decoded, "studentCode");
		if (studentCode == null || studentCode.isBlank()) {
			throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR,
					"studentCode missing from approved admission-decision payload");
		}
		admissionService.finalizeApproval(entityId, stringValue(decoded, "decidedBy"), stringValue(decoded, "notes"),
				studentCode);
	}

	private String stringValue(Map<String, Object> map, String key) {
		Object value = map.get(key);
		return value != null ? value.toString() : null;
	}
}
