package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.health.model.HealthRecord;
import com.altafjava.school.domain.health.model.HealthRecordCorrection;
import com.altafjava.school.domain.health.repository.HealthRecordCorrectionRepository;
import com.altafjava.school.domain.health.repository.HealthRecordRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class HealthRecordService {

	private final HealthRecordRepository healthRecordRepository;
	private final HealthRecordCorrectionRepository healthRecordCorrectionRepository;
	private final StudentRepository studentRepository;
	private final ActivityLogService activityLogService;

	public HealthRecordService(HealthRecordRepository healthRecordRepository,
			HealthRecordCorrectionRepository healthRecordCorrectionRepository, StudentRepository studentRepository,
			ActivityLogService activityLogService) {
		this.healthRecordRepository = healthRecordRepository;
		this.healthRecordCorrectionRepository = healthRecordCorrectionRepository;
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
			// Capture the pre-update values before mutating, mirroring GradeService#correct — an
			// upsert() overwrite with no prior row would otherwise make "what changed" and "what
			// was this PHI before" unanswerable.
			healthRecordCorrectionRepository.save(HealthRecordCorrection.record(record.getId(),
					record.getBloodGroup(), record.getAllergies(), record.getConditions(), record.getImmunizations(),
					bloodGroup, allergies, conditions, immunizations));
			record.update(bloodGroup, allergies, conditions, immunizations);
		}
		HealthRecord saved = healthRecordRepository.save(record);
		// Field values are deliberately excluded from this access-log entry (PHI-grade PII) — it
		// records who touched the record and when; the correction row above records what changed.
		logAccess(isNew ? "CREATE" : "UPDATE", studentPublicId,
				isNew ? "Health record created" : "Health record updated");
		return saved;
	}

	@Transactional(readOnly = true)
	public Page<HealthRecordCorrection> listCorrections(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		HealthRecord record = healthRecordRepository
				.findByStudentIdAndTenantId(resolveStudent(studentPublicId, tenantId).getId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Health record not found for student: " + studentPublicId));
		return healthRecordCorrectionRepository.findByHealthRecordIdAndTenantId(tenantId, record.getId(), pageable);
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
