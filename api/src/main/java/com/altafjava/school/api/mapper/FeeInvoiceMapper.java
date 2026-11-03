package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.invoice.FeeInvoiceResponse;
import com.altafjava.school.api.dto.response.invoice.InvoiceBatchResponse;
import com.altafjava.school.application.invoice.InvoiceBatchResult;
import com.altafjava.school.application.invoice.InvoiceView;
import com.altafjava.school.domain.fee.model.FeeInvoiceLine;

@Mapper(componentModel = "spring", uses = SchoolPublicIdMapping.class, unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface FeeInvoiceMapper {

	@Mapping(target = "publicId", expression = "java(view.invoice().getPublicId().toString())")
	@Mapping(target = "version", source = "invoice.version")
	@Mapping(target = "invoiceNumber", source = "invoice.invoiceNumber")
	@Mapping(target = "studentPublicId", source = "invoice.studentId", qualifiedByName = "student")
	@Mapping(target = "student", source = "invoice.studentId", qualifiedByName = "studentSummary")
	@Mapping(target = "periodStart", source = "invoice.periodStart")
	@Mapping(target = "periodEnd", source = "invoice.periodEnd")
	@Mapping(target = "issueDate", source = "invoice.issueDate")
	@Mapping(target = "dueDate", source = "invoice.dueDate")
	@Mapping(target = "totalAmount", source = "invoice.totalAmount")
	@Mapping(target = "paidAmount", source = "settlement.paid")
	@Mapping(target = "outstandingAmount", source = "settlement.outstanding")
	@Mapping(target = "settlement", source = "settlement.state")
	@Mapping(target = "status", source = "invoice.status")
	@Mapping(target = "cancelledAt", source = "invoice.cancelledAt")
	@Mapping(target = "cancellationReason", source = "invoice.cancellationReason")
	FeeInvoiceResponse toResponse(InvoiceView view);

	@Mapping(target = "feeStructurePublicId", source = "feeStructureId", qualifiedByName = "feeStructure")
	@Mapping(target = "feeStructure", source = "feeStructureId", qualifiedByName = "feeStructureSummary")
	FeeInvoiceResponse.Line toLine(FeeInvoiceLine line);

	InvoiceBatchResponse toResponse(InvoiceBatchResult result);

	InvoiceBatchResponse.Skipped toSkipped(InvoiceBatchResult.Skipped skipped);
}
