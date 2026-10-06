package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.EmergencyContactResponse;
import com.altafjava.school.domain.guardian.model.EmergencyContact;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EmergencyContactMapper {

	@Mapping(target = "publicId", expression = "java(contact.getPublicId().toString())")
	EmergencyContactResponse toResponse(EmergencyContact contact);
}
