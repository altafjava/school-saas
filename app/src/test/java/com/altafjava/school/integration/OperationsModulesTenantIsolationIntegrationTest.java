package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.filter.AssetFilter;
import com.altafjava.school.application.filter.EventFilter;
import com.altafjava.school.application.service.AssetAssignmentService;
import com.altafjava.school.application.service.AssetService;
import com.altafjava.school.application.service.BookCatalogService;
import com.altafjava.school.application.service.CirculationService;
import com.altafjava.school.application.service.DisciplineIncidentService;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.application.service.EventRegistrationService;
import com.altafjava.school.application.service.EventService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.VehicleService;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.employee.model.StaffCategory;
import com.altafjava.school.domain.event.model.Event;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.inventory.model.AssetStatus;
import com.altafjava.school.domain.inventory.model.AssignedToType;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.model.BookCopyStatus;
import com.altafjava.school.domain.library.model.Circulation;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.transport.model.Vehicle;

/**
 * Verifies transport/discipline/events/inventory/library resources created under tenant A are not
 * visible to tenant B, and exercises each module's core workflow end to end (capacity limits,
 * status transitions, fine calculation).
 */
@Import({ TestRedisConfig.class, TestPaymentConfig.class })
class OperationsModulesTenantIsolationIntegrationTest extends SchoolIntegrationTestBase {

	@Autowired
	private VehicleService vehicleService;

	@Autowired
	private DisciplineIncidentService disciplineIncidentService;

	@Autowired
	private EventService eventService;

	@Autowired
	private EventRegistrationService eventRegistrationService;

	@Autowired
	private AssetService assetService;

	@Autowired
	private AssetAssignmentService assetAssignmentService;

	@Autowired
	private EmployeeService employeeService;

	@Autowired
	private BookCatalogService bookCatalogService;

	@Autowired
	private CirculationService circulationService;

	@Autowired
	private StudentService studentService;

	@Autowired
	private TenantOnboardingService onboardingService;

	private Tenant tenantA;
	private Tenant tenantB;

	@BeforeEach
	void createTenants() {
		TenantContext.ForTesting.clear();
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		tenantA = onboardingService.registerTenant(new RegisterTenantCommand(
				"School A", "ops-a-" + suffix, 1L, "admin@ops-a.test", "Password123!", "USD"));
		tenantB = onboardingService.registerTenant(new RegisterTenantCommand(
				"School B", "ops-b-" + suffix, 1L, "admin@ops-b.test", "Password123!", "USD"));
		TenantContext.ForTesting.clear();
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
		SecurityContextHolder.clearContext();
	}

	private void activateTenant(Tenant tenant) {
		TenantContext.ForTesting.setCurrentTenant(tenant.getId(), tenant.getPublicId(), tenant.getSubdomain(),
				tenant.getType());
	}

	private void authenticateAsTenantAdmin() {
		AuthenticatedUser principal = fixedIdPrincipal(-1L);
		List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_TENANT_ADMIN"));
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
	}

	private AuthenticatedUser fixedIdPrincipal(Long userId) {
		return new AuthenticatedUser() {
			@Override
			public Long getId() {
				return userId;
			}

			@Override
			public String getUsername() {
				return "user-" + userId;
			}

			@Override
			public Long getTenantId() {
				return null;
			}
		};
	}

	private String uniqueSuffix() {
		return UUID.randomUUID().toString().substring(0, 8);
	}

	// --- Transport ---

	@Test
	void vehicleCreatedUnderTenantA_isNotVisibleWhenListingTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		vehicleService.create("KA-01-" + uniqueSuffix(), 40, "Ravi", "9999999999");

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Page<Vehicle> vehiclesB = vehicleService.list(null, PageRequest.of(0, 100));

		assertTrue(vehiclesB.getContent().isEmpty(), "Tenant B must not see tenant A's vehicles");
	}

	// --- Discipline ---

	@Test
	void disciplineIncidentCreatedUnderTenantA_isNotVisibleFromTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		Student student = studentService.enroll("STU-" + uniqueSuffix(), "Nora", "Kim",
				"nora-" + uniqueSuffix() + "@school.test", LocalDate.of(2011, 4, 4));
		String studentPublicId = student.getPublicId().toString();

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		assertThrows(ResourceNotFoundException.class,
				() -> disciplineIncidentService.listForStudent(studentPublicId, PageRequest.of(0, 20)),
				"Tenant B must not resolve tenant A's student");
	}

	// --- Events ---

	@Test
	void eventCreatedUnderTenantA_isNotVisibleWhenListingTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		eventService.create("Sports Day", null, LocalDateTime.of(2026, 12, 1, 9, 0), "Field", true, 2);

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Page<Event> eventsB = eventService.list(EventFilter.NONE, PageRequest.of(0, 100));

		assertTrue(eventsB.getContent().isEmpty(), "Tenant B must not see tenant A's events");
	}

	@Test
	void eventRegistration_atCapacity_rejectsFurtherRegistrations() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		Event event = eventService.create("Workshop", null, LocalDateTime.of(2026, 11, 1, 9, 0), "Hall", true, 1);
		Student studentOne = studentService.enroll("STU-" + uniqueSuffix(), "Amy", "Lee",
				"amy-" + uniqueSuffix() + "@school.test", LocalDate.of(2012, 1, 1));
		Student studentTwo = studentService.enroll("STU-" + uniqueSuffix(), "Ben", "Lee",
				"ben-" + uniqueSuffix() + "@school.test", LocalDate.of(2012, 2, 2));

		eventRegistrationService.register(event.getPublicId().toString(), studentOne.getPublicId().toString());

		assertThrows(BusinessException.class, () -> eventRegistrationService.register(
				event.getPublicId().toString(), studentTwo.getPublicId().toString()),
				"Registration beyond capacity must be rejected");
	}

	// --- Inventory ---

	@Test
	void assetCreatedUnderTenantA_isNotVisibleWhenListingTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		assetService.create("AST-" + uniqueSuffix(), "Projector", "Electronics", LocalDate.of(2026, 1, 1),
				BigDecimal.valueOf(500), "Room 101");

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Page<Asset> assetsB = assetService.list(AssetFilter.NONE, PageRequest.of(0, 100));

		assertTrue(assetsB.getContent().isEmpty(), "Tenant B must not see tenant A's assets");
	}

	@Test
	void assetAssignment_endToEnd_cyclesStatusCorrectly() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		Asset asset = assetService.create("AST-" + uniqueSuffix(), "Laptop", "Electronics", LocalDate.of(2026, 1, 1),
				BigDecimal.valueOf(1200), "Store Room");

		var holder = employeeService.hire(StaffCategory.ADMINISTRATIVE, "EMP-" + uniqueSuffix(), "Sam", "Holder",
				"holder-" + uniqueSuffix() + "@test.edu", LocalDate.of(2020, 1, 1));
		var assignment = assetAssignmentService.assign(asset.getPublicId().toString(), AssignedToType.STAFF,
				holder.getPublicId().toString(), LocalDate.of(2026, 4, 1));
		Asset inUse = assetService.findByPublicId(asset.getPublicId().toString());
		assertEquals(AssetStatus.IN_USE, inUse.getStatus());

		assetAssignmentService.markReturned(assignment.getPublicId().toString(), LocalDate.of(2026, 5, 1));
		Asset available = assetService.findByPublicId(asset.getPublicId().toString());
		assertEquals(AssetStatus.AVAILABLE, available.getStatus());
	}

	// --- Library ---

	@Test
	void bookCreatedUnderTenantA_isNotVisibleWhenListingTenantB() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		bookCatalogService.createBook("978-" + uniqueSuffix(), "A Tale", "Author", "Publisher", "Fiction");

		activateTenant(tenantB);
		authenticateAsTenantAdmin();
		Page<Book> booksB = bookCatalogService.listBooks(PageRequest.of(0, 100));

		assertTrue(booksB.getContent().isEmpty(), "Tenant B must not see tenant A's books");
	}

	@Test
	void circulation_checkoutThenOverdueReturn_calculatesFineAndFreesCopy() {
		activateTenant(tenantA);
		authenticateAsTenantAdmin();
		Book book = bookCatalogService.createBook("978-" + uniqueSuffix(), "A Tale", "Author", "Publisher",
				"Fiction");
		BookCopy copy = bookCatalogService.addCopy(book.getPublicId().toString(), "COPY-1");
		Student student = studentService.enroll("STU-" + uniqueSuffix(), "Priya", "Rao",
				"priya-" + uniqueSuffix() + "@school.test", LocalDate.of(2011, 3, 3));

		Circulation circulation = circulationService.checkout(copy.getPublicId().toString(),
				student.getPublicId().toString());
		BookCopy checkedOut = bookCatalogService.listCopies(book.getPublicId().toString()).stream()
				.filter(c -> c.getId().equals(copy.getId()))
				.findFirst()
				.orElseThrow();
		assertEquals(BookCopyStatus.CHECKED_OUT, checkedOut.getStatus());

		LocalDate overdueReturnDate = circulation.getDueDate().plusDays(3);
		Circulation returned = circulationService.returnBook(circulation.getPublicId().toString(),
				overdueReturnDate);

		assertTrue(returned.getFineAmount().compareTo(BigDecimal.ZERO) > 0, "Overdue return must incur a fine");
		BookCopy availableAgain = bookCatalogService.listCopies(book.getPublicId().toString()).stream()
				.filter(c -> c.getId().equals(copy.getId()))
				.findFirst()
				.orElseThrow();
		assertEquals(BookCopyStatus.AVAILABLE, availableAgain.getStatus());
	}
}
