package com.altafjava.school.api.mapper;

import org.springframework.stereotype.Component;
import com.altafjava.school.api.dto.response.PickupAuthorizedGuardianResponse;
import com.altafjava.school.application.service.GuardianPickupService.PickupAuthorizedGuardian;

@Component
public class PickupAuthorizedGuardianMapper {

	public PickupAuthorizedGuardianResponse toResponse(PickupAuthorizedGuardian authorized) {
		var guardian = authorized.guardian();
		return new PickupAuthorizedGuardianResponse(
				guardian.getPublicId().toString(),
				guardian.getFirstName(),
				guardian.getLastName(),
				guardian.getPhone(),
				guardian.getPhotoFilePublicId() != null ? guardian.getPhotoFilePublicId().toString() : null,
				authorized.link().getRelationshipType().name());
	}
}
