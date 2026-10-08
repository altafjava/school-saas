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
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.timetable.model.VenueType;
import com.altafjava.school.domain.timetable.repository.VenueRepository;

@ExtendWith(MockitoExtension.class)
class VenueServiceTest {

	@Mock
	private VenueRepository venueRepository;

	private VenueService venueService;

	@BeforeEach
	void setUp() {
		venueService = new VenueService(venueRepository);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void create_withANewCode_savesTheVenue() {
		when(venueRepository.existsByCodeAndTenantId("LAB-1", 1L)).thenReturn(false);
		when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

		Venue venue = venueService.create("LAB-1", "Physics Lab", VenueType.LABORATORY, 30);

		assertEquals("LAB-1", venue.getCode());
	}

	@Test
	void create_withATakenCode_throwsBusinessException() {
		when(venueRepository.existsByCodeAndTenantId("LAB-1", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> venueService.create("LAB-1", "Lab", VenueType.LABORATORY, 30));

		verify(venueRepository, never()).save(any());
	}

	@Test
	void deactivate_marksTheVenueInactive() {
		UUID publicId = UUID.randomUUID();
		Venue venue = Venue.create("R-1", "Room 1", VenueType.CLASSROOM, 40);
		when(venueRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.of(venue));
		when(venueRepository.save(any(Venue.class))).thenAnswer(inv -> inv.getArgument(0));

		assertFalse(venueService.deactivate(publicId.toString()).isActive());
	}

	@Test
	void findByPublicId_unknown_throwsResourceNotFound() {
		UUID publicId = UUID.randomUUID();
		when(venueRepository.findByPublicIdAndTenantId(publicId, 1L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> venueService.findByPublicId(publicId.toString()));
	}
}
