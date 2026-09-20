package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.health.model.HealthRecord;
import com.altafjava.school.domain.health.repository.HealthRecordRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class HealthRecordService {

	private final HealthRecordRepository healthRecordRepository;
	private final StudentRepository studentRepository;
	private final ActivityLogService activityLogService;

	public HealthRecordService(HealthRecordRepository healthRecordRepository, StudentRepository studentRepository,
			ActivityLogService activityLogService) {
		this.healthRecordRepository = healthRecordRepository;
		this.studentRepository = studentRepository;
		this.activityLogService = activityLogService;
	}

	@Transactional(readOnly = true)
	public HealthRecord getByStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(studentPublicId, tenantId);
		HealthRecord record = healthRecordRepository.findByStudentIdAndTenantId(student.getId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Health record not found for student: "
						+ studentPublicId));
		logAccess("READ", studentPublicId, "Health record viewed");
		return record;
	}

	@Transactional
	public HealthRecord upsert(String studentPublicId, String bloodGroup, String allergies, String conditions,
			String immunizations) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = resolveStudent(studentPublicId, tenantId);
		HealthRecord record = healthRecordRepository.findByStudentIdAndTenantId(student.getId(), tenantId)
				.orElse(null);
		boolean isNew = record == null;
		if (isNew) {
			record = HealthRecord.create(student.getId(), bloodGroup, allergies, conditions, immunizations);
		} else {
			record.update(bloodGroup, allergies, conditions, immunizations);
		}
		HealthRecord saved = healthRecordRepository.save(record);
		// Field values are deliberately excluded from the audit trail (PHI-grade PII) — this
		// records who touched the record and when, not what changed; upsert() overwrites in place
		// with no prior-value history, so "what changed" isn't answerable here regardless.
		logAccess(isNew ? "CREATE" : "UPDATE", studentPublicId,
				isNew ? "Health record created" : "Health record updated");
		return saved;
	}

	private void logAccess(String action, String studentPublicId, String details) {
		activityLogService.log(TenantContext.getCurrentTenantId(), action, "HealthRecord", studentPublicId,
				currentActorId(), null, null, details, null, null);
	}

	private String currentActorId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null ? authentication.getName() : "system";
	}

	private Student resolveStudent(String studentPublicId, Long tenantId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
