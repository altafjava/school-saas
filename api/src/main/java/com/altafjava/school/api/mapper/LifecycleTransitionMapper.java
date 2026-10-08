package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.LifecycleTransitionResponse;
import com.altafjava.school.domain.lifecycle.model.LifecycleTransition;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface LifecycleTransitionMapper {

	@Mapping(target = "publicId", expression = "java(transition.getPublicId().toString())")
	@Mapping(target = "fromStage", expression = "java(transition.getFromStage() == null ? null : transition.getFromStage().name())")
	@Mapping(target = "toStage", expression = "java(transition.getToStage().name())")
	@Mapping(target = "recordedAt", source = "createdAt")
	@Mapping(target = "recordedByUserId", source = "recordedByUserId")
	LifecycleTransitionResponse toResponse(LifecycleTransition transition);
}
