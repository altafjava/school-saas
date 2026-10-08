package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.application.idcard.QrPayloadReader;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.attendance.model.Attendance;
import com.altafjava.school.domain.attendance.model.AttendanceStatus;
import com.altafjava.school.domain.attendance.repository.AttendanceRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class AttendanceScanServiceTest {

	private static final Long TENANT_ID = 1L;
	private static final String CODE = "a1B2c3D4";
	private static final LocalDate TODAY = LocalDate.now();

	@Mock
	private DocumentIssuanceService documentIssuanceService;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentClassroomLinkRepository studentClassroomLinkRepository;
	@Mock
	private ClassroomRepository classroomRepository;
	@Mock
	private AcademicYearRepository academicYearRepository;
	@Mock
	private AttendanceRepository attendanceRepository;
	@Mock
	private HolidayService holidayService;

	private AttendanceScanService service;
	private Student student;
	private Classroom classroom;

	@BeforeEach
	void setUp() {
		StudentPlacementResolver placementResolver = new StudentPlacementResolver(studentClassroomLinkRepository,
				classroomRepository, academicYearRepository);
		service = new AttendanceScanService(new QrPayloadReader(), documentIssuanceService, studentRepository,
				placementResolver, attendanceRepository, holidayService);
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		student = Student.create("STU-1", "Jane", "Doe", "jane@school.test", null);
		student.setId(10L);
		classroom = Classroom.builder().grade("5").section("A").build();
		classroom.setId(20L);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private DocumentIssuance studentCard() {
		return DocumentIssuance.create(SchoolDocumentTypes.STUDENT_ID_CARD, SchoolDocumentTypes.OWNER_STUDENT, 10L,
				"Student ID Card", "Jane Doe", null, null, CODE, "key", 1L);
	}

	private void stubEnrolledStudentWithCard(DocumentIssuance card) {
		when(documentIssuanceService.verify(TENANT_ID, CODE)).thenReturn(card);
		when(studentRepository.findByIdAndTenantId(10L, TENANT_ID)).thenReturn(Optional.of(student));
	}

	private void stubPlacement() {
		StudentClassroomLink link = StudentClassroomLink.create(10L, 20L, 30L, TODAY.minusMonths(2));
		when(studentClassroomLinkRepository.findByStudentId(TENANT_ID, 10L)).thenReturn(List.of(link));
		when(academicYearRepository.findByCurrentTrueAndTenantId(TENANT_ID)).thenReturn(Optional.empty());
		when(classroomRepository.findByIdAndTenantId(20L, TENANT_ID)).thenReturn(Optional.of(classroom));
		when(academicYearRepository.findByIdAndTenantId(30L, TENANT_ID)).thenReturn(Optional.empty());
	}

	@Test
	void scan_aGenuineStudentCard_marksThemPresentToday() {
		stubEnrolledStudentWithCard(studentCard());
		stubPlacement();
		when(holidayService.datesInRange(TENANT_ID, TODAY, TODAY)).thenReturn(Set.of());
		when(attendanceRepository.findByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(10L, 20L, TODAY,
				TENANT_ID)).thenReturn(Optional.empty());
		when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

		AttendanceScanResult result = service.scan("https://school.example/verify/" + CODE, "gate-guard");

		assertFalse(result.alreadyMarked());
		assertEquals(AttendanceStatus.PRESENT, result.attendance().getStatus());
		assertEquals(TODAY, result.attendance().getAttendanceDate());
		assertEquals("qr-scan:gate-guard", result.attendance().getMarkedBy());
		assertEquals(20L, result.attendance().getClassroomId());
	}

	@Test
	void scan_aSecondTimeToday_returnsTheExistingRecordWithoutWritingAgain() {
		stubEnrolledStudentWithCard(studentCard());
		stubPlacement();
		when(holidayService.datesInRange(TENANT_ID, TODAY, TODAY)).thenReturn(Set.of());
		Attendance existing = Attendance.create(10L, 20L, TODAY, AttendanceStatus.PRESENT, "teacher");
		when(attendanceRepository.findByStudentIdAndClassroomIdAndAttendanceDateAndTenantId(10L, 20L, TODAY,
				TENANT_ID)).thenReturn(Optional.of(existing));

		AttendanceScanResult result = service.scan(CODE, "gate-guard");

		assertTrue(result.alreadyMarked());
		assertEquals(existing, result.attendance());
		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void scan_aRevokedCard_isRefused() {
		DocumentIssuance card = studentCard();
		card.revoke("Lost");
		when(documentIssuanceService.verify(TENANT_ID, CODE)).thenReturn(card);

		assertThrows(BusinessException.class, () -> service.scan(CODE, "gate-guard"));
		verify(attendanceRepository, never()).save(any());
	}

	@Test
	void scan_aStaffCard_isRefused() {
		DocumentIssuance staffCard = DocumentIssuance.create(SchoolDocumentTypes.STAFF_ID_CARD,
				SchoolDocumentTypes.OWNER_EMPLOYEE, 10L, "Staff ID Card", "Sam Lee", null, null, CODE, "key", 1L);
		when(documentIssuanceService.verify(TENANT_ID, CODE)).thenReturn(staffCard);

		assertThrows(BusinessException.class, () -> service.scan(CODE, "gate-guard"));
	}

	@Test
	void scan_aCertificateOrOtherDocument_isRefused() {
		DocumentIssuance certificate = DocumentIssuance.create("CERTIFICATE.BONAFIDE",
				SchoolDocumentTypes.OWNER_STUDENT, 10L, "Certificate", "Jane Doe", null, null, CODE, "key", 1L);
		when(documentIssuanceService.verify(TENANT_ID, CODE)).thenReturn(certificate);

		assertThrows(BusinessException.class, () -> service.scan(CODE, "gate-guard"));
	}

	@Test
	void scan_aCardForAStudentWhoHasLeft_isRefused() {
		student.withdraw();
		stubEnrolledStudentWithCard(studentCard());

		assertThrows(BusinessException.class, () -> service.scan(CODE, "gate-guard"));
	}

	@Test
	void scan_onASchoolHoliday_isRefused() {
		stubEnrolledStudentWithCard(studentCard());
		when(holidayService.datesInRange(TENANT_ID, TODAY, TODAY)).thenReturn(Set.of(TODAY));

		assertThrows(BusinessException.class, () -> service.scan(CODE, "gate-guard"));
	}

	@Test
	void scan_aStudentWithNoClassroom_isRefused() {
		stubEnrolledStudentWithCard(studentCard());
		when(holidayService.datesInRange(TENANT_ID, TODAY, TODAY)).thenReturn(Set.of());
		when(studentClassroomLinkRepository.findByStudentId(TENANT_ID, 10L)).thenReturn(List.of());

		assertThrows(BusinessException.class, () -> service.scan(CODE, "gate-guard"));
	}

	@Test
	void scan_anUnknownCode_propagatesNotFound() {
		when(documentIssuanceService.verify(TENANT_ID, CODE))
				.thenThrow(new ResourceNotFoundException("No document found for this verification code"));

		assertThrows(ResourceNotFoundException.class, () -> service.scan(CODE, "gate-guard"));
	}

	@Test
	void scan_garbage_isRefusedBeforeAnyLookup() {
		assertThrows(BusinessException.class, () -> service.scan("not a code!", "gate-guard"));

		verify(documentIssuanceService, never()).verify(any(), any());
	}
}
