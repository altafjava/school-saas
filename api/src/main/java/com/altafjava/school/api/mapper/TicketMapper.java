package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TicketResponse;
import com.altafjava.school.domain.helpdesk.model.Ticket;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TicketMapper {

	@Mapping(target = "publicId", expression = "java(ticket.getPublicId().toString())")
	@Mapping(target = "raisedByUserPublicId", source = "raisedByUserId", qualifiedByName = "user")
	@Mapping(target = "assignedToUserPublicId", source = "assignedToUserId", qualifiedByName = "user")
	TicketResponse toResponse(Ticket ticket);
}
