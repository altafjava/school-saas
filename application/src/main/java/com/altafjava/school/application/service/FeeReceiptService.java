package com.altafjava.school.application.service;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.PaymentMode;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Fee receipt PDFs, issued on first download and kept, so recording a payment never depends on
 * storage or rendering. No transaction is held while the PDF renders.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeReceiptService {

	static final String TITLE = "Fee Receipt";
	private static final String DATE_PATTERN = "d MMMM yyyy";

	private final FeePaymentRepository feePaymentRepository;
	private final StudentRepository studentRepository;
	private final FeeStructureRepository feeStructureRepository;
	private final StudentPlacementResolver placementResolver;
	private final StudentDataAccessGuard studentDataAccessGuard;
	private final DocumentIssuanceService documentIssuanceService;
	private final TenantRepository tenantRepository;
	private final TenantFormattingService tenantFormattingService;
	private final PlatformTransactionManager transactionManager;

	/** For fee staff: any payment in the school. */
	public FeeReceiptPdf download(String paymentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		FeePayment payment = requirePayment(tenantId, paymentPublicId);
		return pdf(tenantId, payment, requireStudent(tenantId, payment, paymentPublicId));
	}

	/** For a guardian or student: only payments for a student they may see. */
	public FeeReceiptPdf downloadForSelfService(String paymentPublicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		FeePayment payment = requirePayment(tenantId, paymentPublicId);
		Student student = requireStudent(tenantId, payment, paymentPublicId);
		studentDataAccessGuard.assertCanView(tenantId, student.getPublicId().toString());
		return pdf(tenantId, payment, student);
	}

	private FeeReceiptPdf pdf(Long tenantId, FeePayment payment, Student student) {
		if (payment.isBounced()) {
			throw new BusinessException("A bounced cheque has no receipt");
		}
		DocumentIssuance issuance = payment.getReceiptDocumentId() != null ? existing(payment)
				: issue(tenantId, payment, student);
		return new FeeReceiptPdf(fileName(payment), documentIssuanceService.downloadPdf(issuance));
	}

	private DocumentIssuance existing(FeePayment payment) {
		return documentIssuanceService.findById(payment.getReceiptDocumentId())
				.orElseThrow(
						() -> new IllegalStateException("Receipt document missing for payment " + payment.getId()));
	}

	// Two first downloads can race; the row lock picks one receipt and the loser's copy is revoked.
	private DocumentIssuance issue(Long tenantId, FeePayment payment, Student student) {
		DocumentIssuance issued = documentIssuanceService.issue(new DocumentIssueRequest(tenantId,
				SchoolDocumentTypes.FEE_RECEIPT, SchoolDocumentTypes.OWNER_STUDENT, student.getId(), TITLE,
				fullName(student), model(tenantId, payment, student), null));
		FeePayment current = new TransactionTemplate(transactionManager).execute(status -> {
			FeePayment locked = feePaymentRepository
					.findByPublicIdAndTenantIdForUpdate(payment.getPublicId(), tenantId)
					.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + payment.getPublicId()));
			if (locked.getReceiptDocumentId() != null || locked.isBounced()) {
				return locked;
			}
			locked.attachReceipt(issued.getId());
			return feePaymentRepository.save(locked);
		});
		if (issued.getId().equals(current.getReceiptDocumentId())) {
			return issued;
		}
		documentIssuanceService.revoke(issued, "Superseded by another copy of this receipt");
		log.info("action=fee-receipt-duplicate-revoked tenantId={} paymentId={}", tenantId, current.getId());
		if (current.isBounced()) {
			throw new BusinessException("A bounced cheque has no receipt");
		}
		return existing(current);
	}

	private Map<String, Object> model(Long tenantId, FeePayment payment, Student student) {
		Locale locale = tenantFormattingService.resolveLocale(tenantId);
		DateTimeFormatter dateFormat = tenantFormattingService.localizedFormatter(tenantId, DATE_PATTERN);
		String reference = reference(payment, dateFormat);
		Map<String, Object> model = new HashMap<>();
		model.put("receiptNumber", payment.getReceiptNumber());
		model.put("studentName", fullName(student));
		model.put("studentCode", student.getStudentCode());
		model.put("className", placementResolver.resolve(tenantId, student.getId())
				.map(StudentPlacementResolver.Placement::classLabel).orElse(""));
		model.put("feeName", feeStructureRepository
				.findAllByIdInAndTenantId(List.of(payment.getFeeStructureId()), tenantId).stream()
				.map(FeeStructure::getName).findFirst().orElse(""));
		model.put("amount", formatAmount(tenantId, payment.getPaidAmount(), locale));
		model.put("paidOn", payment.getPaidAt().format(dateFormat));
		model.put("paymentMode", label(payment.getPaymentMode()));
		model.put("hasReference", reference != null);
		model.put("reference", reference == null ? "" : reference);
		return model;
	}

	private static String reference(FeePayment payment, DateTimeFormatter dateFormat) {
		if (payment.getPaymentMode() == PaymentMode.CHEQUE) {
			StringBuilder cheque = new StringBuilder("Cheque ").append(payment.getChequeNumber());
			if (payment.getChequeBank() != null) {
				cheque.append(", ").append(payment.getChequeBank());
			}
			if (payment.getChequeDate() != null) {
				cheque.append(", dated ").append(payment.getChequeDate().format(dateFormat));
			}
			return cheque.toString();
		}
		if (payment.getTransactionReference() != null) {
			return payment.getTransactionReference();
		}
		return payment.getGatewayChargeReference();
	}

	// The school's currency when it has one; otherwise the bare amount, never a guessed currency.
	private String formatAmount(Long tenantId, BigDecimal amount, Locale locale) {
		return tenantRepository.findById(tenantId).map(Tenant::getCurrencyCode)
				.filter(code -> code != null && !code.isBlank())
				.map(code -> {
					NumberFormat format = NumberFormat.getCurrencyInstance(locale);
					format.setCurrency(Currency.getInstance(code));
					return format.format(amount);
				})
				.orElseGet(() -> new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(locale))
						.format(amount));
	}

	private static String label(PaymentMode mode) {
		return switch (mode) {
			case CASH -> "Cash";
			case CHEQUE -> "Cheque";
			case UPI -> "UPI";
			case BANK_TRANSFER -> "Bank transfer";
			case CARD -> "Card";
			case GATEWAY -> "Online payment";
		};
	}

	private static String fileName(FeePayment payment) {
		return "receipt-" + payment.getReceiptNumber().replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
	}

	private static String fullName(Student student) {
		return student.getFirstName() + " " + student.getLastName();
	}

	private FeePayment requirePayment(Long tenantId, String paymentPublicId) {
		return feePaymentRepository.findByPublicIdAndTenantId(UUID.fromString(paymentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + paymentPublicId));
	}

	private Student requireStudent(Long tenantId, FeePayment payment, String paymentPublicId) {
		return studentRepository.findByIdAndTenantId(payment.getStudentId(), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("FeePayment not found: " + paymentPublicId));
	}
}
