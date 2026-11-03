package com.altafjava.school.domain.enquiry.model;

import java.time.Instant;
import java.time.LocalDate;
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

/**
 * A family's interest in a place, before any application. Staff follow it up until it is converted
 * into an admission application or closed; {@link EnquiryStatus} holds the allowed moves.
 */
@Entity
@Table(name = "enquiries")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class Enquiry extends SoftDeletableEntity {

	private static final String ERASED = "erased";

	// FK to employees.id — the staff member following it up; optional.
	@Column(name = "assigned_employee_id")
	private Long assignedEmployeeId;

	// FK to admissions.id — set once converted.
	@Column(name = "admission_id")
	private Long admissionId;

	@Pii
	@Column(name = "child_first_name", nullable = false, length = 100)
	private String childFirstName;

	@Pii
	@Column(name = "child_last_name", nullable = false, length = 100)
	private String childLastName;

	@Column(name = "child_date_of_birth")
	private LocalDate childDateOfBirth;

	@Column(name = "grade_sought", nullable = false, length = 20)
	private String gradeSought;

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

	@Column(name = "notes", length = 2000)
	private String notes;

	@Column(name = "next_follow_up_on")
	private LocalDate nextFollowUpOn;

	@Column(name = "closed_at")
	private Instant closedAt;

	@Column(name = "close_reason", length = 500)
	private String closeReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 30)
	private EnquirySource source;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EnquiryStatus status;

	public static Enquiry receive(EnquiryDetails details, EnquirySource source, String notes) {
		if (details.guardianEmail() == null && details.guardianPhone() == null) {
			throw new BusinessException("An enquiry needs a phone number or an email address to follow up");
		}
		return Enquiry.builder()
				.childFirstName(details.childFirstName())
				.childLastName(details.childLastName())
				.childDateOfBirth(details.childDateOfBirth())
				.gradeSought(details.gradeSought())
				.guardianFirstName(details.guardianFirstName())
				.guardianLastName(details.guardianLastName())
				.guardianEmail(details.guardianEmail())
				.guardianPhone(details.guardianPhone())
				.source(source)
				.notes(notes)
				.status(EnquiryStatus.NEW)
				.build();
	}

	public void assignTo(Long employeeId) {
		requireOpen("reassign");
		this.assignedEmployeeId = employeeId;
	}

	/** Following up takes a new enquiry into progress; the next date is optional. */
	public void recordFollowUp(LocalDate nextFollowUpOn) {
		requireOpen("follow up");
		if (status == EnquiryStatus.NEW) {
			moveTo(EnquiryStatus.IN_PROGRESS);
		}
		this.nextFollowUpOn = nextFollowUpOn;
	}

	public void close(String reason) {
		moveTo(EnquiryStatus.CLOSED);
		this.closedAt = Instant.now();
		this.closeReason = reason;
		this.nextFollowUpOn = null;
	}

	public void reopen() {
		moveTo(EnquiryStatus.IN_PROGRESS);
		this.closedAt = null;
		this.closeReason = null;
	}

	public void convertedInto(Long admissionId) {
		moveTo(EnquiryStatus.CONVERTED);
		this.admissionId = admissionId;
		this.nextFollowUpOn = null;
	}

	/** Leaves the record for the school's numbers while removing who the family was. */
	public void erasePii() {
		this.childFirstName = ERASED;
		this.childLastName = ERASED;
		this.childDateOfBirth = null;
		this.guardianFirstName = ERASED;
		this.guardianLastName = ERASED;
		this.guardianEmail = null;
		this.guardianPhone = null;
		this.notes = null;
	}

	private void moveTo(EnquiryStatus next) {
		if (!status.canMoveTo(next)) {
			throw new BusinessException("An enquiry that is " + status.name().toLowerCase().replace('_', ' ')
					+ " cannot become " + next.name().toLowerCase().replace('_', ' '));
		}
		this.status = next;
	}

	private void requireOpen(String action) {
		if (!status.isOpen()) {
			throw new BusinessException("Cannot " + action + " an enquiry that is " + status.name().toLowerCase());
		}
	}
}
