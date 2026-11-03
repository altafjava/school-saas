package com.altafjava.school.application.invoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.service.NumberSequenceService;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.platform.domain.numbering.model.ResetPeriod;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.filter.DateWindow;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.StudentPlacementService;
import com.altafjava.school.application.student.StudentPlacement;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.fee.model.FeeInvoice;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine;
import com.altafjava.school.domain.fee.model.FeeInvoiceStatus;
import com.altafjava.school.domain.fee.model.FeePaymentStatus;
import com.altafjava.school.domain.fee.model.InvoicePayment;
import com.altafjava.school.domain.fee.model.InvoicePeriod;
import com.altafjava.school.domain.fee.repository.FeeInvoiceRepository;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Bills students period by period. An invoice lists what falls due in its period at today's
 * balance, is numbered from the tenant's sequence, and carries a verifiable PDF.
 */
@Service
@RequiredArgsConstructor
public class FeeInvoiceService {

	private static final String SEQUENCE = "FEE_INVOICE";
	private static final String TITLE = "Fee Invoice";

	private final FeeInvoiceRepository feeInvoiceRepository;
	private final FeePaymentRepository feePaymentRepository;
	private final FeePaymentService feePaymentService;
	private final StudentRepository studentRepository;
	private final ClassroomRepository classroomRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final StudentPlacementService studentPlacementService;
	private final NumberSequenceService numberSequenceService;
	private final DocumentIssuanceService documentIssuanceService;
	private final SchoolClock schoolClock;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final PublicIdLookup publicIdLookup;

	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "FeeInvoice", details = "Fee invoice issued")
	public InvoiceView issue(String studentPublicId, InvoicePeriod period, Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		requireNoInvoiceForPeriod(tenantId, student, period);
		return issueFor(tenantId, student, period, issuedByUserId);
	}

	/** Invoices every student of the classroom with something due; the others are reported, not failed. */
	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "FeeInvoice", details = "Classroom fee invoices issued")
	public InvoiceBatchResult issueForClassroom(String classroomPublicId, InvoicePeriod period, Long issuedByUserId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Classroom classroom = classroomRepository
				.findByPublicIdAndTenantId(UUID.fromString(classroomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomPublicId));
		List<Long> studentIds = studentClassroomLinkRepository.findAllByClassroomId(tenantId, classroom.getId())
				.stream()
				.map(StudentClassroomLink::getStudentId).distinct().toList();
		List<InvoiceView> issued = new ArrayList<>();
		List<InvoiceBatchResult.Skipped> skipped = new ArrayList<>();
		for (Student student : studentRepository.findAllByIdInAndTenantId(studentIds, tenantId)) {
			String reason = skipReason(tenantId, student, period);
			if (reason == null) {
				issued.add(issueFor(tenantId, student, period, issuedByUserId));
			} else {
				skipped.add(new InvoiceBatchResult.Skipped(student.getPublicId().toString(), reason));
			}
		}
		return new InvoiceBatchResult(issued, skipped);
	}

	@Transactional(readOnly = true)
	public Page<InvoiceView> list(String studentPublicId, FeeInvoiceStatus status, DateWindow dates,
			Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return withSettlements(tenantId, feeInvoiceRepository.search(tenantId,
				publicIdLookup.idOrNull(EntityRef.STUDENT, studentPublicId), status, dates.from(), dates.to(),
				pageable));
	}

	/** A student's own invoices, for the student and their guardians. */
	@Transactional(readOnly = true)
	public Page<InvoiceView> listForStudent(String studentPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = requireStudent(tenantId, studentPublicId);
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		return withSettlements(tenantId,
				feeInvoiceRepository.search(tenantId, student.getId(), null, null, null, pageable));
	}

	@Transactional(readOnly = true)
	public InvoiceView find(String invoicePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return viewOf(tenantId, require(tenantId, invoicePublicId));
	}

	@Transactional(readOnly = true)
	public InvoiceView findForStudent(String studentPublicId, String invoicePublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		studentDataAccessGuard.assertCanView(tenantId, studentPublicId);
		FeeInvoice invoice = require(tenantId, invoicePublicId);
		if (!invoice.getStudentId().equals(requireStudent(tenantId, studentPublicId).getId())) {
			throw new ResourceNotFoundException("Invoice not found: " + invoicePublicId);
		}
		return viewOf(tenantId, invoice);
	}

	@Transactional(readOnly = true)
	public byte[] pdf(FeeInvoice invoice) {
		DocumentIssuance issuance = documentIssuanceService.findById(invoice.getDocumentIssuanceId())
				.orElseThrow(() -> new ResourceNotFoundException("The invoice has no document"));
		return documentIssuanceService.downloadPdf(issuance);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "FeeInvoice", details = "Fee invoice cancelled")
	public InvoiceView cancel(String invoicePublicId, String reason) {
		Long tenantId = TenantContext.getCurrentTenantId();
		FeeInvoice invoice = require(tenantId, invoicePublicId);
		if (paidOn(tenantId, List.of(invoice.getId())).getOrDefault(invoice.getId(), BigDecimal.ZERO).signum() > 0) {
			throw new BusinessException("Payments stand against this invoice, so it cannot be cancelled");
		}
		invoice.cancel(reason);
		documentIssuanceService.findById(invoice.getDocumentIssuanceId())
				.filter(issuance -> !issuance.isRevoked())
				.ifPresent(issuance -> documentIssuanceService.revoke(issuance, "Invoice cancelled: " + reason));
		return viewOf(tenantId, feeInvoiceRepository.save(invoice));
	}

	private InvoiceView issueFor(Long tenantId, Student student, InvoicePeriod period, Long issuedByUserId) {
		List<FeeInvoiceLine> lines = linesFor(tenantId, student, period);
		String number = numberSequenceService.generateNext(tenantId, SEQUENCE, "INV-", 6, ResetPeriod.YEARLY);
		FeeInvoice invoice = feeInvoiceRepository.save(FeeInvoice.issue(student.getId(), number, period,
				schoolClock.today(tenantId), lines));
		invoice.attachDocument(documentIssuanceService.issue(new DocumentIssueRequest(tenantId,
				SchoolDocumentTypes.FEE_INVOICE, SchoolDocumentTypes.OWNER_FEE_INVOICE, invoice.getId(), TITLE,
				student.getFirstName() + " " + student.getLastName(), documentModel(student, invoice),
				issuedByUserId)).getId());
		return InvoiceView.of(feeInvoiceRepository.save(invoice), invoice.settlement(BigDecimal.ZERO));
	}

	private List<FeeInvoiceLine> linesFor(Long tenantId, Student student, InvoicePeriod period) {
		Set<FeeInvoiceLine.ChargeKey> billed = feeInvoiceRepository
				.findBilledLines(tenantId, student.getId(), FeeInvoiceStatus.ISSUED).stream()
				.map(FeeInvoiceLine::charge).collect(Collectors.toSet());
		return InvoiceLineBuilder.linesFor(feePaymentService.calculateBalanceForStudent(tenantId, student), period,
				billed);
	}

	private String skipReason(Long tenantId, Student student, InvoicePeriod period) {
		if (feeInvoiceRepository.existsForPeriod(tenantId, student.getId(), FeeInvoiceStatus.ISSUED, period.start(),
				period.end())) {
			return "Already invoiced for this period";
		}
		return linesFor(tenantId, student, period).isEmpty() ? "Nothing due in the period" : null;
	}

	private void requireNoInvoiceForPeriod(Long tenantId, Student student, InvoicePeriod period) {
		if (feeInvoiceRepository.existsForPeriod(tenantId, student.getId(), FeeInvoiceStatus.ISSUED, period.start(),
				period.end())) {
			throw new BusinessException("The student already has an invoice for this period; cancel it to reissue");
		}
	}

	private Map<String, Object> documentModel(Student student, FeeInvoice invoice) {
		StudentPlacement placement = studentPlacementService.currentPlacements(List.of(student)).get(student.getId());
		Map<String, Object> model = new HashMap<>();
		model.put("invoiceNumber", invoice.getInvoiceNumber());
		model.put("studentName", student.getFirstName() + " " + student.getLastName());
		model.put("studentCode", student.getStudentCode());
		model.put("className", placement == null ? "" : placement.classroomName());
		model.put("period", invoice.getPeriodStart() + " to " + invoice.getPeriodEnd());
		model.put("issueDate", invoice.getIssueDate().toString());
		model.put("dueDate", invoice.getDueDate().toString());
		model.put("lines", invoice.getLines().stream()
				.map(line -> Map.of("description", line.getDescription(), "amount", line.getAmount().toPlainString()))
				.toList());
		model.put("total", invoice.getTotalAmount().toPlainString());
		return model;
	}

	private Page<InvoiceView> withSettlements(Long tenantId, Page<FeeInvoice> page) {
		Map<Long, BigDecimal> paid = page.isEmpty() ? Map.of()
				: paidOn(tenantId, page.map(FeeInvoice::getId).getContent());
		return page.map(invoice -> InvoiceView.of(invoice,
				invoice.settlement(paid.getOrDefault(invoice.getId(), BigDecimal.ZERO))));
	}

	private InvoiceView viewOf(Long tenantId, FeeInvoice invoice) {
		return InvoiceView.of(invoice, invoice.settlement(
				paidOn(tenantId, List.of(invoice.getId())).getOrDefault(invoice.getId(), BigDecimal.ZERO)));
	}

	private Map<Long, BigDecimal> paidOn(Long tenantId, List<Long> invoiceIds) {
		return feePaymentRepository.sumByInvoice(tenantId, invoiceIds, FeePaymentStatus.CLEARED).stream()
				.collect(Collectors.toMap(InvoicePayment::invoiceId, InvoicePayment::paid));
	}

	private FeeInvoice require(Long tenantId, String invoicePublicId) {
		return feeInvoiceRepository.findByPublicIdAndTenantId(UUID.fromString(invoicePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoicePublicId));
	}

	private Student requireStudent(Long tenantId, String studentPublicId) {
		return studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
	}
}
