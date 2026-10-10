package com.altafjava.school.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.event.publisher.EventPublisher;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.UserReferenceResolver;
import com.altafjava.school.domain.common.model.Address;
import com.altafjava.school.domain.common.service.PhoneNumberValidator;
import com.altafjava.school.domain.guardian.event.GuardianLinkedEvent;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.RelationshipType;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class GuardianService {

	private final GuardianRepository guardianRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final StudentRepository studentRepository;
	private final EventPublisher eventPublisher;
	private final UserReferenceResolver userReferenceResolver;
	private final PhoneNumberValidator phoneNumberValidator = new PhoneNumberValidator();

	public GuardianService(GuardianRepository guardianRepository,
			StudentGuardianLinkRepository studentGuardianLinkRepository, StudentRepository studentRepository,
			EventPublisher eventPublisher, UserReferenceResolver userReferenceResolver) {
		this.guardianRepository = guardianRepository;
		this.userReferenceResolver = userReferenceResolver;
		this.studentGuardianLinkRepository = studentGuardianLinkRepository;
		this.studentRepository = studentRepository;
		this.eventPublisher = eventPublisher;
	}

	/** Free-text {@code q} (blank = no filter) over the entity's identifying fields. */
	@Transactional(readOnly = true)
	public Page<Guardian> searchGuardians(Pageable pageable, String q) {
		return guardianRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Guardian> listGuardians(Pageable pageable) {
		return guardianRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Guardian findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return guardianRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Guardian not found: " + publicId));
	}

	@Transactional
	public Guardian create(String firstName, String lastName, String email, String phone, String userPublicId) {
		if (!phoneNumberValidator.isValid(phone, null)) {
			throw new BusinessException("Invalid phone number: " + phone);
		}
		Long userId = userPublicId == null ? null : userReferenceResolver.requireUserId(userPublicId);
		Guardian guardian = Guardian.create(firstName, lastName, email, phone, userId);
		return guardianRepository.save(guardian);
	}

	@Transactional
	public Guardian updateAddress(String publicId, Address address) {
		Guardian guardian = findByPublicId(publicId);
		guardian.updateAddress(address);
		return guardianRepository.save(guardian);
	}

	@Transactional
	public Guardian updatePhone(String publicId, String phone) {
		Guardian guardian = findByPublicId(publicId);
		String defaultRegion = guardian.getAddress() != null ? guardian.getAddress().getCountryCode() : null;
		if (!phoneNumberValidator.isValid(phone, defaultRegion)) {
			throw new BusinessException("Invalid phone number: " + phone);
		}
		guardian.updatePhone(phone);
		return guardianRepository.save(guardian);
	}

	@Transactional
	public Guardian updatePhoto(String publicId, String filePublicId) {
		Guardian guardian = findByPublicId(publicId);
		guardian.updatePhoto(Optional.ofNullable(filePublicId).map(UUID::fromString).orElse(null));
		return guardianRepository.save(guardian);
	}

	@Transactional
	public StudentGuardianLink linkToStudent(String guardianPublicId, String studentPublicId,
			RelationshipType relationshipType, boolean primaryContact) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Guardian guardian = guardianRepository.findByPublicIdAndTenantId(UUID.fromString(guardianPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Guardian not found: " + guardianPublicId));
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		if (studentGuardianLinkRepository.existsByGuardianIdAndStudentIdAndTenantId(guardian.getId(),
				student.getId(), tenantId)) {
			throw new IllegalArgumentException(
					"Guardian " + guardianPublicId + " is already linked to student " + studentPublicId);
		}
		StudentGuardianLink link = StudentGuardianLink.create(student.getId(), guardian.getId(), relationshipType,
				primaryContact);
		StudentGuardianLink saved = studentGuardianLinkRepository.save(link);
		eventPublisher.publish(new GuardianLinkedEvent(tenantId, student.getId(), guardian.getId(),
				relationshipType));
		return saved;
	}

	@Transactional
	public StudentGuardianLink grantConsent(String guardianPublicId, String studentPublicId) {
		StudentGuardianLink link = findLink(guardianPublicId, studentPublicId);
		link.giveConsent();
		return studentGuardianLinkRepository.save(link);
	}

	@Transactional
	public StudentGuardianLink revokeConsent(String guardianPublicId, String studentPublicId) {
		StudentGuardianLink link = findLink(guardianPublicId, studentPublicId);
		link.revokeConsent();
		return studentGuardianLinkRepository.save(link);
	}

	private StudentGuardianLink findLink(String guardianPublicId, String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Guardian guardian = guardianRepository.findByPublicIdAndTenantId(UUID.fromString(guardianPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Guardian not found: " + guardianPublicId));
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		return studentGuardianLinkRepository
				.findByGuardianIdAndStudentIdAndTenantId(guardian.getId(), student.getId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"No guardian-student link found for guardian " + guardianPublicId + " and student "
								+ studentPublicId));
	}

	@Transactional(readOnly = true)
	public Page<Student> listLinkedStudentsForCurrentUser(Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long userId = currentUserId();
		Guardian guardian = guardianRepository.findByUserIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("No guardian record linked to the current user"));
		Page<StudentGuardianLink> links = studentGuardianLinkRepository.findByGuardianId(tenantId, guardian.getId(),
				pageable);
		List<Student> students = links.getContent().stream()
				.map(link -> studentRepository.findByIdAndTenantId(link.getStudentId(), tenantId)
						.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + link.getStudentId())))
				.toList();
		return new PageImpl<>(students, pageable, links.getTotalElements());
	}

	private Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
			return user.getId();
		}
		throw new AccessDeniedException("Authenticated principal missing");
	}
}
