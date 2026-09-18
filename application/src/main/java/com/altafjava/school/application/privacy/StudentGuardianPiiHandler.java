package com.altafjava.school.application.privacy;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.privacy.DomainPiiHandler;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;
import com.altafjava.school.domain.discipline.repository.DisciplineIncidentRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.health.model.HealthRecord;
import com.altafjava.school.domain.health.model.MedicalIncident;
import com.altafjava.school.domain.health.repository.HealthRecordRepository;
import com.altafjava.school.domain.health.repository.MedicalIncidentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

/**
 * school-saas's side of the GDPR/DPDP data-subject-request extension point — see
 * {@link DomainPiiHandler}, wired via {@code SchoolPlatformConfigurer#domainPiiHandler()}. A
 * subject may have a linked {@link Student} record, a linked {@link Guardian} record, both, or
 * neither ({@code userId} is nullable on both — see their own Javadoc); this handler checks both
 * independently rather than assuming exactly one exists.
 *
 * <p>
 * When the subject has a linked {@link Student}, this also covers the student's health,
 * discipline, and counseling records — the most sensitive data this domain holds, keyed by {@code
 * studentId} rather than {@code userId} and easy to miss for exactly that reason. It does not
 * cover records where the student is only a bystander (e.g. a {@code DisciplineIncident} authored
 * by a different student's report), since those belong to that other student's own DSAR.
 */
@Component
public class StudentGuardianPiiHandler implements DomainPiiHandler {

	private final StudentRepository studentRepository;
	private final GuardianRepository guardianRepository;
	private final HealthRecordRepository healthRecordRepository;
	private final MedicalIncidentRepository medicalIncidentRepository;
	private final DisciplineIncidentRepository disciplineIncidentRepository;
	private final CounselingSessionRepository counselingSessionRepository;

	public StudentGuardianPiiHandler(
			StudentRepository studentRepository,
			GuardianRepository guardianRepository,
			HealthRecordRepository healthRecordRepository,
			MedicalIncidentRepository medicalIncidentRepository,
			DisciplineIncidentRepository disciplineIncidentRepository,
			CounselingSessionRepository counselingSessionRepository) {
		this.studentRepository = studentRepository;
		this.guardianRepository = guardianRepository;
		this.healthRecordRepository = healthRecordRepository;
		this.medicalIncidentRepository = medicalIncidentRepository;
		this.disciplineIncidentRepository = disciplineIncidentRepository;
		this.counselingSessionRepository = counselingSessionRepository;
	}

	@Override
	@Transactional
	public void erase(Long tenantId, Long userId) {
		studentRepository.findByUserIdAndTenantId(userId, tenantId).ifPresent(student -> {
			eraseStudentSensitiveRecords(student.getId(), tenantId);
			student.erasePii();
			student.softDelete("gdpr-dsar-erasure");
			studentRepository.save(student);
		});
		guardianRepository.findByUserIdAndTenantId(userId, tenantId).ifPresent(guardian -> {
			guardian.erasePii();
			guardian.softDelete("gdpr-dsar-erasure");
			guardianRepository.save(guardian);
		});
	}

	private void eraseStudentSensitiveRecords(Long studentId, Long tenantId) {
		healthRecordRepository.findByStudentIdAndTenantId(studentId, tenantId).ifPresent(record -> {
			record.erasePii();
			healthRecordRepository.save(record);
		});
		medicalIncidentRepository.findAllByStudentIdAndTenantId(studentId, tenantId).forEach(incident -> {
			incident.erasePii();
			medicalIncidentRepository.save(incident);
		});
		disciplineIncidentRepository.findAllByStudentIdAndTenantId(studentId, tenantId).forEach(incident -> {
			incident.erasePii();
			disciplineIncidentRepository.save(incident);
		});
		counselingSessionRepository.findAllByStudentIdAndTenantId(studentId, tenantId).forEach(session -> {
			session.erasePii();
			counselingSessionRepository.save(session);
		});
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> export(Long tenantId, Long userId) {
		Map<String, Object> data = new HashMap<>();
		studentRepository.findByUserIdAndTenantId(userId, tenantId).ifPresent(student -> {
			data.put("student", toStudentExport(student));
			data.put("healthRecord", healthRecordRepository.findByStudentIdAndTenantId(student.getId(), tenantId)
					.map(this::toHealthRecordExport)
					.orElse(null));
			data.put("medicalIncidents", medicalIncidentRepository
					.findAllByStudentIdAndTenantId(student.getId(), tenantId).stream()
					.map(this::toMedicalIncidentExport).toList());
			data.put("disciplineIncidents", disciplineIncidentRepository
					.findAllByStudentIdAndTenantId(student.getId(), tenantId).stream()
					.map(this::toDisciplineIncidentExport).toList());
			data.put("counselingSessions", counselingSessionRepository
					.findAllByStudentIdAndTenantId(student.getId(), tenantId).stream()
					.map(this::toCounselingSessionExport).toList());
		});
		guardianRepository.findByUserIdAndTenantId(userId, tenantId)
				.ifPresent(guardian -> data.put("guardian", toGuardianExport(guardian)));
		return data;
	}

	private Map<String, Object> toHealthRecordExport(HealthRecord record) {
		Map<String, Object> export = new HashMap<>();
		export.put("bloodGroup", record.getBloodGroup());
		export.put("allergies", record.getAllergies());
		export.put("conditions", record.getConditions());
		export.put("immunizations", record.getImmunizations());
		return export;
	}

	private Map<String, Object> toMedicalIncidentExport(MedicalIncident incident) {
		Map<String, Object> export = new HashMap<>();
		export.put("occurredAt", incident.getOccurredAt());
		export.put("description", incident.getDescription());
		export.put("treatmentGiven", incident.getTreatmentGiven());
		return export;
	}

	private Map<String, Object> toDisciplineIncidentExport(DisciplineIncident incident) {
		Map<String, Object> export = new HashMap<>();
		export.put("incidentDate", incident.getIncidentDate());
		export.put("severity", incident.getSeverity());
		export.put("description", incident.getDescription());
		export.put("actionTaken", incident.getActionTaken());
		return export;
	}

	private Map<String, Object> toCounselingSessionExport(CounselingSession session) {
		Map<String, Object> export = new HashMap<>();
		export.put("sessionDate", session.getSessionDate());
		export.put("notes", session.getNotes());
		return export;
	}

	private Map<String, Object> toStudentExport(Student student) {
		Map<String, Object> export = new HashMap<>();
		export.put("publicId", student.getPublicId().toString());
		export.put("studentCode", student.getStudentCode());
		export.put("firstName", student.getFirstName());
		export.put("lastName", student.getLastName());
		export.put("email", student.getEmail());
		export.put("phone", student.getPhone());
		export.put("dateOfBirth", student.getDateOfBirth());
		return export;
	}

	private Map<String, Object> toGuardianExport(Guardian guardian) {
		Map<String, Object> export = new HashMap<>();
		export.put("publicId", guardian.getPublicId().toString());
		export.put("firstName", guardian.getFirstName());
		export.put("lastName", guardian.getLastName());
		export.put("email", guardian.getEmail());
		export.put("phone", guardian.getPhone());
		return export;
	}
}
