package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.BookReservationResponse;
import com.altafjava.school.domain.library.model.BookReservation;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookReservationMapper {

	@Mapping(target = "publicId", expression = "java(reservation.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(reservation.getStatus().name())")
	BookReservationResponse toResponse(BookReservation reservation);
}
