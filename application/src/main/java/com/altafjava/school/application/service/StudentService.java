package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.lifecycle.LifecycleRecorder;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.common.service.PhoneNumberValidator;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class StudentService {

	private static final String STUDENT_CODE_SEQUENCE = "STUDENT_CODE";

	private final StudentRepository studentRepository;
	private final NumberSequenceService numberSequenceService;
	private final LifecycleRecorder lifecycleRecorder;
	private final PublicIdLookup publicIdLookup;
	private final PhoneNumberValidator phoneNumberValidator = new PhoneNumberValidator();

	public StudentService(StudentRepository studentRepository, NumberSequenceService numberSequenceService,
			LifecycleRecorder lifecycleRecorder, PublicIdLookup publicIdLookup) {
		this.publicIdLookup = publicIdLookup;
		this.studentRepository = studentRepository;
		this.numberSequenceService = numberSequenceService;
		this.lifecycleRecorder = lifecycleRecorder;
	}

	/**
	 * Every filter is optional: a status, the classroom a student is placed in, and a free-text {@code q}
	 * (blank = no filter) over the entity's identifying fields.
	 */
	@Transactional(readOnly = true)
	public Page<Student> searchStudents(Pageable pageable, EnrollmentStatus status, String classroomPublicId,
			String q) {
		Long classroomId = publicIdLookup.idOrNull(EntityRef.CLASSROOM, classroomPublicId);
		return studentRepository.search(TenantContext.getCurrentTenantId(), status, classroomId,
				LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Student> listStudents(Pageable pageable) {
		return studentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Student> listStudents(Pageable pageable, EnrollmentStatus status) {
		if (status == null) {
			return listStudents(pageable);
		}
		return studentRepository.findAllByTenantIdAndEnrollmentStatus(TenantContext.getCurrentTenantId(), status,
				pageable);
	}

	@Transactional(readOnly = true)
	public Student findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + publicId));
	}

	@Transactional
	public Student enroll(String studentCode, String firstName, String lastName,
			String email, LocalDate dateOfBirth) {
		return enroll(studentCode, firstName, lastName, email, dateOfBirth, Gender.NOT_SPECIFIED, null);
	}

	/** {@code sourceAdmissionId} is set when the student comes from an approved admission. */
	@Transactional
	public Student enroll(String studentCode, String firstName, String lastName,
			String email, LocalDate dateOfBirth, Long sourceAdmissionId) {
		return enroll(studentCode, firstName, lastName, email, dateOfBirth, Gender.NOT_SPECIFIED,
				sourceAdmissionId);
	}

	@Transactional
	public Student enroll(String studentCode, String firstName, String lastName,
			String email, LocalDate dateOfBirth, Gender gender, Long sourceAdmissionId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		String resolvedCode = resolveStudentCode(tenantId, studentCode);
		if (studentRepository.existsByStudentCodeAndTenantId(resolvedCode, tenantId)) {
			throw new BusinessException("Student code already exists: " + resolvedCode);
		}
		Student student = studentRepository
				.save(Student.create(resolvedCode, firstName, lastName, email, dateOfBirth, gender));
		if (sourceAdmissionId != null) {
			lifecycleRecorder.enrolledFromAdmission(sourceAdmissionId, student.getId(), LifecycleChange.NONE);
		} else {
			lifecycleRecorder.student(student.getId(), null, EnrollmentStatus.ACTIVE, LifecycleChange.NONE);
		}
		return student;
	}

	// A caller-supplied studentCode is an explicit override; omitting it defers to the tenant's
	// configured numbering sequence (prefix/width/reset period), defaulting to "STU-0001" style.
	private String resolveStudentCode(Long tenantId, String studentCode) {
		if (studentCode != null && !studentCode.isBlank()) {
			return studentCode;
		}
		return numberSequenceService.generateNext(tenantId, STUDENT_CODE_SEQUENCE, "STU-", 4, ResetPeriod.NEVER);
	}

	@Transactional
	public Student withdraw(String publicId, LifecycleChange change) {
		return changeStatus(publicId, change, Student::withdraw);
	}

	@Transactional
	public Student transfer(String publicId, LifecycleChange change) {
		return changeStatus(publicId, change, Student::transfer);
	}

	@Transactional
	public Student graduate(String publicId, LifecycleChange change) {
		return changeStatus(publicId, change, Student::graduate);
	}

	@Transactional
	public Student suspend(String publicId, LifecycleChange change) {
		return changeStatus(publicId, change, Student::suspend);
	}

	@Transactional
	public Student reinstate(String publicId, LifecycleChange change) {
		return changeStatus(publicId, change, Student::reinstate);
	}

	private Student changeStatus(String publicId, LifecycleChange change, Consumer<Student> transition) {
		Student student = findByPublicId(publicId);
		EnrollmentStatus before = student.getEnrollmentStatus();
		transition.accept(student);
		Student saved = studentRepository.save(student);
		lifecycleRecorder.student(saved.getId(), before, saved.getEnrollmentStatus(), change);
		return saved;
	}

	@Transactional
	public Student updateContactDetails(String publicId, String firstName, String lastName, String email,
			LocalDate dateOfBirth, Gender gender, ExpectedVersion expectedVersion) {
		Student student = findByPublicId(publicId);
		expectedVersion.verify(student);
		student.updateContactDetails(firstName, lastName, email, dateOfBirth, gender);
		return studentRepository.save(student);
	}

	@Transactional
	public Student updatePhone(String publicId, String phone, ExpectedVersion expectedVersion) {
		Student student = findByPublicId(publicId);
		expectedVersion.verify(student);
		String defaultRegion = student.getAddress() != null ? student.getAddress().getCountryCode() : null;
		if (!phoneNumberValidator.isValid(phone, defaultRegion)) {
			throw new BusinessException("Invalid phone number: " + phone);
		}
		student.updatePhone(phone);
		return studentRepository.save(student);
	}

	@Transactional
	public Student updateAddress(String publicId, Address address, ExpectedVersion expectedVersion) {
		Student student = findByPublicId(publicId);
		expectedVersion.verify(student);
		student.updateAddress(address);
		return studentRepository.save(student);
	}

	@Transactional
	public Student updatePhoto(String publicId, String filePublicId, ExpectedVersion expectedVersion) {
		Student student = findByPublicId(publicId);
		expectedVersion.verify(student);
		student.updatePhoto(Optional.ofNullable(filePublicId).map(UUID::fromString).orElse(null));
		return studentRepository.save(student);
	}
}
