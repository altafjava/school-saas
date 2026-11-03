package com.altafjava.school.api.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.application.security.annotation.RequiresPermission;
import com.altafjava.platform.core.annotation.LastWriteWins;
import com.altafjava.platform.core.feature.annotation.FeatureEnabled;
import com.altafjava.platform.core.security.AuthenticatedUser;
import com.altafjava.school.api.dto.request.UpdateImportMappingRequest;
import com.altafjava.school.api.dto.response.importing.ImportErrorResponse;
import com.altafjava.school.api.dto.response.importing.ImportJobResponse;
import com.altafjava.school.api.dto.response.importing.ImportJobSummaryResponse;
import com.altafjava.school.api.dto.response.importing.ImportTypeResponse;
import com.altafjava.school.api.mapper.ImportMapper;
import com.altafjava.school.api.ratelimit.RateLimited;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.importing.ImportService;
import com.altafjava.school.application.security.SchoolFeatures;
import com.altafjava.school.domain.importing.model.ImportJobStatus;
import com.altafjava.school.domain.importing.model.ImportPhase;
import com.altafjava.school.domain.importing.model.ImportType;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Bulk import", description = "CSV imports in steps: upload, map columns, dry run, import, with history and "
		+ "an error report. Each type also needs the permission to create that kind of record.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
@RestController
@RequestMapping("/api/v1/imports")
@RequiredArgsConstructor
public class ImportController {

	private final ImportService importService;
	private final ImportMapper importMapper;
	private final SpringDataPageableResolver pageableResolver;

	@GetMapping("/types")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@Operation(summary = "Import types", description = "What can be imported, and the fields each row carries.")
	public ApiResponse<List<ImportTypeResponse>> types() {
		return ApiResponse.success(importService.types().stream().map(importMapper::toResponse).toList());
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@RateLimited(key = "bulk-import-upload", capacity = 20, periodMinutes = 60)
	@Operation(summary = "Upload a file", description = "A UTF-8 CSV whose first line names the columns, up to 1 MB and "
			+ "5000 rows. Columns whose names match a field are mapped already.")
	public ApiResponse<ImportJobResponse> upload(@RequestParam ImportType type, @RequestPart("file") MultipartFile file,
			@AuthenticationPrincipal AuthenticatedUser user) {
		try {
			return ApiResponse.success(importMapper.toResponse(
					importService.upload(type, file.getOriginalFilename(), file.getBytes(), user.getId())));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read the uploaded file", e);
		}
	}

	@GetMapping
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@SortableBy({ "createdAt", "importType", "status" })
	@Operation(summary = "Import history")
	public ApiResponse<com.altafjava.platform.core.model.Page<ImportJobSummaryResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) ImportType type,
			@RequestParam(required = false) ImportJobStatus status) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(
				importService.list(type, status, pageableResolver.resolve(page, size)).map(importMapper::toSummary)));
	}

	@GetMapping("/{publicId}")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@Operation(summary = "Import")
	public ApiResponse<ImportJobResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(importMapper.toResponse(importService.find(publicId)));
	}

	@PutMapping("/{publicId}/mapping")
	@LastWriteWins("The mapping is replaced whole, and the job's import step is claimed under a version check")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@Operation(summary = "Map columns", description = "Replaces the mapping; any earlier dry run no longer counts.")
	public ApiResponse<ImportJobResponse> remap(@PathVariable String publicId,
			@Valid @RequestBody UpdateImportMappingRequest request) {
		return ApiResponse.success(importMapper.toResponse(importService.remap(publicId, request.mapping())));
	}

	@PostMapping("/{publicId}/dry-run")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@Operation(summary = "Dry run", description = "Checks every row as the import would, writes nothing, and keeps the "
			+ "error report.")
	public ApiResponse<ImportJobResponse> dryRun(@PathVariable String publicId) {
		return ApiResponse.success(importMapper.toResponse(importService.dryRun(publicId)));
	}

	@PostMapping("/{publicId}/execute")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@Operation(summary = "Import", description = "Imports the file once, in batches of 100 rows that each go in whole or "
			+ "not at all. Rows the last dry run rejected are skipped.")
	public ApiResponse<ImportJobResponse> execute(@PathVariable String publicId) {
		return ApiResponse.success(importMapper.toResponse(importService.execute(publicId)));
	}

	@GetMapping("/{publicId}/errors")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@SortableBy({ "lineNumber" })
	@Operation(summary = "Row errors", description = "The latest report: the import's once it has run, otherwise the "
			+ "dry run's. The phase picks one.")
	public ApiResponse<com.altafjava.platform.core.model.Page<ImportErrorResponse>> errors(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) ImportPhase phase) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(importService
				.errors(publicId, phase, pageableResolver.resolve(page, size)).map(importMapper::toResponse)));
	}

	@GetMapping("/{publicId}/error-report")
	@FeatureEnabled(SchoolFeatures.BULK_IMPORT)
	@RequiresPermission(SchoolPermissions.BULK_IMPORT_MANAGE)
	@Operation(summary = "Error report", description = "The latest row errors as CSV.")
	public ResponseEntity<byte[]> errorReport(@PathVariable String publicId) {
		return ResponseEntity.ok()
				.contentType(new MediaType("text", "csv"))
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename("import-errors-" + publicId + ".csv").build()
								.toString())
				.body(importService.errorReport(publicId));
	}
}
