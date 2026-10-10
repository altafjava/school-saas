package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.CounselingReferralResponse;
import com.altafjava.school.domain.counseling.model.CounselingReferral;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CounselingReferralMapper {

	@Mapping(target = "publicId", expression = "java(referral.getPublicId().toString())")
	@Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")
	@Mapping(target = "referredByUserPublicId", source = "referredByUserId", qualifiedByName = "user")
	@Mapping(target = "counselingSessionPublicId", source = "counselingSessionId", qualifiedByName = "counselingSession")
	CounselingReferralResponse toResponse(CounselingReferral referral);
}
