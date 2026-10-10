package com.altafjava.school.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.annotation.Command;
import com.altafjava.school.api.controller.api.ClassroomApi;
import com.altafjava.school.api.dto.request.AssignClassroomCurriculumRequest;
import com.altafjava.school.api.dto.request.AssignRollNumberRequest;
import com.altafjava.school.api.dto.request.CreateClassroomRequest;
import com.altafjava.school.api.dto.request.EnrollStudentInClassroomRequest;
import com.altafjava.school.api.dto.request.MoveClassroomAcademicYearRequest;
import com.altafjava.school.api.dto.request.ReassignClassTeacherRequest;
import com.altafjava.school.api.dto.request.UpdateClassroomCapacityRequest;
import com.altafjava.school.api.dto.response.ClassroomResponse;
import com.altafjava.school.api.dto.response.RollNumberResponse;
import com.altafjava.school.api.dto.response.StudentClassroomLinkResponse;
import com.altafjava.school.api.dto.response.StudentResponse;
import com.altafjava.school.api.dto.response.TimetableEntryResponse;
import com.altafjava.school.api.mapper.ClassroomMapper;
import com.altafjava.school.api.mapper.StudentClassroomLinkMapper;
import com.altafjava.school.api.mapper.StudentMapper;
import com.altafjava.school.api.mapper.TimetableEntryMapper;
import com.altafjava.school.api.support.PlatformPageMapper;
import com.altafjava.school.api.support.SortableBy;
import com.altafjava.school.api.support.SpringDataPageableResolver;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.TimetableService;

@RestController
@RequestMapping("/api/v1/classrooms")
public class ClassroomController implements ClassroomApi {

	private final ClassroomService classroomService;
	private final ClassroomMapper classroomMapper;
	private final TimetableService timetableService;
	private final TimetableEntryMapper timetableEntryMapper;
	private final StudentClassroomLinkMapper studentClassroomLinkMapper;
	private final StudentMapper studentMapper;

	private final SpringDataPageableResolver pageableResolver;

	public ClassroomController(ClassroomService classroomService, ClassroomMapper classroomMapper,
			TimetableService timetableService, TimetableEntryMapper timetableEntryMapper,
			StudentClassroomLinkMapper studentClassroomLinkMapper, StudentMapper studentMapper,
			SpringDataPageableResolver pageableResolver) {
		this.classroomService = classroomService;
		this.classroomMapper = classroomMapper;
		this.timetableService = timetableService;
		this.timetableEntryMapper = timetableEntryMapper;
		this.studentClassroomLinkMapper = studentClassroomLinkMapper;
		this.studentMapper = studentMapper;
		this.pageableResolver = pageableResolver;
	}

	@Override
	@GetMapping
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	@SortableBy({ "classCode", "grade", "section" })
	public ApiResponse<com.altafjava.platform.core.model.Page<ClassroomResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String q) {
		return ApiResponse.success(
				PlatformPageMapper
						.toPlatformPage(classroomService.searchClassrooms(pageableResolver.resolve(page, size), q)
								.map(classroomMapper::toResponse)));
	}

	@Override
	@GetMapping("/{publicId}")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public ApiResponse<ClassroomResponse> get(@PathVariable String publicId) {
		return ApiResponse.success(classroomMapper.toResponse(classroomService.findByPublicId(publicId)));
	}

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<ClassroomResponse> create(@Valid @RequestBody CreateClassroomRequest request) {
		return ApiResponse.success(classroomMapper.toResponse(classroomService.create(
				request.classCode(),
				request.grade(),
				request.section(),
				request.academicYearPublicId(),
				request.classTeacherPublicId())));
	}

	@Override
	@PatchMapping("/{publicId}/teacher")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<ClassroomResponse> reassignTeacher(@PathVariable String publicId,
			@Valid @RequestBody ReassignClassTeacherRequest request) {
		return ApiResponse
				.success(classroomMapper
						.toResponse(classroomService.reassignTeacher(publicId, request.teacherPublicId(),
								request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/academic-year")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<ClassroomResponse> moveToAcademicYear(@PathVariable String publicId,
			@Valid @RequestBody MoveClassroomAcademicYearRequest request) {
		return ApiResponse.success(classroomMapper.toResponse(
				classroomService.moveToAcademicYear(publicId, request.academicYearPublicId(),
						request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/capacity")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<ClassroomResponse> updateCapacity(@PathVariable String publicId,
			@Valid @RequestBody UpdateClassroomCapacityRequest request) {
		return ApiResponse
				.success(classroomMapper.toResponse(
						classroomService.updateCapacity(publicId, request.capacity(), request.expectedVersion())));
	}

	@Override
	@PatchMapping("/{publicId}/curriculum")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<ClassroomResponse> assignCurriculum(@PathVariable String publicId,
			@Valid @RequestBody AssignClassroomCurriculumRequest request) {
		return ApiResponse.success(
				classroomMapper.toResponse(classroomService.assignCurriculum(publicId, request.curriculumPublicId(),
						request.expectedVersion())));
	}

	@Override
	@GetMapping("/{publicId}/timetable")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public ApiResponse<List<TimetableEntryResponse>> timetable(@PathVariable String publicId) {
		return ApiResponse.success(timetableEntryMapper.toResponseList(timetableService.listForClassroom(publicId)));
	}

	@Override
	@PostMapping("/{publicId}/students")
	@ResponseStatus(HttpStatus.CREATED)
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<StudentClassroomLinkResponse> enrollStudent(@PathVariable String publicId,
			@Valid @RequestBody EnrollStudentInClassroomRequest request) {
		return ApiResponse.success(studentClassroomLinkMapper.toResponse(
				classroomService.enrollStudent(publicId, request.studentPublicId(), request.academicYearPublicId()),
				request.studentPublicId(), publicId, request.academicYearPublicId()));
	}

	@Override
	@GetMapping("/{publicId}/students")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_READ')")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentResponse>> roster(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return ApiResponse.success(PlatformPageMapper
				.toPlatformPage(classroomService.listRoster(publicId, pageableResolver.resolve(page, size))
						.map(studentMapper::toResponse)));
	}

	@Override
	@PatchMapping("/{publicId}/students/{studentPublicId}/withdraw")
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<Void> withdrawStudent(@PathVariable String publicId, @PathVariable String studentPublicId) {
		classroomService.withdrawStudentFromClassroom(publicId, studentPublicId);
		return ApiResponse.success(null);
	}

	@Override
	@PatchMapping("/{publicId}/students/{studentPublicId}/roll-number")
	@Command
	@PreAuthorize("@permissionAuthorizationService.hasPermission('CLASSROOM_WRITE')")
	public ApiResponse<RollNumberResponse> assignRollNumber(@PathVariable String publicId,
			@PathVariable String studentPublicId, @Valid @RequestBody AssignRollNumberRequest request) {
		var link = classroomService.assignRollNumber(publicId, studentPublicId, request.rollNumber());
		return ApiResponse.success(new RollNumberResponse(studentPublicId, publicId, link.getRollNumber()));
	}
}
