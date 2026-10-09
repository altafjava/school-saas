package com.altafjava.school.api.controller.api;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.school.api.dto.request.AddressRequest;
import com.altafjava.school.api.dto.request.CreateStudentRequest;
import com.altafjava.school.api.dto.request.LifecycleChangeRequest;
import com.altafjava.school.api.dto.request.UpdatePhoneRequest;
import com.altafjava.school.api.dto.request.UpdatePhotoRequest;
import com.altafjava.school.api.dto.request.UpdateStudentContactDetailsRequest;
import com.altafjava.school.api.dto.response.AttendancePercentageResponse;
import com.altafjava.school.api.dto.response.AttendanceResponse;
import com.altafjava.school.api.dto.response.BulkImportResponse;
import com.altafjava.school.api.dto.response.FeeBalanceResponse;
import com.altafjava.school.api.dto.response.GpaResponse;
import com.altafjava.school.api.dto.response.GradeResponse;
import com.altafjava.school.api.dto.response.ReportCardResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Student", description = "APIs for managing Student operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface StudentApi {

	@Operation(summary = "List")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) EnrollmentStatus status,
			@RequestParam(required = false) String q);

	@Operation(summary = "Get")
	public ApiResponse<StudentResponse> get(@PathVariable String publicId);

	@Operation(summary = "Bulk import")
	public ApiResponse<BulkImportResponse> bulkImport(@RequestParam("file") MultipartFile file);

	@Operation(summary = "Enroll")
	public ApiResponse<StudentResponse> enroll(@Valid @RequestBody CreateStudentRequest request);

	@Operation(summary = "Withdraw")
	public ApiResponse<StudentResponse> withdraw(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request);

	@Operation(summary = "Transfer")
	public ApiResponse<StudentResponse> transfer(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request);

	@Operation(summary = "Graduate")
	public ApiResponse<StudentResponse> graduate(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request);

	@Operation(summary = "Suspend")
	public ApiResponse<StudentResponse> suspend(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request);

	@Operation(summary = "Reinstate a suspended student")
	public ApiResponse<StudentResponse> reinstate(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request);

	@Operation(summary = "Update contact details")
	public ApiResponse<StudentResponse> updateContactDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateStudentContactDetailsRequest request);

	@Operation(summary = "Update phone")
	public ApiResponse<StudentResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request);

	@Operation(summary = "Update address")
	public ApiResponse<StudentResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request);

	@Operation(summary = "Update photo")
	public ApiResponse<StudentResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request);

	@Operation(summary = "Grades")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradeResponse>> grades(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Term gpa")
	public ApiResponse<GpaResponse> termGpa(@PathVariable String publicId, @RequestParam String termPublicId);

	@Operation(summary = "Academic year gpa")
	public ApiResponse<GpaResponse> academicYearGpa(@PathVariable String publicId,
			@RequestParam String academicYearPublicId);

	@Operation(summary = "Cumulative gpa")
	public ApiResponse<GpaResponse> cumulativeGpa(@PathVariable String publicId);

	@Operation(summary = "Attendance")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceResponse>> attendance(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Attendance percentage")
	public ApiResponse<AttendancePercentageResponse> attendancePercentage(@PathVariable String publicId,
			@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate);

	@Operation(summary = "Fee balance")
	public ApiResponse<List<FeeBalanceResponse>> feeBalance(@PathVariable String publicId);

	@Operation(summary = "Report cards")
	public ApiResponse<com.altafjava.platform.core.model.Page<ReportCardResponse>> reportCards(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Generate report card")
	public ApiResponse<ReportCardResponse> generateReportCard(@PathVariable String publicId,
			@RequestParam String termPublicId,
			@RequestParam(required = false) String teacherRemarks,
			@RequestParam(required = false) String principalRemarks);

	@Operation(summary = "Download report card")
	public ResponseEntity<byte[]> downloadReportCard(@PathVariable String publicId,
			@PathVariable String reportCardPublicId);
}
