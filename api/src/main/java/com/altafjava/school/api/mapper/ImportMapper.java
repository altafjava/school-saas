package com.altafjava.school.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.altafjava.school.api.dto.response.importing.ImportErrorResponse;
import com.altafjava.school.api.dto.response.importing.ImportFieldResponse;
import com.altafjava.school.api.dto.response.importing.ImportJobResponse;
import com.altafjava.school.api.dto.response.importing.ImportJobSummaryResponse;
import com.altafjava.school.api.dto.response.importing.ImportTypeResponse;
import com.altafjava.school.application.importing.ImportField;
import com.altafjava.school.application.importing.ImportJobView;
import com.altafjava.school.application.importing.ImportTypeDescription;
import com.altafjava.school.domain.importing.model.ImportJob;
import com.altafjava.school.domain.importing.model.ImportJobError;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ImportMapper {

	@Mapping(target = "publicId", expression = "java(view.job().getPublicId().toString())")
	@Mapping(target = "version", source = "job.version")
	@Mapping(target = "importType", source = "job.importType")
	@Mapping(target = "fileName", source = "job.fileName")
	@Mapping(target = "status", source = "job.status")
	@Mapping(target = "totalRows", source = "job.totalRows")
	@Mapping(target = "validRows", source = "job.validRows")
	@Mapping(target = "invalidRows", source = "job.invalidRows")
	@Mapping(target = "importedRows", source = "job.importedRows")
	@Mapping(target = "failedRows", source = "job.failedRows")
	@Mapping(target = "uploadedAt", source = "job.createdAt")
	@Mapping(target = "validatedAt", source = "job.validatedAt")
	@Mapping(target = "completedAt", source = "job.completedAt")
	ImportJobResponse toResponse(ImportJobView view);

	@Mapping(target = "publicId", expression = "java(job.getPublicId().toString())")
	@Mapping(target = "uploadedAt", source = "createdAt")
	ImportJobSummaryResponse toSummary(ImportJob job);

	ImportFieldResponse toResponse(ImportField field);

	ImportTypeResponse toResponse(ImportTypeDescription type);

	ImportErrorResponse toResponse(ImportJobError error);
}
