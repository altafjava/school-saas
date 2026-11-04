package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.transport.model.Route;
import com.altafjava.school.domain.transport.model.RouteStop;
import com.altafjava.school.domain.transport.model.TransportAssignment;
import com.altafjava.school.domain.transport.model.Vehicle;
import com.altafjava.school.domain.transport.repository.RouteRepository;
import com.altafjava.school.domain.transport.repository.RouteStopRepository;
import com.altafjava.school.domain.transport.repository.TransportAssignmentRepository;
import com.altafjava.school.domain.transport.repository.VehicleRepository;

@Service
public class TransportAssignmentService {

	private final TransportAssignmentRepository transportAssignmentRepository;
	private final StudentRepository studentRepository;
	private final RouteRepository routeRepository;
	private final VehicleRepository vehicleRepository;
	private final RouteStopRepository routeStopRepository;

	public TransportAssignmentService(TransportAssignmentRepository transportAssignmentRepository,
			StudentRepository studentRepository, RouteRepository routeRepository, VehicleRepository vehicleRepository,
			RouteStopRepository routeStopRepository) {
		this.transportAssignmentRepository = transportAssignmentRepository;
		this.studentRepository = studentRepository;
		this.routeRepository = routeRepository;
		this.vehicleRepository = vehicleRepository;
		this.routeStopRepository = routeStopRepository;
	}

	@Transactional(readOnly = true)
	public Page<TransportAssignment> listForRoute(String routePublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Route route = routeRepository.findByPublicIdAndTenantId(UUID.fromString(routePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routePublicId));
		return transportAssignmentRepository.findAllByRouteIdAndTenantId(route.getId(), tenantId, pageable);
	}

	@Transactional
	public TransportAssignment assign(String studentPublicId, String routePublicId, String vehiclePublicId,
			String routeStopPublicId, LocalDate effectiveFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		Route route = routeRepository.findByPublicIdAndTenantId(UUID.fromString(routePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Route not found: " + routePublicId));
		Vehicle vehicle = vehicleRepository.findByPublicIdAndTenantId(UUID.fromString(vehiclePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehiclePublicId));
		RouteStop stop = routeStopRepository.findByPublicIdAndTenantId(UUID.fromString(routeStopPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Route stop not found: " + routeStopPublicId));
		if (!stop.getRouteId().equals(route.getId())) {
			throw new ResourceNotFoundException("Route stop " + routeStopPublicId + " does not belong to route "
					+ routePublicId);
		}
		if (transportAssignmentRepository.existsByStudentIdAndTenantIdAndEffectiveToIsNull(student.getId(),
				tenantId)) {
			throw new BusinessException(PlatformErrorCode.CONFLICT,
					"Student " + studentPublicId + " already has an active transport assignment");
		}
		TransportAssignment assignment = TransportAssignment.create(student.getId(), route.getId(), vehicle.getId(),
				stop.getId(), effectiveFrom);
		return transportAssignmentRepository.save(assignment);
	}

	@Transactional
	public TransportAssignment end(String publicId, LocalDate effectiveTo) {
		Long tenantId = TenantContext.getCurrentTenantId();
		TransportAssignment assignment = transportAssignmentRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Transport assignment not found: " + publicId));
		assignment.end(effectiveTo);
		return transportAssignmentRepository.save(assignment);
	}
}
