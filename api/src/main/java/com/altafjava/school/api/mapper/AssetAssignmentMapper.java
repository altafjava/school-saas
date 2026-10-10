package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import com.altafjava.school.api.dto.response.AssetAssignmentResponse;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdResolver;
import com.altafjava.school.domain.inventory.model.AssetAssignment;
import com.altafjava.school.domain.inventory.model.AssignedToType;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public abstract class AssetAssignmentMapper {

	@Autowired
	protected PublicIdResolver publicIdResolver;

	@Mapping(target = "publicId", expression = "java(assignment.getPublicId().toString())")
	@Mapping(target = "assignedToType", expression = "java(assignment.getAssignedToType().name())")
	@Mapping(target = "assignedToPublicId", expression = "java(assignedToPublicId(assignment))")
	@Mapping(target = "assetPublicId", source = "assetId", qualifiedByName = "asset")
	public abstract AssetAssignmentResponse toResponse(AssetAssignment assignment);

	// The holder is a staff member or a classroom, so the id is read against the matching table.
	protected String assignedToPublicId(AssetAssignment assignment) {
		EntityRef holder = assignment.getAssignedToType() == AssignedToType.STAFF ? EntityRef.EMPLOYEE
				: EntityRef.CLASSROOM;
		return publicIdResolver.resolve(holder, assignment.getAssignedToId());
	}
}
