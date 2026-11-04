package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
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
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.transport.model.Route;
import com.altafjava.school.domain.transport.model.RouteStop;
import com.altafjava.school.domain.transport.model.TransportAssignment;
import com.altafjava.school.domain.transport.model.Vehicle;
import com.altafjava.school.domain.transport.repository.RouteRepository;
import com.altafjava.school.domain.transport.repository.RouteStopRepository;
import com.altafjava.school.domain.transport.repository.TransportAssignmentRepository;
import com.altafjava.school.domain.transport.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class TransportAssignmentServiceTest {

	private static final UUID STUDENT_PUBLIC_ID = UUID.randomUUID();
	private static final UUID ROUTE_PUBLIC_ID = UUID.randomUUID();
	private static final UUID VEHICLE_PUBLIC_ID = UUID.randomUUID();
	private static final UUID STOP_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private TransportAssignmentRepository transportAssignmentRepository;
	@Mock
	private StudentRepository studentRepository;
	@Mock
	private RouteRepository routeRepository;
	@Mock
	private VehicleRepository vehicleRepository;
	@Mock
	private RouteStopRepository routeStopRepository;

	private TransportAssignmentService transportAssignmentService;

	@BeforeEach
	void setUp() {
		transportAssignmentService = new TransportAssignmentService(transportAssignmentRepository, studentRepository,
				routeRepository, vehicleRepository, routeStopRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	private Student studentWithId(long id) {
		Student student = Student.create("STU-1", "Alice", "Smith", "alice@school.test", null);
		student.setId(id);
		return student;
	}

	private Route routeWithId(long id) {
		Route route = Route.create("Route A", "RT-A", null);
		route.setId(id);
		return route;
	}

	private Vehicle vehicleWithId(long id) {
		Vehicle vehicle = Vehicle.create("KA-01-AB-1234", 40, "Ravi", "9999999999");
		vehicle.setId(id);
		return vehicle;
	}

	private RouteStop stopWithId(long id, long routeId) {
		RouteStop stop = RouteStop.create(routeId, "Main Gate", 1, null, null);
		stop.setId(id);
		return stop;
	}

	@Test
	void assign_withValidReferences_succeeds() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(routeRepository.findByPublicIdAndTenantId(ROUTE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(routeWithId(20L)));
		when(vehicleRepository.findByPublicIdAndTenantId(VEHICLE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(vehicleWithId(30L)));
		when(routeStopRepository.findByPublicIdAndTenantId(STOP_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(stopWithId(40L, 20L)));
		when(transportAssignmentRepository.existsByStudentIdAndTenantIdAndEffectiveToIsNull(10L, 1L))
				.thenReturn(false);
		when(transportAssignmentRepository.save(any(TransportAssignment.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		TransportAssignment assignment = assertDoesNotThrow(() -> transportAssignmentService.assign(
				STUDENT_PUBLIC_ID.toString(), ROUTE_PUBLIC_ID.toString(), VEHICLE_PUBLIC_ID.toString(),
				STOP_PUBLIC_ID.toString(), LocalDate.of(2026, 4, 1)));

		assertEquals(10L, assignment.getStudentId());
	}

	@Test
	void assign_stopNotBelongingToRoute_isNotFound() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(routeRepository.findByPublicIdAndTenantId(ROUTE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(routeWithId(20L)));
		when(vehicleRepository.findByPublicIdAndTenantId(VEHICLE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(vehicleWithId(30L)));
		when(routeStopRepository.findByPublicIdAndTenantId(STOP_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(stopWithId(40L, 999L)));

		assertThrows(ResourceNotFoundException.class, () -> transportAssignmentService.assign(STUDENT_PUBLIC_ID.toString(),
				ROUTE_PUBLIC_ID.toString(), VEHICLE_PUBLIC_ID.toString(), STOP_PUBLIC_ID.toString(),
				LocalDate.of(2026, 4, 1)));
	}

	@Test
	void assign_studentAlreadyHasActiveAssignment_throwsBusinessException() {
		when(studentRepository.findByPublicIdAndTenantId(STUDENT_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(studentWithId(10L)));
		when(routeRepository.findByPublicIdAndTenantId(ROUTE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(routeWithId(20L)));
		when(vehicleRepository.findByPublicIdAndTenantId(VEHICLE_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(vehicleWithId(30L)));
		when(routeStopRepository.findByPublicIdAndTenantId(STOP_PUBLIC_ID, 1L))
				.thenReturn(Optional.of(stopWithId(40L, 20L)));
		when(transportAssignmentRepository.existsByStudentIdAndTenantIdAndEffectiveToIsNull(10L, 1L))
				.thenReturn(true);

		assertThrows(BusinessException.class, () -> transportAssignmentService.assign(STUDENT_PUBLIC_ID.toString(),
				ROUTE_PUBLIC_ID.toString(), VEHICLE_PUBLIC_ID.toString(), STOP_PUBLIC_ID.toString(),
				LocalDate.of(2026, 4, 1)));
	}
}
