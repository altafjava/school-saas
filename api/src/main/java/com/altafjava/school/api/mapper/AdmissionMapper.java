package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.AdmissionResponse;
import com.altafjava.school.domain.admission.model.Admission;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface AdmissionMapper {

	@Mapping(target = "publicId", expression = "java(admission.getPublicId().toString())")
	@Mapping(target = "hasOfferLetter", expression = "java(admission.getOfferLetterIssuanceId() != null)")
	AdmissionResponse toResponse(Admission admission);
}
