package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.enquiry.EnquiryFollowUpResponse;
import com.altafjava.school.api.dto.response.enquiry.EnquiryResponse;
import com.altafjava.school.domain.enquiry.model.Enquiry;
import com.altafjava.school.domain.enquiry.model.EnquiryFollowUp;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface EnquiryMapper {

	@Mapping(target = "publicId", expression = "java(enquiry.getPublicId().toString())")
	@Mapping(target = "assignedEmployeePublicId", source = "assignedEmployeeId", qualifiedByName = "employee")
	@Mapping(target = "assignedEmployee", source = "assignedEmployeeId", qualifiedByName = "employeeSummary")
	@Mapping(target = "admissionPublicId", source = "admissionId", qualifiedByName = "admission")
	EnquiryResponse toResponse(Enquiry enquiry);

	@Mapping(target = "publicId", expression = "java(followUp.getPublicId().toString())")
	EnquiryFollowUpResponse toResponse(EnquiryFollowUp followUp);
}
