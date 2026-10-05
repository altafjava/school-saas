package com.altafjava.school.domain.teacher.model;

import java.time.LocalDate;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import com.altafjava.school.domain.common.model.Address;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "teachers")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Teacher extends SoftDeletableEntity {

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

	@Column(name = "join_date")
	private LocalDate joinDate;

	// FK to platform users.id — nullable, set only once this teacher has a login account.
	@Column(name = "user_id")
	private Long userId;

	// FK to departments.id — nullable, HR details are assigned after hiring, not at hire time.
	@Column(name = "department_id")
	private Long departmentId;

	@Column(name = "qualification", length = 255)
	private String qualification;

	@Enumerated(EnumType.STRING)
	@Column(name = "employment_type", length = 30)
	private EmploymentType employmentType;

	@Pii(type = Pii.PiiType.PHONE)
	@Column(name = "phone", length = 30)
	private String phone;

	@Embedded
	private Address address;

	// Nullable — set only when the tenant places this teacher on probation. On/after this date the
	// teacher is no longer on probation; null means never on probation (or probation already ended
	// without a tracked date, for records created before this field existed).
	@Column(name = "probation_end_date")
	private LocalDate probationEndDate;

	// FK to platform file_metadata.public_id — see Student.photoFilePublicId's Javadoc for why the
	// UUID publicId, not the internal surrogate id.
	@Column(name = "photo_file_public_id")
	private UUID photoFilePublicId;

	public static Teacher create(String employeeCode, String firstName, String lastName,
			String email, LocalDate joinDate) {
		return Teacher.builder()
				.employeeCode(employeeCode)
				.firstName(firstName)
				.lastName(lastName)
				.email(email)
				.joinDate(joinDate)
				.build();
	}

	public void updateContactDetails(String firstName, String lastName, String email) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
	}

	public void assignHrDetails(Long departmentId, String qualification, EmploymentType employmentType) {
		this.departmentId = departmentId;
		this.qualification = qualification;
		this.employmentType = employmentType;
	}

	// Caller (TeacherService) validates the phone against PhoneNumberValidator first — this
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
}
