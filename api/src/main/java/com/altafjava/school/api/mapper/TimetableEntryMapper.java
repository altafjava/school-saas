package com.altafjava.school.api.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.domain.timetable.model.TimetableEntry;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TimetableEntryMapper {

	@Mapping(target = "publicId", expression = "java(entry.getPublicId().toString())")
	@Mapping(target = "dayOfWeek", expression = "java(entry.getDayOfWeek().name())")
	@Mapping(target = "periodPublicId", source = "periodId", qualifiedByName = "period")
	@Mapping(target = "classroomPublicId", source = "classroomId", qualifiedByName = "classroom")
	@Mapping(target = "subjectPublicId", source = "subjectId", qualifiedByName = "subject")
	@Mapping(target = "teacherPublicId", source = "teacherId", qualifiedByName = "employee")
	@Mapping(target = "venuePublicId", source = "venueId", qualifiedByName = "venue")
	TimetableEntryResponse toResponse(TimetableEntry entry);

	List<TimetableEntryResponse> toResponseList(List<TimetableEntry> entries);
}
