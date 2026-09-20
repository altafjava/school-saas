package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.service.ActivityLogService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.counseling.model.CounselingReferral;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.counseling.repository.CounselingReferralRepository;
import com.altafjava.school.domain.counseling.repository.CounselingSessionRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class CounselingReferralServiceTest {

	private static final Long CURRENT_USER_ID = 55L;
	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private CounselingReferralRepository counselingReferralRepository;
	@Mock
	private CounselingSessionRepository counselingSessionRepository;
	@Mock
	private StudentRepository studentRepository;
	private final ActivityLogService activityLogService = new NoOpActivityLogService();

	private CounselingReferralService counselingReferralService;

	@BeforeEach
	void setUp() {
		counselingReferralService = new CounselingReferralService(counselingReferralRepository,
				counselingSessionRepository, studentRepository, activityLogService);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void authenticateAsUser(Long userId) {
		AuthenticatedUser principal = mock(AuthenticatedUser.class);
		when(principal.getId()).thenReturn(userId);
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void refer_resolvesReferringUserFromSecurityContext() {
		authenticateAsUser(CURRENT_USER_ID);
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(counselingReferralRepository.save(any(CounselingReferral.class))).thenAnswer(inv -> inv.getArgument(0));

		CounselingReferral referral = assertDoesNotThrow(() -> counselingReferralService
				.refer(STUDENT_PUBLIC_ID.toString(), "Struggling academically"));

		assertEquals(CURRENT_USER_ID, referral.getReferredByUserId());
		assertEquals(10L, referral.getStudentId());
	}

	@Test
	void refer_noAuthenticatedPrincipal_throwsAccessDeniedException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));

		assertThrows(AccessDeniedException.class,
				() -> counselingReferralService.refer(STUDENT_PUBLIC_ID.toString(), "Struggling academically"));
	}

	@Test
	void scheduleWithSession_sessionBelongsToSameStudent_succeeds() {
		UUID referralPublicId = UUID.randomUUID();
		UUID sessionPublicId = UUID.randomUUID();
		CounselingReferral referral = CounselingReferral.refer(10L, CURRENT_USER_ID, "Struggling academically");
		CounselingSession session = CounselingSession.schedule(10L, 20L, LocalDate.of(2026, 5, 1), "Notes", false);
		session.setId(42L);
		when(counselingReferralRepository.findByPublicIdAndTenantId(referralPublicId, 1L))
				.thenReturn(Optional.of(referral));
		when(counselingSessionRepository.findByPublicIdAndTenantId(sessionPublicId, 1L))
				.thenReturn(Optional.of(session));
		when(counselingReferralRepository.save(any(CounselingReferral.class))).thenAnswer(inv -> inv.getArgument(0));

		CounselingReferral scheduled = counselingReferralService.scheduleWithSession(referralPublicId.toString(),
				sessionPublicId.toString());

		assertEquals(42L, scheduled.getCounselingSessionId());
	}

	@Test
	void scheduleWithSession_sessionBelongsToDifferentStudent_throwsBusinessException() {
		UUID referralPublicId = UUID.randomUUID();
		UUID sessionPublicId = UUID.randomUUID();
		CounselingReferral referral = CounselingReferral.refer(10L, CURRENT_USER_ID, "Struggling academically");
		CounselingSession session = CounselingSession.schedule(999L, 20L, LocalDate.of(2026, 5, 1), "Notes", false);
		session.setId(42L);
		when(counselingReferralRepository.findByPublicIdAndTenantId(referralPublicId, 1L))
				.thenReturn(Optional.of(referral));
		when(counselingSessionRepository.findByPublicIdAndTenantId(sessionPublicId, 1L))
				.thenReturn(Optional.of(session));

		assertThrows(BusinessException.class,
				() -> counselingReferralService.scheduleWithSession(referralPublicId.toString(),
						sessionPublicId.toString()));
	}

	@Test
	void decline_fromPending_succeeds() {
		UUID referralPublicId = UUID.randomUUID();
		CounselingReferral referral = CounselingReferral.refer(10L, CURRENT_USER_ID, "Struggling academically");
		when(counselingReferralRepository.findByPublicIdAndTenantId(referralPublicId, 1L))
				.thenReturn(Optional.of(referral));
		when(counselingReferralRepository.save(any(CounselingReferral.class))).thenAnswer(inv -> inv.getArgument(0));

		CounselingReferral declined = counselingReferralService.decline(referralPublicId.toString());

		assertEquals(com.altafjava.school.domain.counseling.model.CounselingReferralStatus.DECLINED,
				declined.getStatus());
	}

	@Test
	void get_unknownPublicId_throwsResourceNotFoundException() {
		UUID publicId = UUID.randomUUID();
		when(counselingReferralRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> counselingReferralService.get(publicId.toString()));
	}
}
