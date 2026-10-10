package com.altafjava.school.api.controller;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.platform.core.idempotency.RequireIdempotencyKey;
import com.altafjava.school.api.controller.api.StudentApi;
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
import com.altafjava.school.api.dto.response.StudentGuardianResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.mapper.AddressMapper;
import com.altafjava.school.api.mapper.AttendanceMapper;
import com.altafjava.school.api.mapper.AttendancePercentageMapper;
import com.altafjava.school.api.mapper.BulkImportMapper;
import com.altafjava.school.api.mapper.FeeBalanceMapper;
import com.altafjava.school.api.mapper.GradeMapper;
import com.altafjava.school.api.mapper.ReportCardMapper;
import com.altafjava.school.api.mapper.StudentGuardianMapper;
import com.altafjava.school.api.ratelimit.RateLimited;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.api.support.StudentResponseAssembler;
import com.altafjava.school.application.lifecycle.LifecycleChange;
import com.altafjava.school.application.service.AttendanceService;
import com.altafjava.school.application.service.FeePaymentService;
import com.altafjava.school.application.service.GpaResult;
import com.altafjava.school.application.service.GradeService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.ReportCardService;
import com.altafjava.school.application.service.StudentBulkImportService;
import com.altafjava.school.application.service.StudentGpaService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.domain.reportcard.model.ReportCard;
import com.altafjava.school.domain.student.model.EnrollmentStatus;
import com.altafjava.school.domain.student.model.Student;

@RestController
@RequestMapping("/api/v1/students")
public class StudentController implements StudentApi {

	private final StudentService studentService;
	private final StudentResponseAssembler studentResponseAssembler;
	private final GuardianService guardianService;
	private final StudentGuardianMapper studentGuardianMapper;
	private final AddressMapper addressMapper;
	private final GradeService gradeService;
	private final GradeMapper gradeMapper;
	private final AttendanceService attendanceService;
	private final AttendanceMapper attendanceMapper;
	private final AttendancePercentageMapper attendancePercentageMapper;
	private final FeePaymentService feePaymentService;
	private final FeeBalanceMapper feeBalanceMapper;
	private final ReportCardService reportCardService;
	private final ReportCardMapper reportCardMapper;
	private final TermService termService;
	private final StudentBulkImportService studentBulkImportService;
	private final BulkImportMapper bulkImportMapper;
	private final StudentGpaService studentGpaService;

	private final SpringDataPageableResolver pageableResolver;

	public StudentController(StudentService studentService, StudentResponseAssembler studentResponseAssembler,
			GuardianService guardianService, StudentGuardianMapper studentGuardianMapper, AddressMapper addressMapper,
			GradeService gradeService,
			GradeMapper gradeMapper, AttendanceService attendanceService, AttendanceMapper attendanceMapper,
			AttendancePercentageMapper attendancePercentageMapper, FeePaymentService feePaymentService,
			FeeBalanceMapper feeBalanceMapper, ReportCardService reportCardService, ReportCardMapper reportCardMapper,
			TermService termService, StudentBulkImportService studentBulkImportService,
			BulkImportMapper bulkImportMapper, StudentGpaService studentGpaService,
			SpringDataPageableResolver pageableResolver) {
		this.studentService = studentService;
		this.studentResponseAssembler = studentResponseAssembler;
		this.guardianService = guardianService;
		this.studentGuardianMapper = studentGuardianMapper;
		this.addressMapper = addressMapper;
		this.gradeService = gradeService;
		this.gradeMapper = gradeMapper;
		this.attendanceService = attendanceService;
		this.attendanceMapper = attendanceMapper;
		this.attendancePercentageMapper = attendancePercentageMapper;
		this.feePaymentService = feePaymentService;
		this.feeBalanceMapper = feeBalanceMapper;
		this.reportCardService = reportCardService;
		this.reportCardMapper = reportCardMapper;
		this.termService = termService;
		this.studentBulkImportService = studentBulkImportService;
		this.bulkImportMapper = bulkImportMapper;
		this.studentGpaService = studentGpaService;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_READ')")
	@SortableBy({ "studentCode", "firstName", "lastName", "dateOfBirth", "enrollmentStatus" })
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) EnrollmentStatus status,
			@RequestParam(required = false) String classroomPublicId,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(PlatformPageMapper.toPlatformPage(studentResponseAssembler.toResponses(
				studentService.searchStudents(pageableResolver.resolve(page, size), status, classroomPublicId, q))));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_READ')")
	public ApiResponse<StudentResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(studentResponseAssembler.toResponse(studentService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping("/bulk-import")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	@RateLimited(key = "student-bulk-import", capacity = 5, periodMinutes = 60)
	@RequireIdempotencyKey
	public ApiResponse<BulkImportResponse> bulkImport(@RequestParam("file") MultipartFile file) {
		try (var inputStream = file.getInputStream()) {
			return ApiResponse.success(bulkImportMapper.toResponse(studentBulkImportService.importCsv(inputStream)));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to read uploaded CSV file", e);
		}
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> enroll(@Valid @RequestBody CreateStudentRequest request) {
		Student student = studentService.enroll(
				request.studentCode(),
				request.firstName(),
				request.lastName(),
				request.email(),
				request.dateOfBirth(),
				request.gender(),
				null);
		return ApiResponse.success(studentResponseAssembler.toResponse(student));
	}

	@Override
	@PatchMapping("/{publicId}/withdraw")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> withdraw(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request) {
		return ApiResponse
				.success(studentResponseAssembler.toResponse(studentService.withdraw(publicId, toChange(request))));
	}

	@Override
	@PatchMapping("/{publicId}/transfer")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> transfer(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request) {
		return ApiResponse
				.success(studentResponseAssembler.toResponse(studentService.transfer(publicId, toChange(request))));
	}

	@Override
	@PatchMapping("/{publicId}/graduate")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> graduate(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request) {
		return ApiResponse
				.success(studentResponseAssembler.toResponse(studentService.graduate(publicId, toChange(request))));
	}

	@Override
	@PatchMapping("/{publicId}/suspend")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> suspend(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request) {
		return ApiResponse
				.success(studentResponseAssembler.toResponse(studentService.suspend(publicId, toChange(request))));
	}

	@Override
	@PatchMapping("/{publicId}/reinstate")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> reinstate(@PathVariable String publicId,
			@RequestBody(required = false) @Valid LifecycleChangeRequest request) {
		return ApiResponse
				.success(studentResponseAssembler.toResponse(studentService.reinstate(publicId, toChange(request))));
	}

	private static LifecycleChange toChange(LifecycleChangeRequest request) {
		return request == null ? LifecycleChange.NONE : new LifecycleChange(request.reason(), request.effectiveOn());
	}

	@Override
	@PatchMapping("/{publicId}/contact-details")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> updateContactDetails(@PathVariable String publicId,
			@Valid @RequestBody UpdateStudentContactDetailsRequest request) {
		return ApiResponse
				.success(studentResponseAssembler
						.toResponse(studentService.updateContactDetails(publicId, request.firstName(),
								request.lastName(), request.email(), request.dateOfBirth(), request.gender(),
								request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/phone")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> updatePhone(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhoneRequest request) {
		return ApiResponse.success(studentResponseAssembler
				.toResponse(studentService.updatePhone(publicId, request.phone(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/address")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> updateAddress(@PathVariable String publicId,
			@Valid @RequestBody AddressRequest request) {
		return ApiResponse.success(
				studentResponseAssembler
						.toResponse(studentService.updateAddress(publicId, addressMapper.toDomain(request),
								request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/photo")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	public ApiResponse<StudentResponse> updatePhoto(@PathVariable String publicId,
			@Valid @RequestBody UpdatePhotoRequest request) {
		return ApiResponse
				.success(studentResponseAssembler.toResponse(
						studentService.updatePhoto(publicId, request.filePublicId(), request.expectedVersion())));
	}

	@Override
	@GetMapping("/{publicId}/guardians")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_READ')")
	public ApiResponse<List<StudentGuardianResponse>> guardians(@PathVariable String publicId) {
		return ApiResponse.success(studentGuardianMapper.toResponses(guardianService.listGuardiansOfStudent(publicId)));
	}

	@Override
	@GetMapping("/{publicId}/grades")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<GradeResponse>> grades(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(gradeService.getStudentGrades(publicId, pageableResolver.resolve(page, size))
						.map(gradeMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}/gpa/term")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<GpaResponse> termGpa(@PathVariable String publicId, @RequestParam String termPublicId) {
		return ApiResponse.success(toGpaResponse(studentGpaService.calculateTermGpa(publicId, termPublicId)));
	}

	@Override
	@GetMapping("/{publicId}/gpa/academic-year")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<GpaResponse> academicYearGpa(@PathVariable String publicId,
			@RequestParam String academicYearPublicId) {
		return ApiResponse
				.success(toGpaResponse(studentGpaService.calculateAcademicYearGpa(publicId, academicYearPublicId)));
	}

	@Override
	@GetMapping("/{publicId}/gpa/cumulative")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<GpaResponse> cumulativeGpa(@PathVariable String publicId) {
		return ApiResponse.success(toGpaResponse(studentGpaService.calculateCumulativeGpa(publicId)));
	}

	private GpaResponse toGpaResponse(GpaResult result) {
		return new GpaResponse(result.gpa(), result.gradeCount());
	}

	@Override
	@GetMapping("/{publicId}/attendance")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<AttendanceResponse>> attendance(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(attendanceService.getStudentAttendance(publicId, pageableResolver.resolve(page, size))
						.map(attendanceMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}/attendance/percentage")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<AttendancePercentageResponse> attendancePercentage(@PathVariable String publicId,
			@RequestParam LocalDate fromDate,
			@RequestParam LocalDate toDate) {
		return ApiResponse.success(attendancePercentageMapper.toResponse(
				attendanceService.calculatePercentage(publicId, fromDate, toDate)));
	}

	@Override
	@GetMapping("/{publicId}/fee-balance")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_FEE_BALANCE_READ')")
	public ApiResponse<List<FeeBalanceResponse>> feeBalance(@PathVariable String publicId) {
		return ApiResponse.success(feeBalanceMapper.toResponseList(feePaymentService.calculateBalance(publicId)));
	}

	@Override
	@GetMapping("/{publicId}/report-cards")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<ReportCardResponse>> reportCards(
			@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(reportCardService.listForStudent(publicId, pageableResolver.resolve(page, size))
						.map(reportCardMapper::toResponse)));
	}

	@Override
	@PostMapping("/{publicId}/report-cards")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_MANAGE')")
	@RateLimited(key = "report-card-generate", capacity = 60, periodMinutes = 60)
	public ApiResponse<ReportCardResponse> generateReportCard(@PathVariable String publicId,
			@RequestParam String termPublicId,
			@RequestParam(required = false) String teacherRemarks,
			@RequestParam(required = false) String principalRemarks) {
		Student student = studentService.findByPublicId(publicId);
		var term = termService.findByPublicId(termPublicId);
		ReportCard reportCard = reportCardService.generate(student.getId(), term.getId(), teacherRemarks,
				principalRemarks);
		return ApiResponse.success(reportCardMapper.toResponse(reportCard));
	}

	// Not ApiResponse-wrapped, unlike every other endpoint in this codebase — a raw PDF download
	// needs its own Content-Type/Content-Disposition headers and binary body, not a JSON envelope.
	@Override
	@GetMapping("/{publicId}/report-cards/{reportCardPublicId}/download")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('STUDENT_SELF_SERVICE_READ')")
	public ResponseEntity<byte[]> downloadReportCard(@PathVariable String publicId,
			@PathVariable String reportCardPublicId) {
		ReportCard reportCard = reportCardService.findByPublicId(publicId, reportCardPublicId);
		byte[] pdf = reportCardService.downloadPdf(reportCard);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_PDF)
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(reportCardPublicId + ".pdf").build().toString())
				.body(pdf);
	}
}
