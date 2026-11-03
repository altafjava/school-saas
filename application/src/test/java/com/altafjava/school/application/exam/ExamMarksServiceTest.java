package com.altafjava.school.application.exam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamSlot;
import com.altafjava.school.domain.exam.model.MarksStage;
import com.altafjava.school.domain.exam.repository.ExamRepository;
import com.altafjava.school.domain.grade.repository.GradeRepository;

@ExtendWith(MockitoExtension.class)
class ExamMarksServiceTest {

	private static final Long TENANT_ID = 1L;

	@Mock
	private ExamRepository examRepository;
	@Mock
	private GradeRepository gradeRepository;
	@InjectMocks
	private ExamMarksService service;

	private Exam exam;
	private String examId;

	@BeforeEach
	void completedExam() {
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		exam = Exam.create("Midterm", 5L, 10L, new ExamSlot(LocalDateTime.of(2026, 3, 2, 9, 0), 60, null),
				BigDecimal.TEN, null, 1L, BigDecimal.TEN);
		exam.setId(42L);
		exam.complete();
		UUID publicId = UUID.randomUUID();
		exam.setPublicId(publicId);
		examId = publicId.toString();
		when(examRepository.findByPublicIdAndTenantId(publicId, TENANT_ID)).thenReturn(Optional.of(exam));
	}

	@AfterEach
	void clearTenant() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void verify_bySomeoneWhoEnteredNoMark_verifiesTheMarks() {
		when(gradeRepository.existsByExamIdAndTenantId(42L, TENANT_ID)).thenReturn(true);
		when(gradeRepository.existsByExamIdAndGradedByAndTenantId(42L, "checker", TENANT_ID)).thenReturn(false);
		when(examRepository.save(any(Exam.class))).thenAnswer(call -> call.getArgument(0));

		assertEquals(MarksStage.VERIFIED, service.verify(examId, "checker").getMarksStage());
	}

	@Test
	void verify_bySomeoneWhoEnteredMarks_isRefused() {
		when(gradeRepository.existsByExamIdAndTenantId(42L, TENANT_ID)).thenReturn(true);
		when(gradeRepository.existsByExamIdAndGradedByAndTenantId(42L, "teacher", TENANT_ID)).thenReturn(true);

		assertThrows(AccessDeniedException.class, () -> service.verify(examId, "teacher"));
		verify(examRepository, never()).save(any());
	}

	@Test
	void verify_withNoMarksRecorded_throwsBusinessException() {
		when(gradeRepository.existsByExamIdAndTenantId(42L, TENANT_ID)).thenReturn(false);

		assertThrows(BusinessException.class, () -> service.verify(examId, "checker"));
	}

	@Test
	void lockThenUnlock_returnsTheMarksToEntry() {
		exam.verifyMarks("checker");
		when(examRepository.save(any(Exam.class))).thenAnswer(call -> call.getArgument(0));

		assertEquals(MarksStage.LOCKED, service.lock(examId, "registrar").getMarksStage());
		assertEquals(MarksStage.ENTERED, service.unlock(examId).getMarksStage());
	}
}
