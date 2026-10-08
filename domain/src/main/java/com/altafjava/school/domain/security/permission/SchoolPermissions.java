package com.altafjava.school.domain.security.permission;

import java.util.Set;
import com.altafjava.platform.core.security.permission.PermissionDefinition;

/**
 * school-saas's contribution to the platform's tenant-scoped role/permission catalog, registered
 * via {@code SchoolPlatformConfigurer#domainPermissionCatalog()} — the domain-generic replacement
 * for what used to be a fixed, compile-time role check ({@code SchoolRoles.HAS_*}) on every
 * controller. Every code here is what a tenant admin's role-builder grants/revokes, and what
 * {@code @PreAuthorize("@permissionAuthorizationService.hasPermission('...')")} tests at each
 * controller endpoint. The default TEACHER/PARENT/STUDENT/PRINCIPAL/FINANCE/HR/ACADEMIC roles seed
 * exactly the subset each one needs to preserve pre-migration behavior (see
 * {@code 008-seed-school-roles.xml}/{@code 023-seed-dashboard-roles.xml}) — a tenant admin can
 * then grant any of these to a custom role instead.
 */
public final class SchoolPermissions {

	private static final String CAT_ACADEMIC = "Academic";
	private static final String CAT_ADMISSIONS = "Admissions & Enrollment";
	private static final String CAT_ATTENDANCE = "Attendance & Grades";
	private static final String CAT_FACILITIES = "Assets & Facilities";
	private static final String CAT_COMMUNICATION = "Communication & Events";
	private static final String CAT_DISCIPLINE = "Discipline & Wellbeing";
	private static final String CAT_FINANCE = "Fees & Payroll";
	private static final String CAT_GUARDIAN = "Guardians";
	private static final String CAT_HR = "HR & Staff";
	private static final String CAT_LIBRARY = "Library";
	private static final String CAT_TRANSPORT = "Transport";
	private static final String CAT_DASHBOARD = "Dashboards";
	private static final String CAT_CUSTOM_FIELD = "Custom Fields";
	private static final String CAT_TICKET = "Helpdesk";
	private static final String CAT_CERTIFICATE = "Certificates";

	public static final Set<PermissionDefinition> CATALOG = Set.of(
			def("ACADEMIC_YEAR_READ", "View academic years", CAT_ACADEMIC),
			def("ACADEMIC_YEAR_WRITE", "Create academic years", CAT_ACADEMIC),
			def("ADMISSION_FEE_MANAGE", "Record or waive application fees and set the application fee", CAT_ADMISSIONS),
			def("ADMISSION_MANAGE", "Manage admission applications and decisions", CAT_ADMISSIONS),
			def("ALUMNI_MANAGE", "Manage alumni profiles", CAT_ADMISSIONS),
			def("ASSET_MANAGE", "Manage school assets and their assignment", CAT_FACILITIES),
			def("ASSIGNMENT_READ", "View assignments", CAT_ACADEMIC),
			def("ASSIGNMENT_WRITE", "Create and reschedule assignments", CAT_ACADEMIC),
			def("BOARD_READ", "View education boards", CAT_ACADEMIC),
			def("BOARD_WRITE", "Manage education boards", CAT_ACADEMIC),
			def("BOOK_MANAGE", "Manage the library catalog", CAT_LIBRARY),
			def("BOOK_READ", "Browse the library catalog", CAT_LIBRARY),
			def("CERTIFICATE_MANAGE", "Issue, download and revoke student certificates", CAT_CERTIFICATE),
			def("CERTIFICATE_TYPE_READ", "View certificate types", CAT_CERTIFICATE),
			def("CERTIFICATE_TYPE_WRITE", "Manage certificate types and their wording", CAT_CERTIFICATE),
			def("CIRCULATION_MANAGE", "Check library books in and out", CAT_LIBRARY),
			def("CLASSROOM_READ", "View classrooms", CAT_ACADEMIC),
			def("CLASSROOM_WRITE", "Manage classrooms", CAT_ACADEMIC),
			def("COUNSELING_MANAGE", "Manage counseling referrals and sessions", CAT_DISCIPLINE),
			def("CURRICULUM_READ", "View curricula", CAT_ACADEMIC),
			def("CURRICULUM_WRITE", "Manage curricula", CAT_ACADEMIC),
			def("CUSTOM_FIELD_DEFINITION_MANAGE", "Define custom fields", CAT_CUSTOM_FIELD),
			def("CUSTOM_FIELD_VALUE_READ", "View custom field values", CAT_CUSTOM_FIELD),
			def("CUSTOM_FIELD_VALUE_WRITE", "Edit custom field values", CAT_CUSTOM_FIELD),
			def("DASHBOARD_ACADEMIC_READ", "View the academic dashboard", CAT_DASHBOARD),
			def("DASHBOARD_FINANCE_READ", "View the finance dashboard", CAT_DASHBOARD),
			def("DASHBOARD_HR_READ", "View the HR dashboard", CAT_DASHBOARD),
			def("DASHBOARD_PRINCIPAL_READ", "View the principal's dashboard", CAT_DASHBOARD),
			def("DEPARTMENT_MANAGE", "Manage academic departments", CAT_ACADEMIC),
			def("DISCIPLINE_ACTION", "Act on a discipline incident", CAT_DISCIPLINE),
			def("DISCIPLINE_MANAGE", "View every discipline incident tenant-wide", CAT_DISCIPLINE),
			def("DISCIPLINE_READ", "View a specific student's discipline incidents", CAT_DISCIPLINE),
			def("DISCIPLINE_WRITE", "Record a discipline incident", CAT_DISCIPLINE),
			def("EMPLOYEE_MANAGE", "Hire, update and offboard staff", CAT_HR),
			def("EMPLOYEE_READ", "View staff records", CAT_HR),
			def("EVENT_MANAGE", "Create, edit, and cancel school events", CAT_COMMUNICATION),
			def("EVENT_READ", "View school events", CAT_COMMUNICATION),
			def("EVENT_REGISTER", "Register for and cancel event registration", CAT_COMMUNICATION),
			def("EVENT_STAFF_MANAGE", "View and mark event registration attendance", CAT_COMMUNICATION),
			def("EXAM_COMPLETE", "Mark an exam complete", CAT_ACADEMIC),
			def("EXAM_RESULT_PUBLISH", "Publish or withdraw exam results", CAT_ACADEMIC),
			def("EXAM_TYPE_MANAGE", "Manage tenant exam-type definitions", CAT_ACADEMIC),
			def("EXAM_WRITE", "Schedule, reassign, and cancel exams", CAT_ACADEMIC),
			def("FEE_DISCOUNT_MANAGE", "Grant, revoke and view student fee discounts", CAT_FINANCE),
			def("FEE_INSTALLMENT_MANAGE", "Set and view student fee installment plans", CAT_FINANCE),
			def("FEE_PAYMENT_MANAGE", "View and record fee payments", CAT_FINANCE),
			def("FEE_PAYMENT_SELF_SERVICE", "Pay fees and view own payment history", CAT_FINANCE),
			def("FEE_REFUND_MANAGE", "Refund fee payments and view refunds", CAT_FINANCE),
			def("FEE_STRUCTURE_MANAGE", "Manage fee structures and assignments", CAT_FINANCE),
			def("GRADING_SCALE_READ", "View grading scales", CAT_ACADEMIC),
			def("GRADING_SCALE_WRITE", "Manage grading scales", CAT_ACADEMIC),
			def("EMERGENCY_CONTACT_MANAGE", "View and manage student emergency contacts", CAT_GUARDIAN),
			def("GUARDIAN_CONSENT_MANAGE", "Grant or revoke guardian consent for a student", CAT_GUARDIAN),
			def("GUARDIAN_MANAGE", "Manage guardian records", CAT_GUARDIAN),
			def("GUARDIAN_PICKUP_MANAGE", "Authorize pickup and restrict custody for a guardian", CAT_GUARDIAN),
			def("GUARDIAN_PICKUP_READ", "Check who is authorized to collect a student", CAT_GUARDIAN),
			def("GUARDIAN_REGISTRATION_SETTINGS_MANAGE", "Configure guardian self-registration", CAT_GUARDIAN),
			def("GUARDIAN_SELF_SERVICE", "View own linked students as a guardian", CAT_GUARDIAN),
			def("HEALTH_RECORD_MANAGE", "View and edit student health records", CAT_DISCIPLINE),
			def("HOLIDAY_READ", "View the school calendar", CAT_ACADEMIC),
			def("HOLIDAY_WRITE", "Manage the school calendar", CAT_ACADEMIC),
			def("HOSTEL_READ", "View hostel buildings", CAT_FACILITIES),
			def("HOSTEL_WRITE", "Manage hostel buildings", CAT_FACILITIES),
			def("ID_CARD_ISSUE", "Issue student and staff ID cards", CAT_ADMISSIONS),
			def("LEAVE_BALANCE_MANAGE", "View any teacher's leave balance", CAT_HR),
			def("LEAVE_REQUEST_MANAGE", "View and decide on leave requests", CAT_HR),
			def("LEAVE_REQUEST_REVIEW", "Approve leave requests of the department you head", CAT_HR),
			def("LEAVE_SELF_SERVICE", "Submit and cancel own leave requests", CAT_HR),
			def("LEAVE_TYPE_READ", "View leave types", CAT_HR),
			def("LEAVE_TYPE_WRITE", "Manage leave types", CAT_HR),
			def("LESSON_READ", "View lessons", CAT_ACADEMIC),
			def("LESSON_WRITE", "Create lessons", CAT_ACADEMIC),
			def("MEDICAL_INCIDENT_MANAGE", "View and record medical incidents", CAT_DISCIPLINE),
			def("PAYSLIP_DISBURSE", "Disburse finalized payslips", CAT_HR),
			def("PAYSLIP_MANAGE", "View and finalize payslips", CAT_HR),
			def("PAY_COMPONENT_MANAGE", "Manage tenant pay-component definitions", CAT_HR),
			def("PERIOD_ATTENDANCE_MANAGE", "View and record period-level attendance", CAT_ATTENDANCE),
			def("PERIOD_READ", "View timetable periods", CAT_ACADEMIC),
			def("PERIOD_WRITE", "Manage timetable periods", CAT_ACADEMIC),
			def("REPORT_CARD_TEMPLATE_READ", "View the report card template", CAT_ACADEMIC),
			def("REPORT_CARD_TEMPLATE_WRITE", "Configure the report card template", CAT_ACADEMIC),
			def("ROOM_ALLOCATION_READ", "View hostel room allocations", CAT_FACILITIES),
			def("ROOM_ALLOCATION_WRITE", "Allocate and vacate hostel rooms", CAT_FACILITIES),
			def("ROOM_READ", "View hostel rooms", CAT_FACILITIES),
			def("ROOM_WRITE", "Manage hostel rooms", CAT_FACILITIES),
			def("SALARY_STRUCTURE_MANAGE", "Manage staff salary structures", CAT_HR),
			def("STUDENT_ATTENDANCE_READ", "View student attendance", CAT_ATTENDANCE),
			def("STUDENT_ATTENDANCE_WRITE", "Mark and correct student attendance", CAT_ATTENDANCE),
			def("STUDENT_DOCUMENT_READ", "View student record documents", CAT_ADMISSIONS),
			def("STUDENT_DOCUMENT_MANAGE", "Upload and verify student record documents", CAT_ADMISSIONS),
			def("STUDENT_FEE_BALANCE_READ", "View a student's fee balance", CAT_FINANCE),
			def("STUDENT_GRADES_READ", "View student grades", CAT_ATTENDANCE),
			def("STUDENT_GRADES_WRITE", "Record and correct student grades", CAT_ATTENDANCE),
			def("STUDENT_MANAGE", "Manage student records and lifecycle", CAT_ADMISSIONS),
			def("STUDENT_READ", "View student records", CAT_ADMISSIONS),
			def("STUDENT_SELF_SERVICE_READ", "View own/own child's grades, attendance, and report cards",
					CAT_ADMISSIONS),
			def("SUBJECT_READ", "View subjects", CAT_ACADEMIC),
			def("SUBJECT_WRITE", "Manage subjects", CAT_ACADEMIC),
			def("SUBMISSION_GRADE", "Grade assignment submissions", CAT_ACADEMIC),
			def("SUBMISSION_READ", "View assignment submissions", CAT_ACADEMIC),
			def("SUBMISSION_SUBMIT", "Submit an assignment", CAT_ACADEMIC),
			def("SUBSTITUTION_MANAGE", "Assign and cancel substitute teachers", CAT_ACADEMIC),
			def("TEACHER_MANAGE", "Manage teacher records", CAT_HR),
			def("TERM_READ", "View academic terms", CAT_ACADEMIC),
			def("TERM_WRITE", "Create academic terms", CAT_ACADEMIC),
			def("TICKET_MANAGE", "View and resolve helpdesk tickets", CAT_TICKET),
			def("TICKET_SELF_SERVICE", "Raise and view own helpdesk tickets", CAT_TICKET),
			def("TIMETABLE_READ", "View the timetable", CAT_ACADEMIC),
			def("TIMETABLE_WRITE", "Manage the timetable", CAT_ACADEMIC),
			def("TRANSPORT_ASSIGNMENT_READ", "View student transport assignments", CAT_TRANSPORT),
			def("TRANSPORT_ASSIGNMENT_WRITE", "Manage student transport assignments", CAT_TRANSPORT),
			def("TRANSPORT_ROUTE_READ", "View transport routes", CAT_TRANSPORT),
			def("TRANSPORT_ROUTE_WRITE", "Manage transport routes", CAT_TRANSPORT),
			def("VEHICLE_READ", "View transport vehicles", CAT_TRANSPORT),
			def("VEHICLE_WRITE", "Manage transport vehicles", CAT_TRANSPORT),
			def("VENUE_MANAGE", "Manage teaching venues", CAT_ACADEMIC),
			def("VISITOR_LOG_MANAGE", "Check visitors in and out", CAT_FACILITIES),
			def("VISITOR_REQUEST_APPROVE", "Approve or reject any visitor request", CAT_FACILITIES),
			def("VISITOR_REQUEST_SELF_SERVICE", "Pre-register visitors and decide on the visits you host",
					CAT_FACILITIES));

	private static PermissionDefinition def(String code, String description, String category) {
		return new PermissionDefinition(code, description, category);
	}

	private SchoolPermissions() {
	}
}
