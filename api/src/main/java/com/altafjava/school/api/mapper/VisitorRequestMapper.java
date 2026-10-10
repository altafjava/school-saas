package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.VisitorRequestResponse;
import com.altafjava.school.domain.visitor.model.VisitorRequest;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface VisitorRequestMapper {

	@Mapping(target = "publicId", expression = "java(request.getPublicId().toString())")
	@Mapping(target = "source", expression = "java(request.getSource().name())")
	@Mapping(target = "status", expression = "java(request.getStatus().name())")
	@Mapping(target = "photoFilePublicId", expression = "java(request.getPhotoFilePublicId() != null ? request.getPhotoFilePublicId().toString() : null)")
	@Mapping(target = "hostEmployeePublicId", source = "hostEmployeeId", qualifiedByName = "employee")
	@Mapping(target = "decidedByUserPublicId", source = "decidedByUserId", qualifiedByName = "user")
	VisitorRequestResponse toResponse(VisitorRequest request);
}
