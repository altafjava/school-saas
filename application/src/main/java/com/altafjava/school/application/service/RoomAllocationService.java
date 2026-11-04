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
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.hostel.model.RoomAllocation;
import com.altafjava.school.domain.hostel.repository.RoomAllocationRepository;
import com.altafjava.school.domain.hostel.repository.RoomRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;

@Service
public class RoomAllocationService {

	private final RoomAllocationRepository roomAllocationRepository;
	private final RoomRepository roomRepository;
	private final StudentRepository studentRepository;

	public RoomAllocationService(RoomAllocationRepository roomAllocationRepository, RoomRepository roomRepository,
			StudentRepository studentRepository) {
		this.roomAllocationRepository = roomAllocationRepository;
		this.roomRepository = roomRepository;
		this.studentRepository = studentRepository;
	}

	@Transactional(readOnly = true)
	public Page<RoomAllocation> listForRoom(String roomPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Room room = roomRepository.findByPublicIdAndTenantId(UUID.fromString(roomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomPublicId));
		return roomAllocationRepository.findAllByRoomIdAndTenantId(room.getId(), tenantId, pageable);
	}

	@Transactional
	public RoomAllocation allocate(String studentPublicId, String roomPublicId, LocalDate allocatedFrom) {
		Long tenantId = TenantContext.getCurrentTenantId();
		var student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		Room room = roomRepository.findByPublicIdAndTenantId(UUID.fromString(roomPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Room not found: " + roomPublicId));

		if (roomAllocationRepository.existsByStudentIdAndTenantIdAndAllocatedUntilIsNull(student.getId(), tenantId)) {
			throw new BusinessException(PlatformErrorCode.CONFLICT,
					"Student " + studentPublicId + " already has an active room allocation");
		}
		long occupantCount = roomAllocationRepository.countByRoomIdAndTenantIdAndAllocatedUntilIsNull(room.getId(),
				tenantId);
		if (occupantCount >= room.getCapacity()) {
			throw new BusinessException(PlatformErrorCode.CONFLICT, "Room " + roomPublicId + " is at full capacity");
		}

		RoomAllocation allocation = RoomAllocation.create(student.getId(), room.getId(), allocatedFrom);
		return roomAllocationRepository.save(allocation);
	}

	@Transactional
	public RoomAllocation vacate(String publicId, LocalDate allocatedUntil) {
		Long tenantId = TenantContext.getCurrentTenantId();
		RoomAllocation allocation = roomAllocationRepository
				.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Room allocation not found: " + publicId));
		allocation.vacate(allocatedUntil);
		return roomAllocationRepository.save(allocation);
	}
}
