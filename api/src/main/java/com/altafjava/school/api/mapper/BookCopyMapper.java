package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.BookCopyResponse;
import com.altafjava.school.domain.library.model.BookCopy;

@Mapper(componentModel = "spring", uses = PublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface BookCopyMapper {

	@Mapping(target = "publicId", expression = "java(bookCopy.getPublicId().toString())")
	@Mapping(target = "status", expression = "java(bookCopy.getStatus().name())")
	@Mapping(target = "bookPublicId", source = "bookId", qualifiedByName = "book")
	BookCopyResponse toResponse(BookCopy bookCopy);
}
