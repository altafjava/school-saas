package com.altafjava.school.api.controller.api;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import com.altafjava.platform.api.dto.response.ApiResponse;
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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Classroom", description = "APIs for managing Classroom operations.\n\n**Tenant Scope**: All endpoints are tenant-scoped via X-Tenant-ID header.\n**Auth**: JWT Bearer token required on all endpoints unless marked public.")
@SecurityRequirement(name = "bearerAuth")
@SecurityRequirement(name = "tenantHeader")
public interface ClassroomApi {

	@Operation(summary = "List", operationId = "classroom_list")
	public ApiResponse<com.altafjava.platform.core.model.Page<ClassroomResponse>> list(
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Get", operationId = "classroom_get")
	public ApiResponse<ClassroomResponse> get(@PathVariable String publicId);

	@Operation(summary = "Create", operationId = "classroom_create")
	public ApiResponse<ClassroomResponse> create(@Valid @RequestBody CreateClassroomRequest request);

	@Operation(summary = "Reassign teacher", operationId = "classroom_reassignTeacher")
	public ApiResponse<ClassroomResponse> reassignTeacher(@PathVariable String publicId,
			@Valid @RequestBody ReassignClassTeacherRequest request);

	@Operation(summary = "Move to academic year", operationId = "classroom_moveToAcademicYear")
	public ApiResponse<ClassroomResponse> moveToAcademicYear(@PathVariable String publicId,
			@Valid @RequestBody MoveClassroomAcademicYearRequest request);

	@Operation(summary = "Update capacity", operationId = "classroom_updateCapacity")
	public ApiResponse<ClassroomResponse> updateCapacity(@PathVariable String publicId,
			@Valid @RequestBody UpdateClassroomCapacityRequest request);

	@Operation(summary = "Assign curriculum", operationId = "classroom_assignCurriculum")
	public ApiResponse<ClassroomResponse> assignCurriculum(@PathVariable String publicId,
			@Valid @RequestBody AssignClassroomCurriculumRequest request);

	@Operation(summary = "Timetable", operationId = "classroom_timetable")
	public ApiResponse<List<TimetableEntryResponse>> timetable(@PathVariable String publicId);

	@Operation(summary = "Enroll student", operationId = "classroom_enrollStudent")
	public ApiResponse<StudentClassroomLinkResponse> enrollStudent(@PathVariable String publicId,
			@Valid @RequestBody EnrollStudentInClassroomRequest request);

	@Operation(summary = "Roster", operationId = "classroom_roster")
	public ApiResponse<com.altafjava.platform.core.model.Page<StudentResponse>> roster(@PathVariable String publicId,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size);

	@Operation(summary = "Withdraw student", operationId = "classroom_withdrawStudent")
	public ApiResponse<Void> withdrawStudent(@PathVariable String publicId, @PathVariable String studentPublicId);

	@Operation(summary = "Assign roll number", operationId = "classroom_assignRollNumber")
	public ApiResponse<RollNumberResponse> assignRollNumber(
			@PathVariable String publicId, @PathVariable String studentPublicId,
			@Valid @RequestBody AssignRollNumberRequest request);
}
