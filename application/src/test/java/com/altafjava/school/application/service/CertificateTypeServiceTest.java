package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.certificate.model.CertificateType;
import com.altafjava.school.domain.certificate.repository.CertificateTypeRepository;

@ExtendWith(MockitoExtension.class)
class CertificateTypeServiceTest {

	@Mock
	private CertificateTypeRepository repository;

	private CertificateTypeService service;

	@BeforeEach
	void setUp() {
		service = new CertificateTypeService(repository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_savesANewType() {
		when(repository.save(any(CertificateType.class))).thenAnswer(inv -> inv.getArgument(0));

		CertificateType type = service.create("TRANSFER", "Transfer Certificate", "wording");

		assertEquals("CERTIFICATE.TRANSFER", type.documentType());
	}

	@Test
	void create_duplicateCode_throws() {
		when(repository.existsByCodeAndTenantId("TRANSFER", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> service.create("TRANSFER", "Another", "wording"));

		verify(repository, never()).save(any());
	}

	@Test
	void create_duplicateName_throws() {
		when(repository.existsByNameAndTenantId("Transfer Certificate", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> service.create("TRANSFER", "Transfer Certificate", "wording"));
	}

	@Test
	void findByPublicId_unknown_throwsResourceNotFound() {
		UUID id = UUID.randomUUID();
		when(repository.findByPublicIdAndTenantId(id, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> service.findByPublicId(id.toString()));
	}

	@Test
	void deactivate_flipsActiveFlag() {
		UUID id = UUID.randomUUID();
		CertificateType type = CertificateType.create("TC", "TC", "w");
		when(repository.findByPublicIdAndTenantId(id, 1L)).thenReturn(Optional.of(type));
		when(repository.save(any(CertificateType.class))).thenAnswer(inv -> inv.getArgument(0));

		assertFalse(service.deactivate(id.toString()).isActive());
	}
}
