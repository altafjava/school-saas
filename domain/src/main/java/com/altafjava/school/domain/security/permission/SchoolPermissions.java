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
 * {@code 008-seed-school-roles.xml}/{@code 019-seed-dashboard-roles.xml}) — a tenant admin can
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

	/** Widens classroom-scoped reads from "the classrooms I teach" to every classroom. */
	public static final String ALL_CLASSROOMS_READ = "ALL_CLASSROOMS_READ";
	/** Widens classroom-scoped writes the same way; implies {@link #ALL_CLASSROOMS_READ}. */
	public static final String ALL_CLASSROOMS_WRITE = "ALL_CLASSROOMS_WRITE";

	public static final String ACADEMIC_YEAR_READ = "ACADEMIC_YEAR_READ";
	public static final String ACADEMIC_YEAR_WRITE = "ACADEMIC_YEAR_WRITE";
	public static final String ADMISSION_FEE_MANAGE = "ADMISSION_FEE_MANAGE";
	public static final String ADMISSION_MANAGE = "ADMISSION_MANAGE";
	public static final String ALUMNI_MANAGE = "ALUMNI_MANAGE";
	public static final String ANNOUNCEMENT_CLASS_POST = "ANNOUNCEMENT_CLASS_POST";
	public static final String ANNOUNCEMENT_MANAGE = "ANNOUNCEMENT_MANAGE";
	public static final String ANNOUNCEMENT_READ = "ANNOUNCEMENT_READ";
	public static final String ASSET_MANAGE = "ASSET_MANAGE";
	public static final String ASSIGNMENT_READ = "ASSIGNMENT_READ";
	public static final String ASSIGNMENT_WRITE = "ASSIGNMENT_WRITE";
	public static final String BOARD_READ = "BOARD_READ";
	public static final String BOARD_WRITE = "BOARD_WRITE";
	public static final String BULK_IMPORT_MANAGE = "BULK_IMPORT_MANAGE";
	public static final String BOOK_MANAGE = "BOOK_MANAGE";
	public static final String BOOK_READ = "BOOK_READ";
	public static final String CERTIFICATE_MANAGE = "CERTIFICATE_MANAGE";
	public static final String CERTIFICATE_TYPE_READ = "CERTIFICATE_TYPE_READ";
	public static final String CERTIFICATE_TYPE_WRITE = "CERTIFICATE_TYPE_WRITE";
	public static final String CIRCULATION_MANAGE = "CIRCULATION_MANAGE";
	public static final String CLASSROOM_READ = "CLASSROOM_READ";
	public static final String CLASSROOM_WRITE = "CLASSROOM_WRITE";
	public static final String COUNSELING_MANAGE = "COUNSELING_MANAGE";
	public static final String CURRICULUM_READ = "CURRICULUM_READ";
	public static final String CURRICULUM_WRITE = "CURRICULUM_WRITE";
	public static final String CUSTOM_FIELD_DEFINITION_MANAGE = "CUSTOM_FIELD_DEFINITION_MANAGE";
	public static final String CUSTOM_FIELD_VALUE_READ = "CUSTOM_FIELD_VALUE_READ";
	public static final String CUSTOM_FIELD_VALUE_WRITE = "CUSTOM_FIELD_VALUE_WRITE";
	public static final String DASHBOARD_ACADEMIC_READ = "DASHBOARD_ACADEMIC_READ";
	public static final String DASHBOARD_FINANCE_READ = "DASHBOARD_FINANCE_READ";
	public static final String DASHBOARD_HR_READ = "DASHBOARD_HR_READ";
	public static final String DASHBOARD_PRINCIPAL_READ = "DASHBOARD_PRINCIPAL_READ";
	public static final String DEPARTMENT_MANAGE = "DEPARTMENT_MANAGE";
	public static final String DISCIPLINE_ACTION = "DISCIPLINE_ACTION";
	public static final String DISCIPLINE_MANAGE = "DISCIPLINE_MANAGE";
	public static final String DISCIPLINE_READ = "DISCIPLINE_READ";
	public static final String DISCIPLINE_WRITE = "DISCIPLINE_WRITE";
	public static final String EMERGENCY_CONTACT_MANAGE = "EMERGENCY_CONTACT_MANAGE";
	public static final String EMPLOYEE_MANAGE = "EMPLOYEE_MANAGE";
	public static final String EMPLOYEE_READ = "EMPLOYEE_READ";
	public static final String EVENT_MANAGE = "EVENT_MANAGE";
	public static final String ENQUIRY_MANAGE = "ENQUIRY_MANAGE";
	public static final String EVENT_READ = "EVENT_READ";
	public static final String EVENT_REGISTER = "EVENT_REGISTER";
	public static final String EVENT_STAFF_MANAGE = "EVENT_STAFF_MANAGE";
	public static final String EXAM_COMPLETE = "EXAM_COMPLETE";
	public static final String EXAM_MARKS_VERIFY = "EXAM_MARKS_VERIFY";
	public static final String EXAM_READ = "EXAM_READ";
	public static final String EXAM_RESULT_PUBLISH = "EXAM_RESULT_PUBLISH";
	public static final String EXAM_TYPE_MANAGE = "EXAM_TYPE_MANAGE";
	public static final String EXAM_WRITE = "EXAM_WRITE";
	public static final String FEE_DISCOUNT_MANAGE = "FEE_DISCOUNT_MANAGE";
	public static final String FEE_INSTALLMENT_MANAGE = "FEE_INSTALLMENT_MANAGE";
	public static final String FEE_PAYMENT_MANAGE = "FEE_PAYMENT_MANAGE";
	public static final String FEE_INVOICE_MANAGE = "FEE_INVOICE_MANAGE";
	public static final String FEE_PAYMENT_SELF_SERVICE = "FEE_PAYMENT_SELF_SERVICE";
	public static final String FEE_REFUND_MANAGE = "FEE_REFUND_MANAGE";
	public static final String FEE_STRUCTURE_MANAGE = "FEE_STRUCTURE_MANAGE";
	public static final String GRADING_SCALE_READ = "GRADING_SCALE_READ";
	public static final String GRADING_SCALE_WRITE = "GRADING_SCALE_WRITE";
	public static final String GUARDIAN_CONSENT_MANAGE = "GUARDIAN_CONSENT_MANAGE";
	public static final String GUARDIAN_MANAGE = "GUARDIAN_MANAGE";
	public static final String GUARDIAN_PICKUP_MANAGE = "GUARDIAN_PICKUP_MANAGE";
	public static final String GUARDIAN_PICKUP_READ = "GUARDIAN_PICKUP_READ";
	public static final String GUARDIAN_REGISTRATION_SETTINGS_MANAGE = "GUARDIAN_REGISTRATION_SETTINGS_MANAGE";
	public static final String GUARDIAN_SELF_SERVICE = "GUARDIAN_SELF_SERVICE";
	public static final String HEALTH_RECORD_MANAGE = "HEALTH_RECORD_MANAGE";
	public static final String HOLIDAY_READ = "HOLIDAY_READ";
	public static final String PARENT_HOME_READ = "PARENT_HOME_READ";
	public static final String STUDENT_HOME_READ = "STUDENT_HOME_READ";
	public static final String TEACHER_HOME_READ = "TEACHER_HOME_READ";
	public static final String HOLIDAY_WRITE = "HOLIDAY_WRITE";
	public static final String HOSTEL_READ = "HOSTEL_READ";
	public static final String HOSTEL_WRITE = "HOSTEL_WRITE";
	public static final String ID_CARD_ISSUE = "ID_CARD_ISSUE";
	public static final String LEAVE_BALANCE_MANAGE = "LEAVE_BALANCE_MANAGE";
	public static final String LEAVE_REQUEST_MANAGE = "LEAVE_REQUEST_MANAGE";
	public static final String LEAVE_REQUEST_REVIEW = "LEAVE_REQUEST_REVIEW";
	public static final String LEAVE_SELF_SERVICE = "LEAVE_SELF_SERVICE";
	public static final String LEAVE_TYPE_READ = "LEAVE_TYPE_READ";
	public static final String LEAVE_TYPE_WRITE = "LEAVE_TYPE_WRITE";
	public static final String LESSON_READ = "LESSON_READ";
	public static final String LESSON_WRITE = "LESSON_WRITE";
	public static final String MEDICAL_INCIDENT_MANAGE = "MEDICAL_INCIDENT_MANAGE";
	public static final String PAYSLIP_DISBURSE = "PAYSLIP_DISBURSE";
	public static final String PAYSLIP_MANAGE = "PAYSLIP_MANAGE";
	public static final String PAY_COMPONENT_MANAGE = "PAY_COMPONENT_MANAGE";
	public static final String PERIOD_ATTENDANCE_MANAGE = "PERIOD_ATTENDANCE_MANAGE";
	public static final String PERIOD_READ = "PERIOD_READ";
	public static final String PERIOD_WRITE = "PERIOD_WRITE";
	public static final String REPORT_CARD_TEMPLATE_READ = "REPORT_CARD_TEMPLATE_READ";
	public static final String REPORT_CARD_TEMPLATE_WRITE = "REPORT_CARD_TEMPLATE_WRITE";
	public static final String ROOM_ALLOCATION_READ = "ROOM_ALLOCATION_READ";
	public static final String ROOM_ALLOCATION_WRITE = "ROOM_ALLOCATION_WRITE";
	public static final String ROOM_READ = "ROOM_READ";
	public static final String ROOM_WRITE = "ROOM_WRITE";
	public static final String SALARY_STRUCTURE_MANAGE = "SALARY_STRUCTURE_MANAGE";
	public static final String STAFF_ATTENDANCE_MANAGE = "STAFF_ATTENDANCE_MANAGE";
	public static final String STAFF_ATTENDANCE_READ = "STAFF_ATTENDANCE_READ";
	public static final String STAFF_ATTENDANCE_SELF_SERVICE = "STAFF_ATTENDANCE_SELF_SERVICE";
	public static final String STUDENT_ATTENDANCE_READ = "STUDENT_ATTENDANCE_READ";
	public static final String STUDENT_ATTENDANCE_WRITE = "STUDENT_ATTENDANCE_WRITE";
	public static final String STUDENT_DOCUMENT_MANAGE = "STUDENT_DOCUMENT_MANAGE";
	public static final String STUDENT_DOCUMENT_READ = "STUDENT_DOCUMENT_READ";
	public static final String STUDENT_FEE_BALANCE_READ = "STUDENT_FEE_BALANCE_READ";
	public static final String STUDENT_GRADES_READ = "STUDENT_GRADES_READ";
	public static final String STUDENT_GRADES_WRITE = "STUDENT_GRADES_WRITE";
	public static final String EMERGENCY_ALERT_SEND = "EMERGENCY_ALERT_SEND";
	public static final String MESSAGING_PARTICIPATE = "MESSAGING_PARTICIPATE";
	public static final String MESSAGING_REVIEW = "MESSAGING_REVIEW";
	public static final String MESSAGING_SETTINGS_MANAGE = "MESSAGING_SETTINGS_MANAGE";
	public static final String STUDENT_LEAVE_MANAGE = "STUDENT_LEAVE_MANAGE";
	public static final String STUDENT_LEAVE_REVIEW = "STUDENT_LEAVE_REVIEW";
	public static final String STUDENT_LEAVE_SELF_SERVICE = "STUDENT_LEAVE_SELF_SERVICE";
	public static final String STUDENT_MANAGE = "STUDENT_MANAGE";
	public static final String STUDENT_PROMOTE = "STUDENT_PROMOTE";
	public static final String STUDENT_READ = "STUDENT_READ";
	public static final String STUDENT_READ_ALL = "STUDENT_READ_ALL";
	public static final String STUDENT_SELF_SERVICE_READ = "STUDENT_SELF_SERVICE_READ";
	public static final String SUBJECT_READ = "SUBJECT_READ";
	public static final String SUBJECT_WRITE = "SUBJECT_WRITE";
	public static final String SUBMISSION_GRADE = "SUBMISSION_GRADE";
	public static final String SUBMISSION_READ = "SUBMISSION_READ";
	public static final String SUBMISSION_SUBMIT = "SUBMISSION_SUBMIT";
	public static final String SUBSTITUTION_MANAGE = "SUBSTITUTION_MANAGE";
	public static final String TEACHER_MANAGE = "TEACHER_MANAGE";
	public static final String TERM_READ = "TERM_READ";
	public static final String TERM_WRITE = "TERM_WRITE";
	public static final String TICKET_MANAGE = "TICKET_MANAGE";
	public static final String TICKET_SELF_SERVICE = "TICKET_SELF_SERVICE";
	public static final String TIMETABLE_READ = "TIMETABLE_READ";
	public static final String TIMETABLE_WRITE = "TIMETABLE_WRITE";
	public static final String TRANSPORT_ASSIGNMENT_READ = "TRANSPORT_ASSIGNMENT_READ";
	public static final String TRANSPORT_ASSIGNMENT_WRITE = "TRANSPORT_ASSIGNMENT_WRITE";
	public static final String TRANSPORT_ROUTE_READ = "TRANSPORT_ROUTE_READ";
	public static final String TRANSPORT_ROUTE_WRITE = "TRANSPORT_ROUTE_WRITE";
	public static final String VEHICLE_READ = "VEHICLE_READ";
	public static final String VEHICLE_WRITE = "VEHICLE_WRITE";
	public static final String VENUE_MANAGE = "VENUE_MANAGE";
	public static final String VISITOR_LOG_MANAGE = "VISITOR_LOG_MANAGE";
	public static final String VISITOR_REQUEST_APPROVE = "VISITOR_REQUEST_APPROVE";
	public static final String VISITOR_REQUEST_SELF_SERVICE = "VISITOR_REQUEST_SELF_SERVICE";

	public static final Set<PermissionDefinition> CATALOG = Set.of(
			def(ACADEMIC_YEAR_READ, "View academic years", CAT_ACADEMIC),
			def(ACADEMIC_YEAR_WRITE, "Create academic years", CAT_ACADEMIC),
			def(ADMISSION_FEE_MANAGE, "Record or waive application fees and set the application fee", CAT_ADMISSIONS),
			def(ADMISSION_MANAGE, "Manage admission applications and decisions", CAT_ADMISSIONS),
			def(ALL_CLASSROOMS_READ, "View attendance, grades and coursework of every classroom, not only those taught",
					CAT_ATTENDANCE),
			def(ALL_CLASSROOMS_WRITE,
					"Record attendance, grades and coursework for every classroom, not only those taught",
					CAT_ATTENDANCE),
			def(ALUMNI_MANAGE, "Manage alumni profiles", CAT_ADMISSIONS),
			def(ASSET_MANAGE, "Manage school assets and their assignment", CAT_FACILITIES),
			def(ASSIGNMENT_READ, "View assignments", CAT_ACADEMIC),
			def(ASSIGNMENT_WRITE, "Create and reschedule assignments", CAT_ACADEMIC),
			def(BOARD_READ, "View education boards", CAT_ACADEMIC),
			def(BOARD_WRITE, "Manage education boards", CAT_ACADEMIC),
			def(BULK_IMPORT_MANAGE, "Upload, dry-run and run CSV imports, and see their history", CAT_ADMISSIONS),
			def(BOOK_MANAGE, "Manage the library catalog", CAT_LIBRARY),
			def(BOOK_READ, "Browse the library catalog", CAT_LIBRARY),
			def(CERTIFICATE_MANAGE, "Issue, download and revoke student certificates", CAT_CERTIFICATE),
			def(CERTIFICATE_TYPE_READ, "View certificate types", CAT_CERTIFICATE),
			def(CERTIFICATE_TYPE_WRITE, "Manage certificate types and their wording", CAT_CERTIFICATE),
			def(CIRCULATION_MANAGE, "Check library books in and out", CAT_LIBRARY),
			def(CLASSROOM_READ, "View classrooms", CAT_ACADEMIC),
			def(CLASSROOM_WRITE, "Manage classrooms", CAT_ACADEMIC),
			def(COUNSELING_MANAGE, "Manage counseling referrals and sessions", CAT_DISCIPLINE),
			def(CURRICULUM_READ, "View curricula", CAT_ACADEMIC),
			def(CURRICULUM_WRITE, "Manage curricula", CAT_ACADEMIC),
			def(CUSTOM_FIELD_DEFINITION_MANAGE, "Define custom fields", CAT_CUSTOM_FIELD),
			def(CUSTOM_FIELD_VALUE_READ, "View custom field values", CAT_CUSTOM_FIELD),
			def(CUSTOM_FIELD_VALUE_WRITE, "Edit custom field values", CAT_CUSTOM_FIELD),
			def(DASHBOARD_ACADEMIC_READ, "View the academic dashboard", CAT_DASHBOARD),
			def(DASHBOARD_FINANCE_READ, "View the finance dashboard", CAT_DASHBOARD),
			def(DASHBOARD_HR_READ, "View the HR dashboard", CAT_DASHBOARD),
			def(DASHBOARD_PRINCIPAL_READ, "View the principal's dashboard", CAT_DASHBOARD),
			def(DEPARTMENT_MANAGE, "Manage academic departments", CAT_ACADEMIC),
			def(DISCIPLINE_ACTION, "Act on a discipline incident", CAT_DISCIPLINE),
			def(DISCIPLINE_MANAGE, "View every discipline incident tenant-wide", CAT_DISCIPLINE),
			def(DISCIPLINE_READ, "View a specific student's discipline incidents", CAT_DISCIPLINE),
			def(DISCIPLINE_WRITE, "Record a discipline incident", CAT_DISCIPLINE),
			def(EMPLOYEE_MANAGE, "Hire, update and offboard staff", CAT_HR),
			def(EMPLOYEE_READ, "View staff records", CAT_HR),
			def(EVENT_MANAGE, "Create, edit, and cancel school events", CAT_COMMUNICATION),
			def(ENQUIRY_MANAGE, "Receive, follow up, close and convert admission enquiries", CAT_ADMISSIONS),
			def(EVENT_READ, "View school events", CAT_COMMUNICATION),
			def(EVENT_REGISTER, "Register for and cancel event registration", CAT_COMMUNICATION),
			def(EVENT_STAFF_MANAGE, "View and mark event registration attendance", CAT_COMMUNICATION),
			def(EXAM_COMPLETE, "Mark an exam complete", CAT_ACADEMIC),
			def(EXAM_MARKS_VERIFY, "Verify an exam's marks, or return them for correction", CAT_ACADEMIC),
			def(EXAM_READ, "View the exam schedule within the caller's reach", CAT_ACADEMIC),
			def(EXAM_RESULT_PUBLISH, "Publish or withdraw exam results", CAT_ACADEMIC),
			def(EXAM_TYPE_MANAGE, "Manage tenant exam-type definitions", CAT_ACADEMIC),
			def(EXAM_WRITE, "Schedule, reassign, and cancel exams", CAT_ACADEMIC),
			def(FEE_DISCOUNT_MANAGE, "Grant, revoke and view student fee discounts", CAT_FINANCE),
			def(FEE_INSTALLMENT_MANAGE, "Set and view student fee installment plans", CAT_FINANCE),
			def(FEE_PAYMENT_MANAGE, "View and record fee payments", CAT_FINANCE),
			def(FEE_INVOICE_MANAGE, "Issue, view and cancel student fee invoices", CAT_FINANCE),
			def(FEE_PAYMENT_SELF_SERVICE, "Pay fees and view own payment history", CAT_FINANCE),
			def(FEE_REFUND_MANAGE, "Refund fee payments and view refunds", CAT_FINANCE),
			def(FEE_STRUCTURE_MANAGE, "Manage fee structures and assignments", CAT_FINANCE),
			def(GRADING_SCALE_READ, "View grading scales", CAT_ACADEMIC),
			def(GRADING_SCALE_WRITE, "Manage grading scales", CAT_ACADEMIC),
			def(EMERGENCY_CONTACT_MANAGE, "View and manage student emergency contacts", CAT_GUARDIAN),
			def(GUARDIAN_CONSENT_MANAGE, "Grant or revoke guardian consent for a student", CAT_GUARDIAN),
			def(GUARDIAN_MANAGE, "Manage guardian records", CAT_GUARDIAN),
			def(GUARDIAN_PICKUP_MANAGE, "Authorize pickup and restrict custody for a guardian", CAT_GUARDIAN),
			def(GUARDIAN_PICKUP_READ, "Check who is authorized to collect a student", CAT_GUARDIAN),
			def(GUARDIAN_REGISTRATION_SETTINGS_MANAGE, "Configure guardian self-registration", CAT_GUARDIAN),
			def(GUARDIAN_SELF_SERVICE, "View own linked students as a guardian", CAT_GUARDIAN),
			def(HEALTH_RECORD_MANAGE, "View and edit student health records", CAT_DISCIPLINE),
			def(HOLIDAY_READ, "View the school calendar", CAT_ACADEMIC),
			def(PARENT_HOME_READ, "View the parent home screen for own linked children", CAT_DASHBOARD),
			def(STUDENT_HOME_READ, "View the student home screen for own record", CAT_DASHBOARD),
			def(TEACHER_HOME_READ, "View the teacher home screen for own classes", CAT_DASHBOARD),
			def(HOLIDAY_WRITE, "Manage the school calendar", CAT_ACADEMIC),
			def(HOSTEL_READ, "View hostel buildings", CAT_FACILITIES),
			def(HOSTEL_WRITE, "Manage hostel buildings", CAT_FACILITIES),
			def(ID_CARD_ISSUE, "Issue student and staff ID cards", CAT_ADMISSIONS),
			def(LEAVE_BALANCE_MANAGE, "View any teacher's leave balance", CAT_HR),
			def(LEAVE_REQUEST_MANAGE, "View and decide on leave requests", CAT_HR),
			def(LEAVE_REQUEST_REVIEW, "Approve leave requests of the department you head", CAT_HR),
			def(LEAVE_SELF_SERVICE, "Submit and cancel own leave requests", CAT_HR),
			def(LEAVE_TYPE_READ, "View leave types", CAT_HR),
			def(LEAVE_TYPE_WRITE, "Manage leave types", CAT_HR),
			def(LESSON_READ, "View lessons", CAT_ACADEMIC),
			def(LESSON_WRITE, "Create lessons", CAT_ACADEMIC),
			def(MEDICAL_INCIDENT_MANAGE, "View and record medical incidents", CAT_DISCIPLINE),
			def(PAYSLIP_DISBURSE, "Disburse finalized payslips", CAT_HR),
			def(PAYSLIP_MANAGE, "View and finalize payslips", CAT_HR),
			def(PAY_COMPONENT_MANAGE, "Manage tenant pay-component definitions", CAT_HR),
			def(PERIOD_ATTENDANCE_MANAGE, "View and record period-level attendance", CAT_ATTENDANCE),
			def(PERIOD_READ, "View timetable periods", CAT_ACADEMIC),
			def(PERIOD_WRITE, "Manage timetable periods", CAT_ACADEMIC),
			def(REPORT_CARD_TEMPLATE_READ, "View the report card template", CAT_ACADEMIC),
			def(REPORT_CARD_TEMPLATE_WRITE, "Configure the report card template", CAT_ACADEMIC),
			def(ROOM_ALLOCATION_READ, "View hostel room allocations", CAT_FACILITIES),
			def(ROOM_ALLOCATION_WRITE, "Allocate and vacate hostel rooms", CAT_FACILITIES),
			def(ROOM_READ, "View hostel rooms", CAT_FACILITIES),
			def(ROOM_WRITE, "Manage hostel rooms", CAT_FACILITIES),
			def(SALARY_STRUCTURE_MANAGE, "Manage staff salary structures", CAT_HR),
			def(STAFF_ATTENDANCE_MANAGE, "Record and correct staff attendance, and set the workday", CAT_HR),
			def(STAFF_ATTENDANCE_READ, "View staff attendance and the daily register", CAT_HR),
			def(STAFF_ATTENDANCE_SELF_SERVICE, "Check in and out, and view own attendance", CAT_HR),
			def(STUDENT_ATTENDANCE_READ, "View student attendance", CAT_ATTENDANCE),
			def(STUDENT_ATTENDANCE_WRITE, "Mark and correct student attendance", CAT_ATTENDANCE),
			def(STUDENT_DOCUMENT_READ, "View student record documents", CAT_ADMISSIONS),
			def(STUDENT_DOCUMENT_MANAGE, "Upload and verify student record documents", CAT_ADMISSIONS),
			def(STUDENT_FEE_BALANCE_READ, "View a student's fee balance", CAT_FINANCE),
			def(STUDENT_GRADES_READ, "View student grades", CAT_ATTENDANCE),
			def(STUDENT_GRADES_WRITE, "Record and correct student grades", CAT_ATTENDANCE),
			def(ANNOUNCEMENT_CLASS_POST, "Post announcements to the classrooms one teaches", CAT_COMMUNICATION),
			def(ANNOUNCEMENT_MANAGE, "Create and publish announcements to any audience", CAT_COMMUNICATION),
			def(ANNOUNCEMENT_READ, "Read announcements addressed to oneself", CAT_COMMUNICATION),
			def(EMERGENCY_ALERT_SEND, "Send emergency alerts on every channel", CAT_COMMUNICATION),
			def(MESSAGING_PARTICIPATE, "Exchange private messages between guardians and teachers", CAT_COMMUNICATION),
			def(MESSAGING_REVIEW, "Read conversations as a named reviewer, always audited", CAT_COMMUNICATION),
			def(MESSAGING_SETTINGS_MANAGE, "Set messaging hours and the on/off switch", CAT_COMMUNICATION),
			def(STUDENT_LEAVE_MANAGE, "Decide any student's leave request", CAT_ATTENDANCE),
			def(STUDENT_LEAVE_REVIEW, "Raise and decide leave for students of one's own class", CAT_ATTENDANCE),
			def(STUDENT_LEAVE_SELF_SERVICE, "Request leave for a linked child", CAT_GUARDIAN),
			def(STUDENT_MANAGE, "Manage student records and lifecycle", CAT_ADMISSIONS),
			def(STUDENT_PROMOTE, "Move a classroom into the next academic year", CAT_ACADEMIC),
			def(STUDENT_READ, "View student records within the caller's reach", CAT_ADMISSIONS),
			def(STUDENT_READ_ALL, "View every student's records, not only those the caller teaches",
					CAT_ADMISSIONS),
			def(STUDENT_SELF_SERVICE_READ, "View own/own child's grades, attendance, and report cards",
					CAT_ADMISSIONS),
			def(SUBJECT_READ, "View subjects", CAT_ACADEMIC),
			def(SUBJECT_WRITE, "Manage subjects", CAT_ACADEMIC),
			def(SUBMISSION_GRADE, "Grade assignment submissions", CAT_ACADEMIC),
			def(SUBMISSION_READ, "View assignment submissions", CAT_ACADEMIC),
			def(SUBMISSION_SUBMIT, "Submit an assignment", CAT_ACADEMIC),
			def(SUBSTITUTION_MANAGE, "Assign and cancel substitute teachers", CAT_ACADEMIC),
			def(TEACHER_MANAGE, "Manage teacher records", CAT_HR),
			def(TERM_READ, "View academic terms", CAT_ACADEMIC),
			def(TERM_WRITE, "Create academic terms", CAT_ACADEMIC),
			def(TICKET_MANAGE, "View and resolve helpdesk tickets", CAT_TICKET),
			def(TICKET_SELF_SERVICE, "Raise and view own helpdesk tickets", CAT_TICKET),
			def(TIMETABLE_READ, "View the timetable", CAT_ACADEMIC),
			def(TIMETABLE_WRITE, "Manage the timetable", CAT_ACADEMIC),
			def(TRANSPORT_ASSIGNMENT_READ, "View student transport assignments", CAT_TRANSPORT),
			def(TRANSPORT_ASSIGNMENT_WRITE, "Manage student transport assignments", CAT_TRANSPORT),
			def(TRANSPORT_ROUTE_READ, "View transport routes", CAT_TRANSPORT),
			def(TRANSPORT_ROUTE_WRITE, "Manage transport routes", CAT_TRANSPORT),
			def(VEHICLE_READ, "View transport vehicles", CAT_TRANSPORT),
			def(VEHICLE_WRITE, "Manage transport vehicles", CAT_TRANSPORT),
			def(VENUE_MANAGE, "Manage teaching venues", CAT_ACADEMIC),
			def(VISITOR_LOG_MANAGE, "Check visitors in and out", CAT_FACILITIES),
			def(VISITOR_REQUEST_APPROVE, "Approve or reject any visitor request", CAT_FACILITIES),
			def(VISITOR_REQUEST_SELF_SERVICE, "Pre-register visitors and decide on the visits you host",
					CAT_FACILITIES));

	private static PermissionDefinition def(String code, String description, String category) {
		return new PermissionDefinition(code, description, category);
	}

	private SchoolPermissions() {
	}
}
