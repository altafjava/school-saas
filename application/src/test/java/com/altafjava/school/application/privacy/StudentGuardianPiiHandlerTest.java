package com.altafjava.school.application.privacy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.discipline.model.DisciplineIncident;
import com.altafjava.school.domain.discipline.model.IncidentSeverity;
import com.altafjava.school.domain.discipline.repository.DisciplineIncidentRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.health.model.HealthRecord;
import com.altafjava.school.domain.health.model.MedicalIncident;
import com.altafjava.school.domain.health.repository.HealthRecordRepository;
import com.altafjava.school.domain.health.repository.MedicalIncidentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class StudentGuardianPiiHandlerTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private GuardianRepository guardianRepository;
	@Mock
	private HealthRecordRepository healthRecordRepository;
	@Mock
	private MedicalIncidentRepository medicalIncidentRepository;
	@Mock
	private DisciplineIncidentRepository disciplineIncidentRepository;
	@Mock
	private CounselingSessionRepository counselingSessionRepository;

	private StudentGuardianPiiHandler piiHandler;

	private void newHandler() {
		piiHandler = new StudentGuardianPiiHandler(studentRepository, guardianRepository, healthRecordRepository,
				medicalIncidentRepository, disciplineIncidentRepository, counselingSessionRepository);
	}

	private Student studentWithId(Long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", LocalDate.of(2010, 1, 1));
		student.setId(id);
		return student;
	}

	@Test
	void erase_withLinkedStudentAndGuardian_erasesAndSoftDeletesBoth() {
		newHandler();
		Student student = studentWithId(100L);
		Guardian guardian = Guardian.create("Jane", "Doe", "jane@school.test", "+14155552671", 42L);
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(guardian));
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
		when(guardianRepository.save(any(Guardian.class))).thenAnswer(inv -> inv.getArgument(0));

		piiHandler.erase(9L, 42L);

		assertEquals("[erased]", student.getFirstName());
		assertTrue(student.isDeleted());
		assertEquals("[erased]", guardian.getFirstName());
		assertTrue(guardian.isDeleted());
		verify(studentRepository).save(student);
		verify(guardianRepository).save(guardian);
	}

	@Test
	void erase_withNeitherLinked_doesNothing() {
		newHandler();
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());

		piiHandler.erase(9L, 42L);

		verify(studentRepository, never()).save(any());
		verify(guardianRepository, never()).save(any());
		verify(healthRecordRepository, never()).save(any());
	}

	@Test
	void erase_withLinkedStudent_alsoErasesHealthDisciplineAndCounselingRecords() {
		newHandler();
		Student student = studentWithId(100L);
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());
		when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));

		HealthRecord healthRecord = HealthRecord.create(100L, "O+", "Peanuts", "Asthma", "MMR");
		when(healthRecordRepository.findByStudentIdAndTenantId(100L, 9L)).thenReturn(Optional.of(healthRecord));
		when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		MedicalIncident medicalIncident = MedicalIncident.record(100L, LocalDateTime.now(), "Fell on playground",
				"Ice pack", 5L);
		when(medicalIncidentRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of(medicalIncident));
		when(medicalIncidentRepository.save(any(MedicalIncident.class))).thenAnswer(inv -> inv.getArgument(0));

		DisciplineIncident disciplineIncident = DisciplineIncident.report(100L, 6L, LocalDate.now(),
				IncidentSeverity.MINOR, "Talking in class");
		when(disciplineIncidentRepository.findAllByStudentIdAndTenantId(100L, 9L))
				.thenReturn(List.of(disciplineIncident));
		when(disciplineIncidentRepository.save(any(DisciplineIncident.class))).thenAnswer(inv -> inv.getArgument(0));

		CounselingSession counselingSession = CounselingSession.schedule(100L, 7L, LocalDate.now(),
				"Discussed anxiety", true);
		when(counselingSessionRepository.findAllByStudentIdAndTenantId(100L, 9L))
				.thenReturn(List.of(counselingSession));
		when(counselingSessionRepository.save(any(CounselingSession.class))).thenAnswer(inv -> inv.getArgument(0));

		piiHandler.erase(9L, 42L);

		assertNull(healthRecord.getBloodGroup());
		assertNull(healthRecord.getAllergies());
		assertEquals("[erased]", medicalIncident.getDescription());
		assertNull(medicalIncident.getTreatmentGiven());
		assertEquals("[erased]", disciplineIncident.getDescription());
		assertNull(disciplineIncident.getActionTaken());
		assertNull(counselingSession.getNotes());
		verify(healthRecordRepository).save(healthRecord);
		verify(medicalIncidentRepository).save(medicalIncident);
		verify(disciplineIncidentRepository).save(disciplineIncident);
		verify(counselingSessionRepository).save(counselingSession);
	}

	@Test
	void export_withLinkedStudentOnly_returnsOnlyStudentKey() {
		newHandler();
		Student student = studentWithId(100L);
		student.setPublicId(java.util.UUID.randomUUID());
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());
		when(healthRecordRepository.findByStudentIdAndTenantId(100L, 9L)).thenReturn(Optional.empty());
		when(medicalIncidentRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of());
		when(disciplineIncidentRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of());
		when(counselingSessionRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of());

		Map<String, Object> result = piiHandler.export(9L, 42L);

		assertTrue(result.containsKey("student"));
		assertFalse(result.containsKey("guardian"));
		@SuppressWarnings("unchecked")
		Map<String, Object> studentExport = (Map<String, Object>) result.get("student");
		assertEquals("STU-1", studentExport.get("studentCode"));
		assertEquals("Alice", studentExport.get("firstName"));
	}

	@Test
	void export_withLinkedStudent_includesHealthAndBehavioralRecords() {
		newHandler();
		Student student = studentWithId(100L);
		student.setPublicId(java.util.UUID.randomUUID());
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.of(student));
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());

		HealthRecord healthRecord = HealthRecord.create(100L, "O+", "Peanuts", "Asthma", "MMR");
		when(healthRecordRepository.findByStudentIdAndTenantId(100L, 9L)).thenReturn(Optional.of(healthRecord));
		when(medicalIncidentRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of());
		when(disciplineIncidentRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of());
		when(counselingSessionRepository.findAllByStudentIdAndTenantId(100L, 9L)).thenReturn(List.of());

		Map<String, Object> result = piiHandler.export(9L, 42L);

		@SuppressWarnings("unchecked")
		Map<String, Object> healthExport = (Map<String, Object>) result.get("healthRecord");
		assertEquals("O+", healthExport.get("bloodGroup"));
		assertEquals("Peanuts", healthExport.get("allergies"));
		assertTrue(((List<?>) result.get("medicalIncidents")).isEmpty());
		assertTrue(((List<?>) result.get("disciplineIncidents")).isEmpty());
		assertTrue(((List<?>) result.get("counselingSessions")).isEmpty());
	}

	@Test
	void export_withNeitherLinked_returnsEmptyMap() {
		newHandler();
		when(studentRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());
		when(guardianRepository.findByUserIdAndTenantId(42L, 9L)).thenReturn(Optional.empty());

		Map<String, Object> result = piiHandler.export(9L, 42L);

		assertTrue(result.isEmpty());
	}
}
