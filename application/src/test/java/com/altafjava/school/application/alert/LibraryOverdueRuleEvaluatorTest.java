package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.school.application.scheduler.support.StudentNotificationRecipientResolver;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.library.repository.CirculationRepository;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class LibraryOverdueRuleEvaluatorTest {

	@Mock
	private CirculationRepository circulationRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private StudentNotificationRecipientResolver recipientResolver;

	private LibraryOverdueRuleEvaluator evaluator;

	private void newEvaluator() {
		evaluator = new LibraryOverdueRuleEvaluator(circulationRepository, studentRepository, recipientResolver);
	}

	private AlertRule ruleWithGraceDays(Integer graceDays) {
		AlertRule rule = AlertRule.create(LibraryOverdueRuleEvaluator.RULE_TYPE, "Library overdue", true,
				graceDays == null ? null : BigDecimal.valueOf(graceDays), SchoolNotificationTypes.BOOK_OVERDUE, null);
		rule.setTenantId(1L);
		return rule;
	}

	private Circulation overdueCirculation(long studentId, LocalDate dueDate) {
		Circulation circulation = Circulation.checkout(5L, studentId, dueDate.minusDays(14), dueDate);
		circulation.setId(1L);
		return circulation;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	@Test
	void evaluate_overdueBeyondGraceDays_returnsTrigger() {
		newEvaluator();
		Circulation circulation = overdueCirculation(1L, LocalDate.now().minusDays(5));
		Student student = studentWithId(1L);
		when(circulationRepository.findAllByTenantIdAndReturnedAtIsNull(1L)).thenReturn(List.of(circulation));
		when(studentRepository.findAllByIdInAndTenantId(List.of(1L), 1L)).thenReturn(List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(55L));

		List<AlertTrigger> triggers = evaluator.evaluate(ruleWithGraceDays(0));

		assertEquals(1, triggers.size());
		assertEquals(55L, triggers.get(0).recipientUserId());
	}

	@Test
	void evaluate_overdueButWithinGracePeriod_returnsNoTriggers() {
		newEvaluator();
		Circulation circulation = overdueCirculation(1L, LocalDate.now().minusDays(2));
		when(circulationRepository.findAllByTenantIdAndReturnedAtIsNull(1L)).thenReturn(List.of(circulation));

		assertTrue(evaluator.evaluate(ruleWithGraceDays(5)).isEmpty());
	}

	@Test
	void evaluate_notYetDue_returnsNoTriggers() {
		newEvaluator();
		Circulation circulation = overdueCirculation(1L, LocalDate.now().plusDays(3));
		when(circulationRepository.findAllByTenantIdAndReturnedAtIsNull(1L)).thenReturn(List.of(circulation));

		assertTrue(evaluator.evaluate(ruleWithGraceDays(0)).isEmpty());
	}

	@Test
	void evaluate_nullThreshold_usesZeroGraceDays() {
		newEvaluator();
		Circulation circulation = overdueCirculation(1L, LocalDate.now().minusDays(1));
		Student student = studentWithId(1L);
		when(circulationRepository.findAllByTenantIdAndReturnedAtIsNull(1L)).thenReturn(List.of(circulation));
		when(studentRepository.findAllByIdInAndTenantId(List.of(1L), 1L)).thenReturn(List.of(student));
		when(recipientResolver.resolve(1L, student)).thenReturn(Optional.of(55L));

		assertEquals(1, evaluator.evaluate(ruleWithGraceDays(null)).size());
	}
}
