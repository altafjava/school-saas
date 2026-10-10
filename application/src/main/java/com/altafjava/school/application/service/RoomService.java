package com.altafjava.school.application.service;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.concurrency.ExpectedVersion;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.domain.hostel.model.HostelBuilding;
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.hostel.repository.HostelBuildingRepository;
import com.altafjava.school.domain.hostel.repository.RoomRepository;

@Service
public class RoomService {

	private final RoomRepository roomRepository;
	private final HostelBuildingRepository hostelBuildingRepository;

	public RoomService(RoomRepository roomRepository, HostelBuildingRepository hostelBuildingRepository) {
		this.roomRepository = roomRepository;
		this.hostelBuildingRepository = hostelBuildingRepository;
	}

	@Transactional(readOnly = true)
	public Page<Room> listForBuilding(String hostelBuildingPublicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		HostelBuilding building = hostelBuildingRepository
				.findByPublicIdAndTenantId(UUID.fromString(hostelBuildingPublicId), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Hostel building not found: " + hostelBuildingPublicId));
		return roomRepository.findAllByHostelBuildingIdAndTenantId(building.getId(), tenantId, pageable);
	}

	@Transactional(readOnly = true)
	public Room findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return roomRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Room not found: " + publicId));
	}

	@Transactional
	public Room create(String hostelBuildingPublicId, String roomNumber, int capacity) {
		Long tenantId = TenantContext.getCurrentTenantId();
		HostelBuilding building = hostelBuildingRepository
				.findByPublicIdAndTenantId(UUID.fromString(hostelBuildingPublicId), tenantId)
				.orElseThrow(
						() -> new ResourceNotFoundException("Hostel building not found: " + hostelBuildingPublicId));
		return roomRepository.save(Room.create(building.getId(), roomNumber, capacity));
	}

	@Transactional
	public Room updateDetails(String publicId, String roomNumber, int capacity, ExpectedVersion expectedVersion) {
		Room room = findByPublicId(publicId);
		expectedVersion.verify(room);
		room.updateDetails(roomNumber, capacity);
		return roomRepository.save(room);
	}

	@Transactional
	public Room deactivate(String publicId) {
		Room room = findByPublicId(publicId);
		room.deactivate();
		return roomRepository.save(room);
	}
}
