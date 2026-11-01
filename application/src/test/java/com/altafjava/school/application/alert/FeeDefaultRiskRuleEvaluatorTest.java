package com.altafjava.school.application.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.alert.AlertTrigger;
import com.altafjava.platform.domain.alert.model.AlertRule;
import com.altafjava.platform.domain.user.model.Role;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.notification.SchoolNotificationTypes;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
class FeeDefaultRiskRuleEvaluatorTest {

	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeePaymentService feePaymentService;
	@Mock
	private UserRepository userRepository;
	@Mock
	private RoleRepository roleRepository;

	private FeeDefaultRiskRuleEvaluator evaluator;

	private void newEvaluator() {
		evaluator = new FeeDefaultRiskRuleEvaluator(studentRepository, feePaymentService, userRepository,
				roleRepository);
	}

	private AlertRule ruleWithThreshold(Integer thresholdAmount) {
		AlertRule rule = AlertRule.create(FeeDefaultRiskRuleEvaluator.RULE_TYPE, "Fee default risk", true,
				thresholdAmount == null ? null : BigDecimal.valueOf(thresholdAmount),
				SchoolNotificationTypes.FEE_DEFAULT_RISK, null);
		rule.setTenantId(1L);
		return rule;
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-" + id, "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private User userWithId(long id) {
		User user = User.builder().email("staff" + id + "@school.test").passwordHash("hash").build();
		user.setId(id);
		return user;
	}

	private Role roleWithId(String name, long id) {
		Role role = Role.builder().name(name).build();
		role.setId(id);
		return role;
	}

	private void stubStaffUsers(User... financeUsers) {
		when(roleRepository.findByName("FINANCE")).thenReturn(Optional.of(roleWithId("FINANCE", 100L)));
		when(userRepository.findAllByRoleId(100L)).thenReturn(List.of(financeUsers));
		when(roleRepository.findByName("TENANT_ADMIN")).thenReturn(Optional.of(roleWithId("TENANT_ADMIN", 101L)));
		when(userRepository.findAllByRoleId(101L)).thenReturn(List.of());
	}

	@Test
	void evaluate_outstandingExceedsThreshold_notifiesEachStaffMember() {
		newEvaluator();
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(1L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(2000),
						BigDecimal.ZERO, BigDecimal.valueOf(1500), BigDecimal.ZERO, BigDecimal.ZERO, null))));
		stubStaffUsers(userWithId(90L), userWithId(91L));

		List<AlertTrigger> triggers = evaluator.evaluate(ruleWithThreshold(1000));

		assertEquals(2, triggers.size());
		assertTrue(triggers.stream().anyMatch(t -> t.recipientUserId() == 90L));
		assertTrue(triggers.stream().anyMatch(t -> t.recipientUserId() == 91L));
		assertEquals("1500", triggers.get(0).templateVariables().get("amount"));
	}

	@Test
	void evaluate_outstandingBelowThreshold_returnsNoTriggers() {
		newEvaluator();
		Student student = studentWithId(1L);
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of(student));
		when(feePaymentService.calculateBalancesForStudents(1L, List.of(student)))
				.thenReturn(Map.of(1L, List.of(new FeeBalance(10L, "Tuition", BigDecimal.valueOf(2000),
						BigDecimal.valueOf(1500), BigDecimal.valueOf(500), BigDecimal.ZERO, BigDecimal.ZERO, null))));

		assertTrue(evaluator.evaluate(ruleWithThreshold(1000)).isEmpty());
	}

	@Test
	void evaluate_noAtRiskStudents_neverQueriesStaffUsers() {
		newEvaluator();
		when(studentRepository.findAllByEnrollmentStatusAndTenantId(EnrollmentStatus.ACTIVE, 1L))
				.thenReturn(List.of());
		when(feePaymentService.calculateBalancesForStudents(1L, List.of())).thenReturn(Map.of());

		assertTrue(evaluator.evaluate(ruleWithThreshold(1000)).isEmpty());
	}
}
