package com.altafjava.school.application.listener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.alert.AlertRuleService;
import com.altafjava.platform.application.event.events.TenantCreatedEvent;
import com.altafjava.platform.application.service.approval.ApprovalWorkflowDefinitionService;
import com.altafjava.platform.application.service.privacy.DataRetentionPolicyService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantContextSnapshot;
import com.altafjava.platform.domain.approval.repository.ApprovalWorkflowDefinitionRepository;
import com.altafjava.platform.domain.notification.model.NotificationChannel;
import com.altafjava.platform.domain.notification.model.NotificationTemplate;
import com.altafjava.platform.domain.notification.model.NotificationType;
import com.altafjava.platform.domain.notification.repository.NotificationTemplateRepository;
import com.altafjava.platform.domain.privacy.model.DeletionPolicy;
import com.altafjava.platform.domain.report.model.ReportDefinition;
import com.altafjava.platform.domain.report.model.ReportOutputFormat;
import com.altafjava.platform.domain.report.model.ReportType;
import com.altafjava.platform.domain.report.repository.ReportDefinitionRepository;
import com.altafjava.school.application.alert.AttendanceNotMarkedRuleEvaluator;
import com.altafjava.school.application.alert.ExamScheduleReminderRuleEvaluator;
import com.altafjava.school.application.alert.FeeDefaultRiskRuleEvaluator;
import com.altafjava.school.application.alert.FeePaymentReminderRuleEvaluator;
import com.altafjava.school.application.alert.LibraryOverdueRuleEvaluator;
import com.altafjava.school.application.alert.LowAttendanceRuleEvaluator;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.curriculum.model.GradingScaleThreshold;
import com.altafjava.school.domain.curriculum.repository.GradingScaleRepository;
import com.altafjava.school.domain.curriculum.repository.GradingScaleThresholdRepository;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.exam.repository.ExamTypeDefinitionRepository;
import com.altafjava.school.domain.payroll.model.PayComponentDefinition;
import com.altafjava.school.domain.payroll.model.PayComponentType;
import com.altafjava.school.domain.payroll.repository.PayComponentDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Reacts to a new tenant being provisioned by the platform. Seeds school-specific default data
 * (academic year, notification templates). Role seeding is not needed here — TEACHER/STUDENT/PARENT
 * are global system-role templates seeded once via Liquibase, see {@code Role.java}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SchoolTenantProvisioningListener {

	private static final Set<NotificationChannel> DEFAULT_CHANNELS = Set.of(NotificationChannel.EMAIL,
			NotificationChannel.IN_APP);

	private static final List<TemplateSeed> NOTIFICATION_TEMPLATE_SEEDS = List.of(
			new TemplateSeed(NotificationType.FEE_DUE, "Fee Payment Reminder",
					"Dear parent/guardian,\n\n{{studentName}} has an outstanding fee balance of {{amount}}. "
							+ "Please make payment at your earliest convenience to avoid any disruption to "
							+ "enrollment.\n\nThank you.",
					List.of("studentName", "amount")),
			new TemplateSeed(NotificationType.EXAM_SCHEDULED, "Upcoming Exam: {{examTitle}}",
					"Dear parent/guardian,\n\n{{studentName}} has a {{subjectName}} exam ({{examTitle}}) "
							+ "scheduled for {{scheduledAt}}. Please ensure they are well prepared.\n\nThank you.",
					List.of("studentName", "examTitle", "subjectName", "scheduledAt")),
			new TemplateSeed(NotificationType.LOW_ATTENDANCE_ALERT, "Low Attendance Alert",
					"Dear parent/guardian,\n\n{{studentName}}'s attendance over the last {{windowDays}} days is "
							+ "{{percentage}}%, which is below the school's minimum threshold. Please contact the "
							+ "school office if you have any concerns.\n\nThank you.",
					List.of("studentName", "percentage", "windowDays")),
			new TemplateSeed(NotificationType.ASSIGNMENT_POSTED, "New Assignment: {{assignmentTitle}}",
					"Dear {{studentName}},\n\nA new assignment \"{{assignmentTitle}}\" has been posted and is "
							+ "due on {{dueDate}}. Please log in to view details and submit your work on "
							+ "time.\n\nThank you.",
					List.of("studentName", "assignmentTitle", "dueDate")),
			new TemplateSeed(NotificationType.SUBMISSION_GRADED, "Submission Graded: {{assignmentTitle}}",
					"Dear student,\n\nYour submission for \"{{assignmentTitle}}\" has been graded. You received "
							+ "{{marksObtained}} marks.\n\nFeedback: {{feedback}}\n\nThank you.",
					List.of("assignmentTitle", "marksObtained", "feedback")),
			new TemplateSeed(NotificationType.LEAVE_REQUESTED, "Leave Request: {{teacherName}}",
					"{{teacherName}} requested {{daysRequested}} day(s) of {{leaveTypeName}} leave, from "
							+ "{{startDate}} to {{endDate}}. Please review and approve or reject this request.",
					List.of("teacherName", "leaveTypeName", "startDate", "endDate", "daysRequested")),
			new TemplateSeed(NotificationType.LEAVE_APPROVED, "Leave Request Approved",
					"Dear teacher,\n\nYour leave request from {{startDate}} to {{endDate}} "
							+ "({{daysRequested}} day(s)) has been approved.\n\nThank you.",
					List.of("startDate", "endDate", "daysRequested")),
			new TemplateSeed(NotificationType.LEAVE_REJECTED, "Leave Request Rejected",
					"Dear teacher,\n\nYour leave request from {{startDate}} to {{endDate}} "
							+ "({{daysRequested}} day(s)) has been rejected.\n\nReason: {{rejectionReason}}\n\n"
							+ "Please contact the school office if you have any questions.",
					List.of("startDate", "endDate", "daysRequested", "rejectionReason")),
			new TemplateSeed(NotificationType.DISCIPLINE_INCIDENT_RECORDED, "Discipline Incident Reported",
					"Dear parent/guardian,\n\nA {{severity}} discipline incident was recorded for {{studentName}} "
							+ "on {{incidentDate}}.\n\nDetails: {{description}}\n\nPlease contact the school office "
							+ "if you have any questions.",
					List.of("studentName", "severity", "incidentDate", "description")),
			new TemplateSeed(NotificationType.BOOK_OVERDUE, "Overdue Library Book",
					"Dear parent/guardian,\n\n{{studentName}} has a library book overdue since {{dueDate}}. "
							+ "Please return it at your earliest convenience to avoid further fines.\n\nThank you.",
					List.of("studentName", "dueDate")),
			new TemplateSeed(NotificationType.EVENT_REGISTRATION_CONFIRMED, "Event Registration Confirmed",
					"Dear {{studentName}},\n\nYour registration for \"{{eventTitle}}\" on {{eventDate}} is "
							+ "confirmed.\n\nThank you.",
					List.of("studentName", "eventTitle", "eventDate")));

	private static final List<DashboardReportSeed> DASHBOARD_REPORT_SEEDS = List.of(
			new DashboardReportSeed("Principal Dashboard", "principalDashboardDataProvider"),
			new DashboardReportSeed("Finance Dashboard", "financeDashboardDataProvider"),
			new DashboardReportSeed("HR Dashboard", "hrDashboardDataProvider"),
			new DashboardReportSeed("Academic Dashboard", "academicDashboardDataProvider"),
			new DashboardReportSeed("Principal Dashboard Trend", "attendanceTrendDataProvider"),
			new DashboardReportSeed("Academic Dashboard Trend", "attendanceTrendDataProvider"),
			new DashboardReportSeed("Finance Dashboard Trend", "feeCollectionTrendDataProvider"),
			new DashboardReportSeed("HR Dashboard Trend", "leaveUtilizationTrendDataProvider"));

	// Defaults exactly preserve pre-Phase-4 hardcoded job behavior; a tenant admin can tune or
	// disable any of these via AlertRuleController without a deploy.
	private static final List<AlertRuleSeed> ALERT_RULE_SEEDS = List.of(
			new AlertRuleSeed(LowAttendanceRuleEvaluator.RULE_TYPE, "Low attendance alert",
					BigDecimal.valueOf(75), NotificationType.LOW_ATTENDANCE_ALERT),
			new AlertRuleSeed(FeePaymentReminderRuleEvaluator.RULE_TYPE, "Fee payment reminder",
					null, NotificationType.FEE_DUE),
			new AlertRuleSeed(FeeDefaultRiskRuleEvaluator.RULE_TYPE, "Fee default risk",
					BigDecimal.valueOf(1000), NotificationType.FEE_DEFAULT_RISK),
			new AlertRuleSeed(ExamScheduleReminderRuleEvaluator.RULE_TYPE, "Exam schedule reminder",
					BigDecimal.valueOf(2), NotificationType.EXAM_SCHEDULED),
			new AlertRuleSeed(AttendanceNotMarkedRuleEvaluator.RULE_TYPE, "Daily attendance not marked",
					null, NotificationType.ANNOUNCEMENT),
			new AlertRuleSeed(LibraryOverdueRuleEvaluator.RULE_TYPE, "Library book overdue",
					BigDecimal.ZERO, NotificationType.BOOK_OVERDUE));

	// Default starting catalog — freely renamed/deactivated afterward via
	// PayComponentDefinitionController, no code change needed for non-Indian tenants.
	private static final List<PayComponentSeed> PAY_COMPONENT_SEEDS = List.of(
			new PayComponentSeed("BASIC", "Basic Pay", PayComponentType.EARNING, 1),
			new PayComponentSeed("HRA", "House Rent Allowance", PayComponentType.EARNING, 2),
			new PayComponentSeed("TRANSPORT", "Transport Allowance", PayComponentType.EARNING, 3),
			new PayComponentSeed("OTHER_ALLOWANCE", "Other Allowances", PayComponentType.EARNING, 4),
			new PayComponentSeed("OTHER_DEDUCTION", "Other Deductions", PayComponentType.DEDUCTION, 5));

	// Default starting catalog — freely renamed/deactivated afterward via
	// ExamTypeDefinitionController for boards with a different scheme (e.g. Formative/Summative).
	private static final List<ExamTypeSeed> EXAM_TYPE_SEEDS = List.of(
			new ExamTypeSeed("UNIT_TEST", "Unit Test", 1),
			new ExamTypeSeed("MIDTERM", "Midterm", 2),
			new ExamTypeSeed("FINAL", "Final", 3),
			new ExamTypeSeed("QUIZ", "Quiz", 4));

	private final AcademicYearRepository academicYearRepository;
	private final NotificationTemplateRepository notificationTemplateRepository;
	private final GradingScaleRepository gradingScaleRepository;
	private final GradingScaleThresholdRepository gradingScaleThresholdRepository;
	private final ReportDefinitionRepository reportDefinitionRepository;
	private final AlertRuleService alertRuleService;
	private final ApprovalWorkflowDefinitionService approvalWorkflowDefinitionService;
	private final ApprovalWorkflowDefinitionRepository approvalWorkflowDefinitionRepository;
	private final PayComponentDefinitionRepository payComponentDefinitionRepository;
	private final ExamTypeDefinitionRepository examTypeDefinitionRepository;
	private final DataRetentionPolicyService dataRetentionPolicyService;
	private final ObjectMapper objectMapper;

	private static final String STUDENT_ENTITY_TYPE = "STUDENT";
	// 7 years post-withdrawal/graduation — an editable starting point (PUT
	// /api/v1/data-retention-policies/{id}), not a legal opinion; confirm against the tenant's
	// actual jurisdiction (FERPA/COPPA/GDPR-K/DPDP) before relying on it.
	private static final int STUDENT_RETENTION_PERIOD_DAYS = 2555;

	@Async("platformTaskExecutor")
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@EventListener
	public void onTenantCreated(TenantCreatedEvent event) {
		log.info("action=school-tenant-provisioning tenantId={} tenantType={}", event.tenantId(), event.tenantType());
		TenantContextSnapshot snapshot = new TenantContextSnapshot(
				event.tenantId(), null, null, event.tenantType(), null);
		TenantContext.runAsTenant(snapshot, () -> {
			seedDefaultAcademicYear(event.tenantId());
			seedDefaultNotificationTemplates(event.tenantId());
			seedDefaultGradingScale(event.tenantId());
			seedDashboardReportDefinitions(event.tenantId());
			seedDefaultAlertRules(event.tenantId());
			seedDefaultApprovalWorkflows();
			seedDefaultPayComponents(event.tenantId());
			seedDefaultExamTypes(event.tenantId());
			seedDefaultDataRetentionPolicy(event.tenantId());
		});
		log.info("action=school-tenant-provisioning-complete tenantId={}", event.tenantId());
	}

	private void seedDefaultAcademicYear(Long tenantId) {
		LocalDate now = LocalDate.now();
		int year = now.getYear();
		String name = year + "-" + (year + 1);

		// Runs asynchronously after the tenant is created: an admin may have set up a current year by now, and a
		// second current year would break the one-current-year rule.
		if (academicYearRepository.existsByTenantId(tenantId)) {
			log.info("action=seed-academic-year-skipped tenantId={} name={} reason=tenant-has-a-year", tenantId, name);
			return;
		}

		AcademicYear academicYear = AcademicYear.create(
				name,
				LocalDate.of(year, 4, 1),
				LocalDate.of(year + 1, 3, 31),
				true);
		academicYearRepository.save(academicYear);
		log.info("action=seed-academic-year-created tenantId={} name={}", tenantId, name);
	}

	private static final List<ThresholdSeed> DEFAULT_GRADING_SCALE_THRESHOLDS = List.of(
			new ThresholdSeed("A", new BigDecimal("90"), new BigDecimal("4.0")),
			new ThresholdSeed("B", new BigDecimal("80"), new BigDecimal("3.0")),
			new ThresholdSeed("C", new BigDecimal("70"), new BigDecimal("2.0")),
			new ThresholdSeed("D", new BigDecimal("60"), new BigDecimal("1.0")),
			new ThresholdSeed("F", BigDecimal.ZERO, BigDecimal.ZERO));

	private void seedDefaultGradingScale(Long tenantId) {
		if (gradingScaleRepository.findByIsDefaultTrueAndTenantId(tenantId).isPresent()) {
			log.info("action=seed-grading-scale-skipped tenantId={} reason=already-exists", tenantId);
			return;
		}
		GradingScale scale = gradingScaleRepository.save(GradingScale.create("Default", true));
		DEFAULT_GRADING_SCALE_THRESHOLDS.forEach(seed -> gradingScaleThresholdRepository.save(
				GradingScaleThreshold.create(scale.getId(), seed.letter(), seed.minPercentage(), seed.points())));
		log.info("action=seed-grading-scale-created tenantId={}", tenantId);
	}

	private void seedDashboardReportDefinitions(Long tenantId) {
		DASHBOARD_REPORT_SEEDS.forEach(seed -> seedDashboardReportDefinition(tenantId, seed));
	}

	private void seedDashboardReportDefinition(Long tenantId, DashboardReportSeed seed) {
		if (reportDefinitionRepository.existsByNameAndTenantId(seed.name(), tenantId)) {
			log.info("action=seed-dashboard-report-skipped tenantId={} name={} reason=already-exists", tenantId,
					seed.name());
			return;
		}
		ReportDefinition definition = ReportDefinition.builder()
				.tenantId(tenantId)
				.name(seed.name())
				.description(seed.name() + " — role-scoped aggregate, exportable as CSV/Excel/PDF/JSON")
				.type(ReportType.SERVICE_CALL)
				.queryTemplate(seed.providerBeanName())
				.outputFormat(ReportOutputFormat.JSON)
				.active(true)
				.build();
		reportDefinitionRepository.save(definition);
		log.info("action=seed-dashboard-report-created tenantId={} name={}", tenantId, seed.name());
	}

	private void seedDefaultNotificationTemplates(Long tenantId) {
		NOTIFICATION_TEMPLATE_SEEDS.forEach(seed -> seedNotificationTemplate(tenantId, seed));
	}

	private void seedNotificationTemplate(Long tenantId, TemplateSeed seed) {
		if (notificationTemplateRepository.findByTenantIdAndType(tenantId, seed.type()).isPresent()) {
			log.info("action=seed-notification-template-skipped tenantId={} type={} reason=already-exists", tenantId,
					seed.type());
			return;
		}

		NotificationTemplate template = NotificationTemplate.builder()
				.tenantId(tenantId)
				.type(seed.type())
				.subjectTemplate(seed.subject())
				.bodyTemplate(seed.body())
				.channels(DEFAULT_CHANNELS)
				.variables(serializeVariableNames(seed.variableNames()))
				.active(true)
				.build();
		notificationTemplateRepository.save(template);
		log.info("action=seed-notification-template-created tenantId={} type={}", tenantId, seed.type());
	}

	// Preserves today's admission-decision behavior out of the box (any PRINCIPAL decides, one
	// stage) while making it genuinely tenant-editable afterward — e.g. into a "class teacher then
	// principal" chain — purely through the approval-workflow stage API, no code change. See
	// AdmissionService#requestApproval/AdmissionApprovalHandler.
	private void seedDefaultApprovalWorkflows() {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (approvalWorkflowDefinitionRepository.findByTenantIdAndOperationCode(tenantId, "ADMISSION_DECISION")
				.isPresent()) {
			log.info("action=seed-approval-workflow-skipped tenantId={} operationCode=ADMISSION_DECISION "
					+ "reason=already-exists", tenantId);
			return;
		}
		approvalWorkflowDefinitionService.create(
				"ADMISSION_DECISION",
				"Admission Decision",
				"Approval required to enroll a student from an admission application",
				48,
				true,
				true,
				true,
				List.of(new ApprovalWorkflowDefinitionService.StageSpec("Principal approval",
						List.of(SchoolRoles.PRINCIPAL), 1, 1, false)));
		log.info("action=seed-approval-workflow-created tenantId={} operationCode=ADMISSION_DECISION", tenantId);
	}

	private void seedDefaultAlertRules(Long tenantId) {
		ALERT_RULE_SEEDS.forEach(seed -> seedAlertRule(tenantId, seed));
	}

	private void seedAlertRule(Long tenantId, AlertRuleSeed seed) {
		if (alertRuleService.exists(tenantId, seed.ruleType())) {
			log.info("action=seed-alert-rule-skipped tenantId={} ruleType={} reason=already-exists", tenantId,
					seed.ruleType());
			return;
		}
		alertRuleService.create(tenantId, seed.ruleType(), seed.name(), true, seed.thresholdValue(),
				seed.notificationType(), null);
		log.info("action=seed-alert-rule-created tenantId={} ruleType={}", tenantId, seed.ruleType());
	}

	private void seedDefaultPayComponents(Long tenantId) {
		PAY_COMPONENT_SEEDS.forEach(seed -> seedPayComponent(tenantId, seed));
	}

	private void seedPayComponent(Long tenantId, PayComponentSeed seed) {
		if (payComponentDefinitionRepository.existsByCodeAndTenantId(seed.code(), tenantId)) {
			log.info("action=seed-pay-component-skipped tenantId={} code={} reason=already-exists", tenantId,
					seed.code());
			return;
		}
		payComponentDefinitionRepository
				.save(PayComponentDefinition.create(seed.code(), seed.name(), seed.type(), seed.displayOrder()));
		log.info("action=seed-pay-component-created tenantId={} code={}", tenantId, seed.code());
	}

	private void seedDefaultExamTypes(Long tenantId) {
		EXAM_TYPE_SEEDS.forEach(seed -> seedExamType(tenantId, seed));
	}

	private void seedExamType(Long tenantId, ExamTypeSeed seed) {
		if (examTypeDefinitionRepository.existsByCodeAndTenantId(seed.code(), tenantId)) {
			log.info("action=seed-exam-type-skipped tenantId={} code={} reason=already-exists", tenantId,
					seed.code());
			return;
		}
		examTypeDefinitionRepository.save(ExamTypeDefinition.create(seed.code(), seed.name(), seed.displayOrder()));
		log.info("action=seed-exam-type-created tenantId={} code={}", tenantId, seed.code());
	}

	private void seedDefaultDataRetentionPolicy(Long tenantId) {
		boolean exists = dataRetentionPolicyService.findAllForTenant(tenantId).stream()
				.anyMatch(policy -> STUDENT_ENTITY_TYPE.equals(policy.getEntityType()));
		if (exists) {
			log.info("action=seed-retention-policy-skipped tenantId={} entityType={} reason=already-exists",
					tenantId, STUDENT_ENTITY_TYPE);
			return;
		}
		dataRetentionPolicyService.create(tenantId, STUDENT_ENTITY_TYPE, STUDENT_RETENTION_PERIOD_DAYS,
				DeletionPolicy.ANONYMIZE,
				"Default starting point — confirm against the tenant's actual jurisdiction before relying on it");
		log.info("action=seed-retention-policy-created tenantId={} entityType={} retentionPeriodDays={}", tenantId,
				STUDENT_ENTITY_TYPE, STUDENT_RETENTION_PERIOD_DAYS);
	}

	private String serializeVariableNames(List<String> variableNames) {
		try {
			return objectMapper.writeValueAsString(variableNames);
		} catch (JacksonException e) {
			throw new BusinessException("Failed to serialize notification template variables");
		}
	}

	private record TemplateSeed(NotificationType type, String subject, String body, List<String> variableNames) {
	}

	private record ThresholdSeed(String letter, BigDecimal minPercentage, BigDecimal points) {
	}

	private record DashboardReportSeed(String name, String providerBeanName) {
	}

	private record AlertRuleSeed(String ruleType, String name, BigDecimal thresholdValue,
			NotificationType notificationType) {
	}

	private record PayComponentSeed(String code, String name, PayComponentType type, int displayOrder) {
	}

	private record ExamTypeSeed(String code, String name, int displayOrder) {
	}
}
