package com.altafjava.school.application.document;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.altafjava.platform.application.document.DefaultDocumentTemplate;
import com.altafjava.platform.application.document.DocumentTypeDefinition;
import com.altafjava.platform.domain.document.model.DocumentTemplateFormat;
import com.altafjava.platform.domain.document.model.PlaceholderField;
import com.altafjava.school.domain.certificate.model.CertificateType;

/**
 * Every document school-saas issues through the platform Document Template Engine: the fields
 * each one supplies (what tenant template designers can use) and the built-in default design. The
 * engine adds its system fields (tenant name/logo, title, issue date, verification code/QR).
 */
@Configuration
public class SchoolDocumentTypes {

	public static final String STUDENT_ID_CARD = "STUDENT_ID_CARD";
	public static final String STAFF_ID_CARD = "STAFF_ID_CARD";
	public static final String CERTIFICATE = CertificateType.DOCUMENT_TYPE_FAMILY;
	public static final String REPORT_CARD = "REPORT_CARD";
	public static final String ADMISSION_OFFER_LETTER = "ADMISSION_OFFER_LETTER";
	public static final String VISITOR_BADGE = "VISITOR_BADGE";

	public static final String OWNER_STUDENT = "STUDENT";
	public static final String OWNER_EMPLOYEE = "EMPLOYEE";
	public static final String OWNER_ADMISSION = "ADMISSION";
	public static final String OWNER_VISITOR_REQUEST = "VISITOR_REQUEST";

	@Bean
	DocumentTypeDefinition studentIdCardDocumentType() {
		return new DocumentTypeDefinition(STUDENT_ID_CARD, "Student ID card", List.of(
				PlaceholderField.text("studentName", "Student name"),
				PlaceholderField.text("rollNumberOrAdmissionNumber", "Roll no. (falls back to admission no.)"),
				PlaceholderField.text("className", "Class"),
				PlaceholderField.image("photo", "Photo")),
				DefaultDocumentTemplate.classpath(DocumentTemplateFormat.SVG,
						"document-templates/student-id-card.svg"));
	}

	@Bean
	DocumentTypeDefinition staffIdCardDocumentType() {
		return new DocumentTypeDefinition(STAFF_ID_CARD, "Staff ID card", List.of(
				PlaceholderField.text("employeeName", "Name"),
				PlaceholderField.text("employeeCode", "Employee code"),
				PlaceholderField.text("department", "Department"),
				PlaceholderField.text("designation", "Designation"),
				PlaceholderField.image("photo", "Photo")),
				DefaultDocumentTemplate.classpath(DocumentTemplateFormat.SVG, "document-templates/staff-id-card.svg"));
	}

	// One registration serves every CERTIFICATE.<code> type a tenant defines.
	@Bean
	DocumentTypeDefinition certificateDocumentType() {
		return new DocumentTypeDefinition(CERTIFICATE, "Certificate", List.of(
				PlaceholderField.text("body", "Certificate wording, resolved for the student"),
				PlaceholderField.text("studentName", "Student name"),
				PlaceholderField.text("studentCode", "Admission no."),
				PlaceholderField.text("className", "Class"),
				PlaceholderField.text("academicYear", "Academic year"),
				PlaceholderField.text("admissionDate", "Admission date")),
				DefaultDocumentTemplate.classpath(DocumentTemplateFormat.HTML, "document-templates/certificate.html"));
	}

	@Bean
	DocumentTypeDefinition admissionOfferLetterDocumentType() {
		return new DocumentTypeDefinition(ADMISSION_OFFER_LETTER, "Admission offer letter", List.of(
				PlaceholderField.text("applicantName", "Applicant name"),
				PlaceholderField.text("guardianName", "Guardian name"),
				PlaceholderField.text("appliedGrade", "Grade offered"),
				PlaceholderField.text("applicationReference", "Application reference"),
				PlaceholderField.text("offerDate", "Offer date"),
				PlaceholderField.flag("hasFeeReceipt", "Application fee was paid"),
				PlaceholderField.text("feeReceiptNumber", "Application fee receipt number")),
				DefaultDocumentTemplate.classpath(DocumentTemplateFormat.HTML,
						"document-templates/admission-offer-letter.html"));
	}

	@Bean
	DocumentTypeDefinition visitorBadgeDocumentType() {
		return new DocumentTypeDefinition(VISITOR_BADGE, "Visitor badge", List.of(
				PlaceholderField.text("visitorName", "Visitor name"),
				PlaceholderField.text("hostName", "Person being visited"),
				PlaceholderField.text("purpose", "Purpose of the visit"),
				PlaceholderField.text("validOn", "Date the badge is valid"),
				PlaceholderField.image("photo", "Photo")),
				DefaultDocumentTemplate.classpath(DocumentTemplateFormat.SVG, "document-templates/visitor-badge.svg"));
	}

	@Bean
	DocumentTypeDefinition reportCardDocumentType() {
		return new DocumentTypeDefinition(REPORT_CARD, "Report card", List.of(
				PlaceholderField.text("studentName", "Student name"),
				PlaceholderField.text("studentCode", "Admission no."),
				PlaceholderField.text("termName", "Term"),
				PlaceholderField.text("grade", "Grade"),
				PlaceholderField.text("section", "Section"),
				PlaceholderField.list("lines", "Results", List.of(
						PlaceholderField.text("subject", "Subject"),
						PlaceholderField.text("exam", "Exam"),
						PlaceholderField.text("marks", "Marks"),
						PlaceholderField.text("maxMarks", "Maximum marks"),
						PlaceholderField.text("weightage", "Exam weightage (percent)"),
						PlaceholderField.text("gradeLetter", "Grade letter"))),
				PlaceholderField.flag("hasTotals", "Has totals"),
				PlaceholderField.text("totalMarks", "Total marks"),
				PlaceholderField.text("totalMaxMarks", "Total maximum marks"),
				PlaceholderField.text("percentage", "Overall percentage, weighted by exam weightage"),
				PlaceholderField.flag("showAttendance", "Show attendance"),
				PlaceholderField.text("attendanceSummary", "Attendance summary"),
				PlaceholderField.flag("showRank", "Show rank"),
				PlaceholderField.text("rank", "Class rank"),
				PlaceholderField.flag("showCompetencies", "Show competencies"),
				PlaceholderField.list("competencies", "Competencies", List.of(
						PlaceholderField.text("label", "Competency"),
						PlaceholderField.text("value", "Assessment"))),
				PlaceholderField.flag("showRemarks", "Show remarks"),
				PlaceholderField.text("teacherRemarks", "Teacher's remarks"),
				PlaceholderField.text("principalRemarks", "Principal's remarks"),
				PlaceholderField.text("labelGrade", "Label: Grade"),
				PlaceholderField.text("labelSection", "Label: Section"),
				PlaceholderField.text("labelAttendance", "Label: Attendance"),
				PlaceholderField.text("labelRank", "Label: Rank"),
				PlaceholderField.text("labelCompetencies", "Label: Competencies"),
				PlaceholderField.text("labelTeacherRemarks", "Label: Teacher's remarks"),
				PlaceholderField.text("labelPrincipalRemarks", "Label: Principal's remarks"),
				PlaceholderField.text("labelClassTeacher", "Label: Class teacher"),
				PlaceholderField.text("labelPrincipal", "Label: Principal")),
				DefaultDocumentTemplate.classpath(DocumentTemplateFormat.HTML, "document-templates/report-card.html"));
	}
}
