package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.VisitorLogResponse;
import com.altafjava.school.domain.visitor.model.VisitorLog;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VisitorLogMapper {

	@Mapping(target = "publicId", expression = "java(visitorLog.getPublicId().toString())")
	@Mapping(target = "photoFilePublicId", expression = "java(visitorLog.getPhotoFilePublicId() != null ? visitorLog.getPhotoFilePublicId().toString() : null)")
	@Mapping(target = "badgeIssued", expression = "java(visitorLog.getBadgeIssuanceId() != null)")
	@Mapping(target = "hostEmployeePublicId", source = "hostEmployeeId", qualifiedByName = "employee")
	VisitorLogResponse toResponse(VisitorLog visitorLog);
}
