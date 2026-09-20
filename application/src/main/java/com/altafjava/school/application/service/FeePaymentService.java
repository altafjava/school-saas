package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.fee.model.FeeAssignment;
import com.altafjava.school.domain.fee.model.FeeBalance;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.repository.FeeAssignmentRepository;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.fee.service.FeeBalanceCalculator;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class FeePaymentService {

	private static final String FEE_RECEIPT_SEQUENCE = "FEE_RECEIPT";

	private final FeePaymentRepository feePaymentRepository;
	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final FeeAssignmentRepository feeAssignmentRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final NumberSequenceService numberSequenceService;
	private final FeeBalanceCalculator feeBalanceCalculator = new FeeBalanceCalculator();

	public FeePaymentService(FeePaymentRepository feePaymentRepository, StudentRepository studentRepository,
			FeeStructureRepository feeStructureRepository, FeeAssignmentRepository feeAssignmentRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository,
			StudentDataAccessGuard studentDataAccessGuard, NumberSequenceService numberSequenceService) {
		this.feePaymentRepository = feePaymentRepository;
		this.studentRepository = studentRepository;
		this.feeStructureRepository = feeStructureRepository;
		this.feeAssignmentRepository = feeAssignmentRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.studentDataAccessGuard = studentDataAccessGuard;
		this.numberSequenceService = numberSequenceService;
	}

	@Transactional(readOnly = true)
	public Page<FeePayment> listFeePayments(Pageable pageable) {
		return feePaymentRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public FeePayment findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return feePaymentRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public List<FeeBalance> calculateBalance(String studentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return calculateBalanceForStudent(tenantId, student);
	}

	/**
	 * Same computation as {@link #calculateBalance}, for trusted internal callers (scheduler
	 * jobs) that already have a resolved {@link Student} and tenant, and are not acting on behalf
	 * of an end user — so no {@link StudentDataAccessGuard} check applies.
	 */
	@Transactional(readOnly = true)
	public List<FeeBalance> calculateBalanceForStudent(Long tenantId, Student student) {
		Map<Long, FeeAssignment> assignmentsByFeeStructureId = resolveApplicableAssignments(tenantId, student);
		List<FeeStructure> feeStructures = feeStructureRepository
				.findAllByIdInAndTenantId(new ArrayList<>(assignmentsByFeeStructureId.keySet()), tenantId);
		List<FeePayment> payments = feePaymentRepository.findByStudentId(tenantId, student.getId());
		LocalDate today = LocalDate.now();

		return feeStructures.stream()
				.map(feeStructure -> feeBalanceCalculator.calculate(feeStructure,
						assignmentsByFeeStructureId.get(feeStructure.getId()),
						totalPaidFor(payments, feeStructure.getId()), today))
				.toList();
	}

	/**
	 * Batched form of {@link #calculateBalanceForStudent} for callers (alert evaluators) that need
	 * every active student's balance in one pass — a constant number of queries regardless of how
	 * many students are passed in, instead of {@link #calculateBalanceForStudent}'s ~5 queries
	 * called once per student.
	 */
	@Transactional(readOnly = true)
	public Map<Long, List<FeeBalance>> calculateBalancesForStudents(Long tenantId, List<Student> students) {
		if (students.isEmpty()) {
			return Map.of();
		}
		List<Long> studentIds = students.stream().map(Student::getId).toList();

		Map<Long, Long> currentClassroomIdByStudentId = resolveCurrentClassroomIds(tenantId, studentIds);
		List<Long> classroomIds = currentClassroomIdByStudentId.values().stream().distinct().toList();

		Map<Long, Map<Long, FeeAssignment>> assignmentsByStudentId = new HashMap<>();
		for (Long studentId : studentIds) {
			assignmentsByStudentId.put(studentId, new HashMap<>());
		}
		// Classroom-scoped assignments applied first (lower precedence)...
		Map<Long, List<FeeAssignment>> classroomAssignmentsByClassroomId = classroomIds.isEmpty() ? Map.of()
				: feeAssignmentRepository.findByTenantIdAndClassroomIdIn(tenantId, classroomIds).stream()
						.collect(Collectors.groupingBy(FeeAssignment::getClassroomId));
		for (Long studentId : studentIds) {
			Long classroomId = currentClassroomIdByStudentId.get(studentId);
			if (classroomId != null) {
				for (FeeAssignment assignment : classroomAssignmentsByClassroomId.getOrDefault(classroomId,
						List.of())) {
					assignmentsByStudentId.get(studentId).put(assignment.getFeeStructureId(), assignment);
				}
			}
		}
		// ...then student-scoped assignments override (higher precedence) for the same structure.
		for (FeeAssignment assignment : feeAssignmentRepository.findByTenantIdAndStudentIdIn(tenantId, studentIds)) {
			assignmentsByStudentId.get(assignment.getStudentId()).put(assignment.getFeeStructureId(), assignment);
		}

		Set<Long> allFeeStructureIds = assignmentsByStudentId.values().stream()
				.flatMap(assignments -> assignments.keySet().stream())
				.collect(Collectors.toSet());
		Map<Long, FeeStructure> feeStructuresById = feeStructureRepository
				.findAllByIdInAndTenantId(allFeeStructureIds, tenantId).stream()
				.collect(Collectors.toMap(FeeStructure::getId, Function.identity()));

		Map<Long, List<FeePayment>> paymentsByStudentId = feePaymentRepository
				.findByStudentIdIn(tenantId, studentIds).stream()
				.collect(Collectors.groupingBy(FeePayment::getStudentId));

		LocalDate today = LocalDate.now();
		Map<Long, List<FeeBalance>> result = new HashMap<>();
		for (Long studentId : studentIds) {
			List<FeePayment> payments = paymentsByStudentId.getOrDefault(studentId, List.of());
			List<FeeBalance> balances = assignmentsByStudentId.get(studentId).entrySet().stream()
					.map(entry -> {
						FeeStructure structure = feeStructuresById.get(entry.getKey());
						return structure == null ? null
								: feeBalanceCalculator.calculate(structure, entry.getValue(),
										totalPaidFor(payments, structure.getId()), today);
					})
					.filter(Objects::nonNull)
					.toList();
			result.put(studentId, balances);
		}
		return result;
	}

	private Map<Long, Long> resolveCurrentClassroomIds(Long tenantId, List<Long> studentIds) {
		Map<Long, Long> currentClassroomIdByStudentId = new HashMap<>();
		Map<Long, LocalDate> latestEnrolledAtByStudentId = new HashMap<>();
		for (var link : studentClassroomLinkRepository.findByStudentIdIn(tenantId, studentIds)) {
			Long studentId = link.getStudentId();
			LocalDate latest = latestEnrolledAtByStudentId.get(studentId);
			if (latest == null || link.getEnrolledAt().compareTo(latest) > 0) {
				currentClassroomIdByStudentId.put(studentId, link.getClassroomId());
				latestEnrolledAtByStudentId.put(studentId, link.getEnrolledAt());
			}
		}
		return currentClassroomIdByStudentId;
	}

	// Keyed by feeStructureId, one assignment per structure — a direct student-scoped assignment
	// takes precedence over a classroom-scoped one for the same structure (applied second, so it
	// overwrites), since a student-specific due-date/late-fee override is more specific.
	private Map<Long, FeeAssignment> resolveApplicableAssignments(Long tenantId, Student student) {
		Map<Long, FeeAssignment> byFeeStructureId = new HashMap<>();
		resolveCurrentClassroomId(tenantId, student.getId())
				.map(id -> feeAssignmentRepository.findByTenantIdAndClassroomId(tenantId, id))
				.orElseGet(List::of)
				.forEach(assignment -> byFeeStructureId.put(assignment.getFeeStructureId(), assignment));
		feeAssignmentRepository.findByTenantIdAndStudentId(tenantId, student.getId())
				.forEach(assignment -> byFeeStructureId.put(assignment.getFeeStructureId(), assignment));
		return byFeeStructureId;
	}

	private Optional<Long> resolveCurrentClassroomId(Long tenantId, Long studentId) {
		return studentClassroomLinkRepository.findByStudentId(tenantId, studentId).stream()
				.max((a, b) -> a.getEnrolledAt().compareTo(b.getEnrolledAt()))
				.map(link -> link.getClassroomId());
	}

	private BigDecimal totalPaidFor(List<FeePayment> payments, Long feeStructureId) {
		return payments.stream()
				.filter(payment -> feeStructureId.equals(payment.getFeeStructureId()))
				.map(FeePayment::getPaidAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "FeePayment", details = "Fee payment recorded")
	public FeePayment record(Long studentId, Long feeStructureId, BigDecimal paidAmount,
			LocalDateTime paidAt, String receiptNumber) {
		Long tenantId = TenantContext.getCurrentTenantId();
		if (!studentRepository.existsByIdAndTenantId(studentId, tenantId)) {
			throw new ResourceNotFoundException("Student not found: " + studentId);
		}
		if (!feeStructureRepository.existsByIdAndTenantId(feeStructureId, tenantId)) {
			throw new ResourceNotFoundException("FeeStructure not found: " + feeStructureId);
		}
		String resolvedReceiptNumber = resolveReceiptNumber(tenantId, receiptNumber);
		if (feePaymentRepository.existsByReceiptNumberAndTenantId(resolvedReceiptNumber, tenantId)) {
			throw new IllegalArgumentException("Receipt number already exists: " + resolvedReceiptNumber);
		}
		FeePayment payment = FeePayment.create(studentId, feeStructureId, paidAmount, paidAt, resolvedReceiptNumber);
		return feePaymentRepository.save(payment);
	}

	// A caller-supplied receiptNumber is an explicit override; omitting it defers to the tenant's
	// configured numbering sequence (prefix/width/reset period), defaulting to a YEARLY-resetting
	// "RCPT-2026-000001" style — the common accounting convention of receipt numbers restarting
	// each fiscal/calendar year, unlike student/employee codes which never reset.
	private String resolveReceiptNumber(Long tenantId, String receiptNumber) {
		if (receiptNumber != null && !receiptNumber.isBlank()) {
			return receiptNumber;
		}
		return numberSequenceService.generateNext(tenantId, FEE_RECEIPT_SEQUENCE, "RCPT-", 6, ResetPeriod.YEARLY);
	}
}
