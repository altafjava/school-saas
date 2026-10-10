package com.altafjava.school.application.service;

import static com.altafjava.school.application.support.TestEntities.publicId;
import static com.altafjava.school.application.support.TestEntities.withId;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetAssignment;
import com.altafjava.school.domain.inventory.model.AssetStatus;
import com.altafjava.school.domain.inventory.model.AssignedToType;
import com.altafjava.school.domain.inventory.repository.AssetAssignmentRepository;
import com.altafjava.school.domain.inventory.repository.AssetRepository;

@ExtendWith(MockitoExtension.class)
class AssetAssignmentServiceTest {

	private static final UUID ASSET_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private AssetAssignmentRepository assetAssignmentRepository;
	@Mock
	private AssetRepository assetRepository;

	@Mock
	private EmployeeRepository employeeRepository;
	@Mock
	private ClassroomRepository classroomRepository;

	private AssetAssignmentService assetAssignmentService;

	@BeforeEach
	void setUp() {
		assetAssignmentService = new AssetAssignmentService(assetAssignmentRepository, assetRepository,
				employeeRepository, classroomRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Asset assetWithId(long id) {
		Asset asset = Asset.create("AST-1", "Projector", "Electronics", LocalDate.of(2026, 1, 1),
				BigDecimal.valueOf(500), "Room 101");
		asset.setId(id);
		return asset;
	}

	@Test
	void assign_marksAssetInUse() {
		Asset asset = assetWithId(5L);
		when(assetRepository.findByPublicIdAndTenantId(ASSET_PUBLIC_ID, 1L)).thenReturn(Optional.of(asset));
		when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));
		when(assetAssignmentRepository.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));

		when(employeeRepository.findByPublicIdAndTenantId(publicId("employee", 20), 1L))
				.thenReturn(Optional.of(withId(Employee.class, 20L)));

		AssetAssignment assignment = assertDoesNotThrow(() -> assetAssignmentService.assign(
				ASSET_PUBLIC_ID.toString(), AssignedToType.STAFF, publicId("employee", 20).toString(),
				LocalDate.of(2026, 4, 1)));

		assertEquals(AssetStatus.IN_USE, asset.getStatus());
		assertEquals(5L, assignment.getAssetId());
		assertEquals(20L, assignment.getAssignedToId());
	}

	@Test
	void assign_toUnknownHolder_throwsResourceNotFoundAndLeavesAssetUntouched() {
		Asset asset = assetWithId(5L);
		when(assetRepository.findByPublicIdAndTenantId(ASSET_PUBLIC_ID, 1L)).thenReturn(Optional.of(asset));
		when(classroomRepository.findByPublicIdAndTenantId(publicId("classroom", 9), 1L))
				.thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> assetAssignmentService.assign(
				ASSET_PUBLIC_ID.toString(), AssignedToType.CLASSROOM, publicId("classroom", 9).toString(),
				LocalDate.of(2026, 4, 1)));

		verify(assetAssignmentRepository, never()).save(any());
	}

	@Test
	void markReturned_marksAssetAvailableAgain() {
		UUID assignmentPublicId = UUID.randomUUID();
		Asset asset = assetWithId(5L);
		asset.markInUse();
		AssetAssignment assignment = AssetAssignment.create(5L, AssignedToType.STAFF, 20L, LocalDate.of(2026, 4, 1));
		when(assetAssignmentRepository.findByPublicIdAndTenantId(assignmentPublicId, 1L))
				.thenReturn(Optional.of(assignment));
		when(assetAssignmentRepository.save(any(AssetAssignment.class))).thenAnswer(inv -> inv.getArgument(0));
		when(assetRepository.findByIdAndTenantId(5L, 1L)).thenReturn(Optional.of(asset));
		when(assetRepository.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

		assetAssignmentService.markReturned(assignmentPublicId.toString(), LocalDate.of(2026, 5, 1));

		assertEquals(AssetStatus.AVAILABLE, asset.getStatus());
	}
}
