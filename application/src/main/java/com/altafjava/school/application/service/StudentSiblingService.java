package com.altafjava.school.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.student.model.SiblingGroup;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.SiblingGroupRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Keeps track of which students are siblings. Linking two students joins their families (a student
 * who already has siblings brings them all), so siblinghood stays transitive; leaving removes only
 * the one student, and a family left with a single member is dissolved.
 */
@Service
@RequiredArgsConstructor
public class StudentSiblingService {

	private static final Comparator<Student> BY_NAME = Comparator.comparing(Student::getFirstName)
			.thenComparing(Student::getLastName);

	private final StudentRepository studentRepository;
	private final SiblingGroupRepository siblingGroupRepository;

	@Transactional(readOnly = true)
	public List<Student> listSiblings(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = require(tenantId, studentPublicId);
		return siblingsOf(tenantId, student);
	}

	/** Students linked to one of this student's guardians who are not yet recorded as siblings. */
	@Transactional(readOnly = true)
	public List<Student> suggestSiblings(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = require(tenantId, studentPublicId);
		return studentRepository.findStudentsSharingAGuardianWith(tenantId, student.getId()).stream()
				.filter(candidate -> !student.isSiblingOf(candidate))
				.sorted(BY_NAME)
				.toList();
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Student", details = "Sibling relationship recorded")
	public List<Student> link(String studentPublicId, String siblingPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = require(tenantId, studentPublicId);
		Student sibling = require(tenantId, siblingPublicId);
		if (student.getId().equals(sibling.getId())) {
			throw new BusinessException("A student cannot be their own sibling");
		}
		if (student.isSiblingOf(sibling)) {
			throw new BusinessException("They are already recorded as siblings");
		}
		joinFamilies(tenantId, student, sibling);
		return siblingsOf(tenantId, student);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Student", details = "Sibling relationship removed")
	public void leave(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = require(tenantId, studentPublicId);
		Long groupId = student.getSiblingGroupId();
		if (groupId == null) {
			throw new BusinessException("The student has no recorded siblings");
		}
		student.leaveSiblingGroup();
		studentRepository.saveAndFlush(student);
		List<Student> remaining = studentRepository.findAllBySiblingGroupIdAndTenantId(groupId, tenantId);
		if (remaining.size() <= 1) {
			dissolve(tenantId, groupId, remaining);
		}
	}

	private void joinFamilies(Long tenantId, Student student, Student sibling) {
		Long keptGroupId = student.getSiblingGroupId();
		if (keptGroupId == null) {
			keptGroupId = sibling.getSiblingGroupId() != null ? sibling.getSiblingGroupId()
					: siblingGroupRepository.save(SiblingGroup.create()).getId();
		}
		Long retiredGroupId = sibling.getSiblingGroupId();
		student.joinSiblingGroup(keptGroupId);
		studentRepository.save(student);
		if (retiredGroupId == null || retiredGroupId.equals(keptGroupId)) {
			sibling.joinSiblingGroup(keptGroupId);
			studentRepository.save(sibling);
			return;
		}
		mergeInto(tenantId, keptGroupId, retiredGroupId);
	}

	private void mergeInto(Long tenantId, Long keptGroupId, Long retiredGroupId) {
		for (Student member : studentRepository.findAllBySiblingGroupIdAndTenantId(retiredGroupId, tenantId)) {
			member.joinSiblingGroup(keptGroupId);
			studentRepository.save(member);
		}
		retire(tenantId, retiredGroupId, "sibling-group-merge");
	}

	private void dissolve(Long tenantId, Long groupId, List<Student> remaining) {
		remaining.forEach(member -> {
			member.leaveSiblingGroup();
			studentRepository.save(member);
		});
		retire(tenantId, groupId, "sibling-group-dissolved");
	}

	private void retire(Long tenantId, Long groupId, String reason) {
		siblingGroupRepository.findByIdAndTenantId(groupId, tenantId).ifPresent(group -> {
			group.softDelete(reason);
			siblingGroupRepository.save(group);
		});
	}

	private List<Student> siblingsOf(Long tenantId, Student student) {
		if (student.getSiblingGroupId() == null) {
			return List.of();
		}
		return studentRepository.findAllBySiblingGroupIdAndTenantId(student.getSiblingGroupId(), tenantId).stream()
				.filter(member -> !member.getId().equals(student.getId()))
				.sorted(BY_NAME)
				.toList();
	}

	private Student require(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
