package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class FeeAssignmentService {

	private final FeeAssignmentRepository feeAssignmentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;

	public FeeAssignmentService(FeeAssignmentRepository feeAssignmentRepository,
			FeeStructureRepository feeStructureRepository, StudentRepository studentRepository,
			ClassroomRepository classroomRepository) {
		this.feeAssignmentRepository = feeAssignmentRepository;
		this.feeStructureRepository = feeStructureRepository;
		this.studentRepository = studentRepository;
		this.classroomRepository = classroomRepository;
	}

	@Transactional
	public FeeAssignment assign(String feeStructurePublicId, String studentPublicId, String classroomPublicId) {
		if ((studentPublicId == null) == (classroomPublicId == null)) {
			throw new BusinessException("Exactly one of studentPublicId or classroomPublicId must be provided");
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		Long feeStructureId = feeStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(feeStructurePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeeStructure not found: " + feeStructurePublicId))
				.getId();

		if (studentPublicId != null) {
			return assignToStudent(tenantId, feeStructureId, studentPublicId);
		}
		return assignToClassroom(tenantId, feeStructureId, classroomPublicId);
	}

	private FeeAssignment assignToStudent(Long tenantId, Long feeStructureId, String studentPublicId) {
		Long studentId = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId))
				.getId();
		if (feeAssignmentRepository.existsByFeeStructureIdAndStudentIdAndTenantId(feeStructureId, studentId,
				tenantId)) {
			throw new BusinessException("Fee structure is already assigned to student " + studentPublicId);
		}
		return feeAssignmentRepository.save(FeeAssignment.forStudent(feeStructureId, studentId));
	}

	private FeeAssignment assignToClassroom(Long tenantId, Long feeStructureId, String classroomPublicId) {
		Long classroomId = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId))
				.getId();
		if (feeAssignmentRepository.existsByFeeStructureIdAndClassroomIdAndTenantId(feeStructureId, classroomId,
				tenantId)) {
			throw new BusinessException("Fee structure is already assigned to classroom " + classroomPublicId);
		}
		return feeAssignmentRepository.save(FeeAssignment.forClassroom(feeStructureId, classroomId));
	}

	@Transactional(readOnly = true)
	public Page<FeeAssignment> listForFeeStructure(String feeStructurePublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Long feeStructureId = feeStructureRepository
				.findByPublicIdAndTenantId(UUID.fromString(feeStructurePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeeStructure not found: " + feeStructurePublicId))
				.getId();
		return feeAssignmentRepository.findByFeeStructureIdAndTenantId(tenantId, feeStructureId, pageable);
	}

	@Transactional
	public void revoke(String assignmentPublicId) {
		FeeAssignment assignment = findByPublicId(assignmentPublicId);
		assignment.softDelete("fee-assignment-revocation");
		feeAssignmentRepository.save(assignment);
	}

	@Transactional
	public FeeAssignment configureDueDate(String assignmentPublicId, LocalDate dueDate, Integer graceDays,
			BigDecimal lateFeePercentage, ExpectedVersion expectedVersion) {
		FeeAssignment assignment = findByPublicId(assignmentPublicId);
		expectedVersion.verify(assignment);
		assignment.configureDueDate(dueDate, graceDays, lateFeePercentage);
		return feeAssignmentRepository.save(assignment);
	}

	private FeeAssignment findByPublicId(String assignmentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return feeAssignmentRepository.findByPublicIdAndTenantId(UUID.fromString(assignmentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeeAssignment not found: " + assignmentPublicId));
	}
}
