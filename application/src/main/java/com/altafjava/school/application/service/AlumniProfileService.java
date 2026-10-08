package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.lifecycle.LifecycleRecorder;
import com.altafjava.school.domain.alumni.model.AlumniProfile;
import com.altafjava.school.domain.alumni.repository.AlumniProfileRepository;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class AlumniProfileService {

	private final AlumniProfileRepository alumniProfileRepository;
	private final StudentRepository studentRepository;
	private final LifecycleRecorder lifecycleRecorder;

	public AlumniProfileService(AlumniProfileRepository alumniProfileRepository,
			StudentRepository studentRepository, LifecycleRecorder lifecycleRecorder) {
		this.lifecycleRecorder = lifecycleRecorder;
		this.alumniProfileRepository = alumniProfileRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public Page<AlumniProfile> list(Pageable pageable) {
		return alumniProfileRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public AlumniProfile findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return alumniProfileRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Alumni profile not found: " + publicId));
	}

	@Transactional
	public AlumniProfile create(String studentPublicId, int graduationYear, String currentOccupation,
			String contactEmail, String contactPhone) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		if (student.getEnrollmentStatus() != EnrollmentStatus.GRADUATED) {
			throw new BusinessException(
					"Cannot create an alumni profile for a student who has not graduated: " + studentPublicId);
		}
		if (alumniProfileRepository.existsByStudentIdAndTenantId(student.getId(), tenantId)) {
			throw new BusinessException("Alumni profile already exists for student: " + studentPublicId);
		}

		AlumniProfile profile = AlumniProfile.create(student.getId(), graduationYear, currentOccupation,
				contactEmail, contactPhone);
		AlumniProfile saved = alumniProfileRepository.save(profile);
		lifecycleRecorder.alumnus(student.getId(), LifecycleChange.NONE);
		return saved;
	}

	@Transactional
	public AlumniProfile updateContactInfo(String publicId, String currentOccupation, String contactEmail,
			String contactPhone) {
		AlumniProfile profile = findByPublicId(publicId);
		profile.updateContactInfo(currentOccupation, contactEmail, contactPhone);
		return alumniProfileRepository.save(profile);
	}

	@Transactional
	public AlumniProfile activate(String publicId) {
		AlumniProfile profile = findByPublicId(publicId);
		profile.activate();
		return alumniProfileRepository.save(profile);
	}

	@Transactional
	public AlumniProfile deactivate(String publicId) {
		AlumniProfile profile = findByPublicId(publicId);
		profile.deactivate();
		return alumniProfileRepository.save(profile);
	}
}
