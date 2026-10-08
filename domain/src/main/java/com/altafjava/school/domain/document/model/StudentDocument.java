package com.altafjava.school.domain.document.model;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A single uploaded document (birth certificate, transfer certificate, ID proof, ...) linked to
 * either a pre-conversion {@code Admission} applicant or an already-enrolled {@code Student} —
 * built once for both admission-time uploads and later student-record documents, per the Phase 1
 * plan. School-specific (birth certificate, transfer certificate are school concepts, not
 * platform-generic ones), so this lives here, not in platform-saas — contrast with the Document
 * Template Engine (SVG rendering) itself, which genuinely is platform-generic and lives there.
 *
 * <p>
 * {@code documentType} is an open {@code String}, not a closed enum — a tenant's required document
 * set varies (Aadhar card, passport, birth certificate, ...), matching the same genericity lesson
 * applied elsewhere ({@code AlertRule.ruleType}, {@code CertificateTemplate}). Exactly one of
 * {@code studentId}/{@code admissionId} is set, enforced by the factory methods.
 */
@Entity
@Table(name = "student_documents")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class StudentDocument extends SoftDeletableEntity {

	// FK to students.id — set when uploaded against an already-enrolled student's record.
	@Column(name = "student_id")
	private Long studentId;

	// FK to admissions.id — set when uploaded during the admissions process, before enrollment.
	@Column(name = "admission_id")
	private Long admissionId;

	// FK to platform file_metadata.public_id — see Student.photoFilePublicId's Javadoc for why the
	// UUID publicId, not the internal surrogate id.
	@Column(name = "file_public_id", nullable = false)
	private UUID filePublicId;

	// FK to platform users.id — the staff member who verified/rejected this document.
	@Column(name = "verified_by_user_id")
	private Long verifiedByUserId;

	@Column(name = "verified_at")
	private Instant verifiedAt;

	@Column(name = "rejection_reason", length = 500)
	private String rejectionReason;

	@Column(name = "document_type", nullable = false, length = 100)
	private String documentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "verification_status", nullable = false, length = 20)
	private DocumentVerificationStatus verificationStatus;

	public static StudentDocument forStudent(Long studentId, String documentType, UUID filePublicId) {
		if (studentId == null) {
			throw new BusinessException("studentId is required");
		}
		return create(studentId, null, documentType, filePublicId);
	}

	public static StudentDocument forAdmission(Long admissionId, String documentType, UUID filePublicId) {
		if (admissionId == null) {
			throw new BusinessException("admissionId is required");
		}
		return create(null, admissionId, documentType, filePublicId);
	}

	private static StudentDocument create(Long studentId, Long admissionId, String documentType,
			UUID filePublicId) {
		return StudentDocument.builder()
				.studentId(studentId)
				.admissionId(admissionId)
				.documentType(documentType)
				.filePublicId(filePublicId)
				.verificationStatus(DocumentVerificationStatus.PENDING)
				.build();
	}

	public void verify(Long verifiedByUserId) {
		requirePending();
		this.verificationStatus = DocumentVerificationStatus.VERIFIED;
		this.verifiedByUserId = verifiedByUserId;
		this.verifiedAt = Instant.now();
		this.rejectionReason = null;
	}

	public void reject(Long verifiedByUserId, String rejectionReason) {
		requirePending();
		if (rejectionReason == null || rejectionReason.isBlank()) {
			throw new BusinessException("A rejection reason is required");
		}
		this.verificationStatus = DocumentVerificationStatus.REJECTED;
		this.verifiedByUserId = verifiedByUserId;
		this.verifiedAt = Instant.now();
		this.rejectionReason = rejectionReason;
	}

	private void requirePending() {
		if (this.verificationStatus != DocumentVerificationStatus.PENDING) {
			throw new BusinessException(
					"Document has already been " + this.verificationStatus.name().toLowerCase()
							+ " — resubmit a new document instead of re-verifying");
		}
	}
}
