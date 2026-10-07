package com.altafjava.school.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.guardian.model.EmergencyContact;
import com.altafjava.school.domain.guardian.repository.EmergencyContactRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

	private final EmergencyContactRepository emergencyContactRepository;
	private final StudentRepository studentRepository;

	@Transactional(readOnly = true)
	public List<EmergencyContact> listForStudent(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return emergencyContactRepository.findAllByStudentIdAndTenantIdOrderByPriorityAsc(student.getId(), tenantId);
	}

	@Transactional
	public EmergencyContact add(String studentPublicId, String name, String relationship, String phone,
			String alternatePhone, int priority) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return emergencyContactRepository.save(
				EmergencyContact.create(student.getId(), name, relationship, phone, alternatePhone, priority));
	}

	@Transactional
	public EmergencyContact update(String studentPublicId, String contactPublicId, String name, String relationship,
			String phone, String alternatePhone, int priority) {
		EmergencyContact contact = requireOwnedContact(studentPublicId, contactPublicId);
		contact.update(name, relationship, phone, alternatePhone, priority);
		return emergencyContactRepository.save(contact);
	}

	@Transactional
	public void remove(String studentPublicId, String contactPublicId, Long deletedByUserId) {
		EmergencyContact contact = requireOwnedContact(studentPublicId, contactPublicId);
		contact.softDelete(String.valueOf(deletedByUserId));
		emergencyContactRepository.save(contact);
	}

	// Scoping the contact to the path's student stops one student's URL addressing another's contact.
	private EmergencyContact requireOwnedContact(String studentPublicId, String contactPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		return emergencyContactRepository.findByPublicIdAndTenantId(UUID.fromString(contactPublicId), tenantId)
				.filter(contact -> contact.getStudentId().equals(student.getId()))
				.orElseThrow(() -> new ResourceNotFoundException("Emergency contact not found: " + contactPublicId));
	}

	private Student requireStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
