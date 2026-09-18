package com.altafjava.school.domain.admission.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "admissions")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Admission extends SoftDeletableEntity {

	@Pii
	@Column(name = "applicant_first_name", nullable = false, length = 100)
	private String applicantFirstName;

	@Pii
	@Column(name = "applicant_last_name", nullable = false, length = 100)
	private String applicantLastName;

	@Column(name = "applicant_date_of_birth")
	private LocalDate applicantDateOfBirth;

	@Pii
	@Column(name = "guardian_first_name", nullable = false, length = 100)
	private String guardianFirstName;

	@Pii
	@Column(name = "guardian_last_name", nullable = false, length = 100)
	private String guardianLastName;

	@Pii
	@Column(name = "guardian_email", length = 255)
	private String guardianEmail;

	@Pii
	@Column(name = "guardian_phone", length = 30)
	private String guardianPhone;

	@Column(name = "applied_grade", nullable = false, length = 20)
	private String appliedGrade;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AdmissionStatus status;

	@Column(name = "submitted_at", nullable = false)
	private Instant submittedAt;

	// Correlates to the SagaLog row of the enrollment saga (see AdmissionEnrollmentSaga) —
	// lets compensateStep() find this admission from just the sagaId it's handed, without
	// re-encoding the tenant ID the way TenantProvisioningSaga does.
	@Column(name = "enrollment_saga_id")
	private UUID enrollmentSagaId;

	// FK to students.id — set as soon as the ENROLL_STUDENT step creates the record, not only
	// once the whole saga completes, so a later step's compensation can still find and undo it.
	@Column(name = "enrolled_student_id")
	private Long enrolledStudentId;

	// FK to guardians.id — set as soon as the LINK_GUARDIAN step creates the record, same reason.
	@Column(name = "enrolled_guardian_id")
	private Long enrolledGuardianId;

	@Column(name = "entrance_test_score", precision = 10, scale = 2)
	private BigDecimal entranceTestScore;

	@Column(name = "entrance_test_max_score", precision = 10, scale = 2)
	private BigDecimal entranceTestMaxScore;

	@Column(name = "merit_rank")
	private Integer meritRank;

	public static Admission submit(String applicantFirstName, String applicantLastName,
			LocalDate applicantDateOfBirth, String guardianFirstName, String guardianLastName,
			String guardianEmail, String guardianPhone, String appliedGrade) {
		return Admission.builder()
				.applicantFirstName(applicantFirstName)
				.applicantLastName(applicantLastName)
				.applicantDateOfBirth(applicantDateOfBirth)
				.guardianFirstName(guardianFirstName)
				.guardianLastName(guardianLastName)
				.guardianEmail(guardianEmail)
				.guardianPhone(guardianPhone)
				.appliedGrade(appliedGrade)
				.status(AdmissionStatus.SUBMITTED)
				.submittedAt(Instant.now())
				.build();
	}

	public void markUnderReview() {
		if (this.status != AdmissionStatus.SUBMITTED) {
			throw new BusinessException(
					"Admission must be SUBMITTED to move under review, was " + this.status);
		}
		this.status = AdmissionStatus.UNDER_REVIEW;
	}

	public void approve() {
		requireDecidable();
		this.status = AdmissionStatus.APPROVED;
	}

	public void reject() {
		requireDecidable();
		this.status = AdmissionStatus.REJECTED;
	}

	/** SUBMITTED and UNDER_REVIEW are the only states without a final decision recorded yet. */
	private void requireDecidable() {
		if (this.status != AdmissionStatus.SUBMITTED && this.status != AdmissionStatus.UNDER_REVIEW) {
			throw new BusinessException("Admission already has a final decision, status=" + this.status);
		}
	}

	public void beginEnrollmentSaga(UUID sagaId) {
		this.enrollmentSagaId = sagaId;
	}

	public void recordEnrolledStudent(Long studentId) {
		this.enrolledStudentId = studentId;
	}

	public void recordEnrolledGuardian(Long guardianId) {
		this.enrolledGuardianId = guardianId;
	}

	public void markEnrolled() {
		if (this.status != AdmissionStatus.APPROVED) {
			throw new BusinessException("Admission must be APPROVED to enroll, was " + this.status);
		}
		this.status = AdmissionStatus.ENROLLED;
	}

	// Saga compensation: undo everything recorded since APPROVED when a later step fails.
	public void revertEnrollment() {
		this.status = AdmissionStatus.APPROVED;
		this.enrolledStudentId = null;
		this.enrolledGuardianId = null;
		this.enrollmentSagaId = null;
	}

	public void recordEntranceTestScore(BigDecimal score, BigDecimal maxScore) {
		if (this.status != AdmissionStatus.UNDER_REVIEW) {
			throw new BusinessException("Entrance test score can only be recorded while UNDER_REVIEW, was "
					+ this.status);
		}
		this.entranceTestScore = score;
		this.entranceTestMaxScore = maxScore;
	}

	public void assignMeritRank(int rank) {
		if (rank < 1) {
			throw new BusinessException("Merit rank must be a positive integer, was " + rank);
		}
		this.meritRank = rank;
	}

	public void waitlist() {
		if (this.status != AdmissionStatus.UNDER_REVIEW) {
			throw new BusinessException("Cannot waitlist an admission that is not UNDER_REVIEW, was " + this.status);
		}
		this.status = AdmissionStatus.WAITLISTED;
	}

	public void promoteFromWaitlist() {
		if (this.status != AdmissionStatus.WAITLISTED) {
			throw new BusinessException("Cannot promote an admission that is not WAITLISTED, was " + this.status);
		}
		this.status = AdmissionStatus.UNDER_REVIEW;
	}
}
