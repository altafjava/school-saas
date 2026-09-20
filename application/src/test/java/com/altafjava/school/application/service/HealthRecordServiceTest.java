package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.health.model.HealthRecord;
import com.altafjava.school.domain.health.repository.HealthRecordRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private HealthRecordRepository healthRecordRepository;
	@Mock
	private StudentRepository studentRepository;
	private final ActivityLogService activityLogService = new NoOpActivityLogService();

	private HealthRecordService healthRecordService;

	@BeforeEach
	void setUp() {
		healthRecordService = new HealthRecordService(healthRecordRepository, studentRepository, activityLogService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void upsert_noExistingRecord_createsNewOne() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(healthRecordRepository.findByStudentIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());
		when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		HealthRecord record = healthRecordService.upsert(STUDENT_PUBLIC_ID.toString(), "O+", "Peanuts", "Asthma",
				"MMR");

		assertEquals(10L, record.getStudentId());
		assertEquals("O+", record.getBloodGroup());
	}

	@Test
	void upsert_existingRecord_updatesInPlace() {
		HealthRecord existing = HealthRecord.create(10L, "O+", "Peanuts", "Asthma", "MMR");
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(healthRecordRepository.findByStudentIdAndTenantId(10L, 1L)).thenReturn(Optional.of(existing));
		when(healthRecordRepository.save(any(HealthRecord.class))).thenAnswer(inv -> inv.getArgument(0));

		HealthRecord updated = healthRecordService.upsert(STUDENT_PUBLIC_ID.toString(), "A-", "Pollen", "None",
				"MMR, HepB");

		assertEquals("A-", updated.getBloodGroup());
		assertEquals("Pollen", updated.getAllergies());
	}

	@Test
	void getByStudent_noRecord_throwsResourceNotFoundException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(healthRecordRepository.findByStudentIdAndTenantId(10L, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> healthRecordService.getByStudent(STUDENT_PUBLIC_ID.toString()));
	}
}
