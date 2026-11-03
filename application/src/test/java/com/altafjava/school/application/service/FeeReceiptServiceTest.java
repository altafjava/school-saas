package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;
import com.altafjava.platform.application.document.DocumentIssuanceService;
import com.altafjava.platform.application.document.DocumentIssueRequest;
import com.altafjava.platform.application.tenant.TenantFormattingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.document.model.DocumentIssuance;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.application.document.SchoolDocumentTypes;
import com.altafjava.school.application.document.StudentPlacementResolver;
import com.altafjava.school.application.security.StudentDataAccessGuard;
import com.altafjava.school.domain.fee.model.FeePayment;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.fee.model.PaymentInstrument;
import com.altafjava.school.domain.fee.model.PaymentMode;
import com.altafjava.school.domain.fee.repository.FeePaymentRepository;
import com.altafjava.school.domain.fee.repository.FeeStructureRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeeReceiptServiceTest {

	private static final Long TENANT_ID = 1L;
	private static final byte[] PDF = { '%', 'P', 'D', 'F' };

	@Mock
	private FeePaymentRepository feePaymentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private FeeStructureRepository feeStructureRepository;
	@Mock
	private StudentPlacementResolver placementResolver;
	@Mock
	private StudentDataAccessGuard studentDataAccessGuard;
	@Mock
	private DocumentIssuanceService documentIssuanceService;
	@Mock
	private TenantRepository tenantRepository;
	@Mock
	private TenantFormattingService tenantFormattingService;
	@Mock
	private PlatformTransactionManager transactionManager;
	@Mock
	private Tenant tenant;

	private FeeReceiptService service;
	private FeePayment payment;
	private Student student;

	@BeforeEach
	void setUp() {
		TenantContext.ForTesting.setCurrentTenant(TENANT_ID, null, null, TenantType.SHARED);
		service = new FeeReceiptService(feePaymentRepository, studentRepository, feeStructureRepository,
				placementResolver, studentDataAccessGuard, documentIssuanceService, tenantRepository,
				tenantFormattingService, transactionManager);
		student = Student.create("ADM-7", "Asha", "Rao", "asha@school.test", null);
		student.setId(5L);
		student.setPublicId(UUID.randomUUID());
		payment = payment(PaymentMode.CASH);
		FeeStructure tuition = org.mockito.Mockito.mock(FeeStructure.class);
		when(tuition.getName()).thenReturn("Tuition");
		when(feeStructureRepository.findAllByIdInAndTenantId(List.of(2L), TENANT_ID)).thenReturn(List.of(tuition));
		when(studentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(student));
		when(placementResolver.resolve(TENANT_ID, 5L)).thenReturn(Optional.empty());
		when(tenantFormattingService.resolveLocale(TENANT_ID)).thenReturn(Locale.forLanguageTag("en-IN"));
		when(tenantFormattingService.localizedFormatter(eq(TENANT_ID), anyString()))
				.thenReturn(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH));
		when(tenant.getCurrencyCode()).thenReturn("INR");
		when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.of(tenant));
		when(documentIssuanceService.downloadPdf(any())).thenReturn(PDF);
	}

	@AfterEach
	void clear() {
		TenantContext.ForTesting.clear();
	}

	private FeePayment payment(PaymentMode mode) {
		PaymentInstrument instrument = mode == PaymentMode.CHEQUE
				? new PaymentInstrument(mode, null, "004512", LocalDate.of(2026, 1, 30), "State Bank")
				: new PaymentInstrument(mode, null, null, null, null);
		FeePayment created = FeePayment.create(5L, 2L, BigDecimal.valueOf(1500), LocalDateTime.of(2026, 2, 1, 10, 0),
				"RCPT/2026/1", instrument);
		created.setId(9L);
		created.setPublicId(UUID.randomUUID());
		created.setTenantId(TENANT_ID);
		when(feePaymentRepository.findByPublicIdAndTenantId(created.getPublicId(), TENANT_ID))
				.thenReturn(Optional.of(created));
		when(feePaymentRepository.findByPublicIdAndTenantIdForUpdate(created.getPublicId(), TENANT_ID))
				.thenReturn(Optional.of(created));
		return created;
	}

	private static DocumentIssuance issuance(long id) {
		DocumentIssuance issuance = DocumentIssuance.create(SchoolDocumentTypes.FEE_RECEIPT,
				SchoolDocumentTypes.OWNER_STUDENT, 5L, FeeReceiptService.TITLE, "Asha Rao", null, null, "CODE" + id,
				"key", null);
		issuance.setId(id);
		return issuance;
	}

	@Test
	void firstDownload_issuesTheReceiptAndKeepsIt() {
		when(documentIssuanceService.issue(any())).thenReturn(issuance(77L));
		when(feePaymentRepository.save(payment)).thenReturn(payment);

		FeeReceiptPdf receipt = service.download(payment.getPublicId().toString());

		assertEquals(77L, payment.getReceiptDocumentId());
		assertEquals("receipt-RCPT_2026_1.pdf", receipt.fileName());
		assertArrayEquals(PDF, receipt.content());
		ArgumentCaptor<DocumentIssueRequest> request = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(request.capture());
		assertEquals(SchoolDocumentTypes.FEE_RECEIPT, request.getValue().documentType());
		assertEquals(5L, request.getValue().ownerEntityId());
		assertEquals("₹1,500.00", request.getValue().model().get("amount"));
		assertEquals("Tuition", request.getValue().model().get("feeName"));
		assertEquals("1 February 2026", request.getValue().model().get("paidOn"));
		assertEquals(false, request.getValue().model().get("hasReference"));
	}

	@Test
	void laterDownload_returnsTheReceiptAlreadyIssued() {
		payment.attachReceipt(77L);
		when(documentIssuanceService.findById(77L)).thenReturn(Optional.of(issuance(77L)));

		assertArrayEquals(PDF, service.download(payment.getPublicId().toString()).content());

		verify(documentIssuanceService, never()).issue(any());
	}

	@Test
	void chequeReceipt_printsTheChequeDetails() {
		FeePayment cheque = payment(PaymentMode.CHEQUE);
		when(documentIssuanceService.issue(any())).thenReturn(issuance(78L));
		when(feePaymentRepository.save(cheque)).thenReturn(cheque);

		service.download(cheque.getPublicId().toString());

		ArgumentCaptor<DocumentIssueRequest> request = ArgumentCaptor.forClass(DocumentIssueRequest.class);
		verify(documentIssuanceService).issue(request.capture());
		assertEquals("Cheque 004512, State Bank, dated 30 January 2026", request.getValue().model().get("reference"));
	}

	@Test
	void bouncedCheque_hasNoReceipt() {
		FeePayment cheque = payment(PaymentMode.CHEQUE);
		cheque.bounce("Insufficient funds", LocalDateTime.now());

		assertThrows(BusinessException.class, () -> service.download(cheque.getPublicId().toString()));
		verify(documentIssuanceService, never()).issue(any());
	}

	@Test
	void raceForTheFirstReceipt_keepsTheWinnerAndRevokesTheCopy() {
		DocumentIssuance copy = issuance(78L);
		when(documentIssuanceService.issue(any())).thenReturn(copy);
		FeePayment alreadyIssued = FeePayment.create(5L, 2L, BigDecimal.valueOf(1500), LocalDateTime.now(),
				"RCPT/2026/1",
				PaymentInstrument.CASH);
		alreadyIssued.setId(9L);
		alreadyIssued.attachReceipt(77L);
		when(feePaymentRepository.findByPublicIdAndTenantIdForUpdate(payment.getPublicId(), TENANT_ID))
				.thenReturn(Optional.of(alreadyIssued));
		when(documentIssuanceService.findById(77L)).thenReturn(Optional.of(issuance(77L)));

		service.download(payment.getPublicId().toString());

		verify(documentIssuanceService).revoke(eq(copy), anyString());
		verify(feePaymentRepository, never()).save(any());
	}

	@Test
	void selfService_checksTheCallerMaySeeTheStudent() {
		doThrow(new AccessDeniedException("no")).when(studentDataAccessGuard).assertCanView(TENANT_ID,
				student.getPublicId().toString());

		assertThrows(AccessDeniedException.class,
				() -> service.downloadForSelfService(payment.getPublicId().toString()));
		verify(documentIssuanceService, never()).issue(any());
	}
}
