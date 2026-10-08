package com.altafjava.school.application.lifecycle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.admission.model.AdmissionStatus;
import com.altafjava.school.domain.lifecycle.model.LifecycleStage;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;
import com.altafjava.school.domain.lifecycle.repository.LifecycleTransitionRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;

@ExtendWith(MockitoExtension.class)
class LifecycleRecorderTest {

	@Mock
	private LifecycleTransitionRepository repository;

	private LifecycleRecorder recorder;

	@BeforeEach
	void setUp() {
		recorder = new LifecycleRecorder(repository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clear() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private LifecycleTransition saved() {
		ArgumentCaptor<LifecycleTransition> captor = ArgumentCaptor.forClass(LifecycleTransition.class);
		verify(repository).save(captor.capture());
		return captor.getValue();
	}

	@Test
	void student_recordsTheStageChangeForTheCurrentTenantAndUser() {
		AuthenticatedUser user = mock(AuthenticatedUser.class);
		when(user.getId()).thenReturn(42L);
		SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));

		recorder.student(7L, EnrollmentStatus.ACTIVE, EnrollmentStatus.WITHDRAWN,
				new LifecycleChange("Moved abroad", LocalDate.now().minusDays(2)));

		LifecycleTransition transition = saved();
		assertEquals(LifecycleStage.ENROLLED, transition.getFromStage());
		assertEquals(LifecycleStage.WITHDRAWN, transition.getToStage());
		assertEquals(7L, transition.getStudentId());
		assertEquals(42L, transition.getRecordedByUserId());
		assertEquals(1L, transition.getTenantId());
		assertEquals(LocalDate.now().minusDays(2), transition.getEffectiveOn());
	}

	@Test
	void admission_withoutAnAuthenticatedUser_recordsNullActor() {
		recorder.admission(3L, null, AdmissionStatus.SUBMITTED, LifecycleChange.NONE);

		LifecycleTransition transition = saved();
		assertNull(transition.getFromStage());
		assertNull(transition.getRecordedByUserId());
		assertEquals(3L, transition.getAdmissionId());
	}

	@Test
	void enrolledFromAdmission_belongsToBothTheAdmissionAndTheStudent() {
		recorder.enrolledFromAdmission(3L, 8L, LifecycleChange.NONE);

		LifecycleTransition transition = saved();
		assertEquals(3L, transition.getAdmissionId());
		assertEquals(8L, transition.getStudentId());
		assertEquals(LifecycleStage.APPROVED, transition.getFromStage());
		assertEquals(LifecycleStage.ENROLLED, transition.getToStage());
	}

	@Test
	void change_cannotTakeEffectInTheFuture() {
		assertThrows(BusinessException.class, () -> new LifecycleChange("x", LocalDate.now().plusDays(1)));
	}
}
