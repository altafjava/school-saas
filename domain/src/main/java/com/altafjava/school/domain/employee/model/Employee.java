package com.altafjava.school.domain.employee.model;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import com.altafjava.school.domain.common.model.Address;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Anyone the school employs — the HR master record that leave, payroll, departments and staff ID
 * cards hang off. {@link com.altafjava.school.domain.teacher.model.Teacher} is a subtype (JOINED
 * inheritance: same id, same public id) for teaching staff, so classrooms, timetables and lessons
 * can only ever reference a real teacher, while a driver, clerk or counsellor is a plain
 * {@code Employee} with the same HR capabilities.
 */
@Entity
@Table(name = "employees")
@Inheritance(strategy = InheritanceType.JOINED)
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Employee extends SoftDeletableEntity {

	// FK to platform users.id — nullable, set only once this person has a login account.
	@Column(name = "user_id")
	private Long userId;

	// FK to departments.id — nullable, HR details are assigned after hiring, not at hire time.
	@Column(name = "department_id")
	private Long departmentId;

	// FK to platform file_metadata.public_id — see Student.photoFilePublicId's Javadoc for why the
	// UUID publicId, not the internal surrogate id.
	@Column(name = "photo_file_public_id")
	private UUID photoFilePublicId;

	@Column(name = "employee_code", nullable = false, length = 50)
	private String employeeCode;

	@Pii
	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Pii
	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Pii
	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Pii(type = Pii.PiiType.PHONE)
	@Column(name = "phone", length = 30)
	private String phone;

	@Embedded
	private Address address;

	@Column(name = "join_date")
	private LocalDate joinDate;

	@Column(name = "designation", length = 100)
	private String designation;

	@Column(name = "qualification", length = 255)
	private String qualification;

	@Enumerated(EnumType.STRING)
	@Column(name = "staff_category", nullable = false, length = 20)
	private StaffCategory staffCategory;

	// Nullable — set only when the school places this person on probation; on/after this date they
	// are no longer on probation, and null means never (or already ended).
	@Column(name = "probation_end_date")
	private LocalDate probationEndDate;

	@Column(name = "exit_date")
	private LocalDate exitDate;

	@Column(name = "exit_reason", length = 500)
	private String exitReason;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", length = 30)
	private EmploymentType employmentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private EmployeeStatus status;

	/** Non-teaching staff; teachers are created with {@code Teacher.create}. */
	public static Employee create(StaffCategory staffCategory, String employeeCode, String firstName,
			String lastName, String email, LocalDate joinDate) {
		if (staffCategory == null || staffCategory == StaffCategory.TEACHING) {
			throw new BusinessException("Teaching staff are hired as teachers; use a non-teaching category here");
		}
		return Employee.builder()
				.staffCategory(staffCategory)
				.status(EmployeeStatus.ACTIVE)
				.employeeCode(employeeCode)
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.joinDate(joinDate)
				.build();
	}

	public boolean isActive() {
		return status == EmployeeStatus.ACTIVE;
	}

	/**
	 * Records that this person has left. Exit is final — the record stays for payroll history and
	 * statutory retention; rehiring is a new hire, not a reversal.
	 */
	public void exit(EmployeeStatus exitStatus, LocalDate exitDate, String reason) {
		if (exitStatus == null || !exitStatus.isExit()) {
			throw new BusinessException("An exit must be RESIGNED, TERMINATED or RETIRED");
		}
		if (!isActive()) {
			throw new BusinessException("Employee has already left the school: " + this.status);
		}
		if (exitDate == null || exitDate.isAfter(LocalDate.now())) {
			throw new BusinessException("The exit date must be today or earlier");
		}
		if (joinDate != null && exitDate.isBefore(joinDate)) {
			throw new BusinessException("The exit date cannot be before the join date");
		}
		this.status = exitStatus;
		this.exitDate = exitDate;
		this.exitReason = reason;
	}

	public void updateContactDetails(String firstName, String lastName, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	public void assignHrDetails(Long departmentId, String designation, String qualification,
			EmploymentType employmentType) {
		this.departmentId = departmentId;
		this.designation = designation;
		this.qualification = qualification;
		this.employmentType = employmentType;
	}

	// Caller (EmployeeService) validates the phone against PhoneNumberValidator first — this
	// method just persists an already-validated value.
	public void updatePhone(String phone) {
		this.phone = phone;
	}

	public void updateAddress(Address address) {
		this.address = Address.copyOf(address);
	}

	public void setProbationPeriod(LocalDate probationEndDate) {
		this.probationEndDate = probationEndDate;
	}

	public void endProbation() {
		this.probationEndDate = null;
	}

	public boolean isOnProbation(LocalDate asOf) {
		return probationEndDate != null && asOf.isBefore(probationEndDate);
	}

	public void updatePhoto(UUID photoFilePublicId) {
		this.photoFilePublicId = photoFilePublicId;
	}

	// GDPR/DPDP erasure (see DomainPiiHandler) — NOT NULL columns get an opaque placeholder,
	// everything else PII-bearing is cleared. employeeCode stays: an operational identifier.
	public void erasePii() {
		this.firstName = "[erased]";
		this.lastName = "[erased]";
		this.email = "[erased]";
		this.phone = null;
		this.address = null;
		this.photoFilePublicId = null;
	}
}
