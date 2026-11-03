package com.altafjava.school.api.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdResolver;
import com.altafjava.school.application.reference.ReferenceSummaries;
import com.altafjava.school.application.reference.ReferenceSummary;
import lombok.RequiredArgsConstructor;

/**
 * Lets a mapper publish a reference as the public id of the record it points at, e.g.
 * {@code @Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")}.
 */
@Component
@RequiredArgsConstructor
public class SchoolPublicIdMapping {

	private final PublicIdResolver publicIdResolver;
	private final ReferenceSummaries referenceSummaries;

	@Named("academicYear")
	public String academicYear(Long id) {
		return publicIdResolver.resolve(EntityRef.ACADEMIC_YEAR, id);
	}

	@Named("asset")
	public String asset(Long id) {
		return publicIdResolver.resolve(EntityRef.ASSET, id);
	}

	@Named("assignment")
	public String assignment(Long id) {
		return publicIdResolver.resolve(EntityRef.ASSIGNMENT, id);
	}

	@Named("board")
	public String board(Long id) {
		return publicIdResolver.resolve(EntityRef.BOARD, id);
	}

	@Named("book")
	public String book(Long id) {
		return publicIdResolver.resolve(EntityRef.BOOK, id);
	}

	@Named("bookCopy")
	public String bookCopy(Long id) {
		return publicIdResolver.resolve(EntityRef.BOOK_COPY, id);
	}

	@Named("classroom")
	public String classroom(Long id) {
		return publicIdResolver.resolve(EntityRef.CLASSROOM, id);
	}

	@Named("counselingSession")
	public String counselingSession(Long id) {
		return publicIdResolver.resolve(EntityRef.COUNSELING_SESSION, id);
	}

	@Named("curriculum")
	public String curriculum(Long id) {
		return publicIdResolver.resolve(EntityRef.CURRICULUM, id);
	}

	@Named("department")
	public String department(Long id) {
		return publicIdResolver.resolve(EntityRef.DEPARTMENT, id);
	}

	@Named("employee")
	public String employee(Long id) {
		return publicIdResolver.resolve(EntityRef.EMPLOYEE, id);
	}

	@Named("event")
	public String event(Long id) {
		return publicIdResolver.resolve(EntityRef.EVENT, id);
	}

	@Named("exam")
	public String exam(Long id) {
		return publicIdResolver.resolve(EntityRef.EXAM, id);
	}

	@Named("examType")
	public String examType(Long id) {
		return publicIdResolver.resolve(EntityRef.EXAM_TYPE, id);
	}

	@Named("feeStructure")
	public String feeStructure(Long id) {
		return publicIdResolver.resolve(EntityRef.FEE_STRUCTURE, id);
	}

	@Named("gradingScale")
	public String gradingScale(Long id) {
		return publicIdResolver.resolve(EntityRef.GRADING_SCALE, id);
	}

	@Named("conversation")
	public String conversation(Long id) {
		return publicIdResolver.resolve(EntityRef.CONVERSATION, id);
	}

	@Named("conversationMessage")
	public String conversationMessage(Long id) {
		return publicIdResolver.resolve(EntityRef.CONVERSATION_MESSAGE, id);
	}

	@Named("guardian")
	public String guardian(Long id) {
		return publicIdResolver.resolve(EntityRef.GUARDIAN, id);
	}

	@Named("hostelBuilding")
	public String hostelBuilding(Long id) {
		return publicIdResolver.resolve(EntityRef.HOSTEL_BUILDING, id);
	}

	@Named("leaveType")
	public String leaveType(Long id) {
		return publicIdResolver.resolve(EntityRef.LEAVE_TYPE, id);
	}

	@Named("period")
	public String period(Long id) {
		return publicIdResolver.resolve(EntityRef.PERIOD, id);
	}

	@Named("room")
	public String room(Long id) {
		return publicIdResolver.resolve(EntityRef.ROOM, id);
	}

	@Named("route")
	public String route(Long id) {
		return publicIdResolver.resolve(EntityRef.ROUTE, id);
	}

	@Named("routeStop")
	public String routeStop(Long id) {
		return publicIdResolver.resolve(EntityRef.ROUTE_STOP, id);
	}

	@Named("student")
	public String student(Long id) {
		return publicIdResolver.resolve(EntityRef.STUDENT, id);
	}

	@Named("subject")
	public String subject(Long id) {
		return publicIdResolver.resolve(EntityRef.SUBJECT, id);
	}

	@Named("term")
	public String term(Long id) {
		return publicIdResolver.resolve(EntityRef.TERM, id);
	}

	@Named("timetableEntry")
	public String timetableEntry(Long id) {
		return publicIdResolver.resolve(EntityRef.TIMETABLE_ENTRY, id);
	}

	@Named("user")
	public String user(Long id) {
		return publicIdResolver.resolve(EntityRef.USER, id);
	}

	@Named("vehicle")
	public String vehicle(Long id) {
		return publicIdResolver.resolve(EntityRef.VEHICLE, id);
	}

	@Named("venue")
	public String venue(Long id) {
		return publicIdResolver.resolve(EntityRef.VENUE, id);
	}

	@Named("academicYearSummary")
	public ReferenceSummary academicYearSummary(Long id) {
		return referenceSummaries.of(EntityRef.ACADEMIC_YEAR, id);
	}

	@Named("assetSummary")
	public ReferenceSummary assetSummary(Long id) {
		return referenceSummaries.of(EntityRef.ASSET, id);
	}

	@Named("assignmentSummary")
	public ReferenceSummary assignmentSummary(Long id) {
		return referenceSummaries.of(EntityRef.ASSIGNMENT, id);
	}

	@Named("boardSummary")
	public ReferenceSummary boardSummary(Long id) {
		return referenceSummaries.of(EntityRef.BOARD, id);
	}

	@Named("bookSummary")
	public ReferenceSummary bookSummary(Long id) {
		return referenceSummaries.of(EntityRef.BOOK, id);
	}

	@Named("bookCopySummary")
	public ReferenceSummary bookCopySummary(Long id) {
		return referenceSummaries.of(EntityRef.BOOK_COPY, id);
	}

	@Named("classroomSummary")
	public ReferenceSummary classroomSummary(Long id) {
		return referenceSummaries.of(EntityRef.CLASSROOM, id);
	}

	@Named("curriculumSummary")
	public ReferenceSummary curriculumSummary(Long id) {
		return referenceSummaries.of(EntityRef.CURRICULUM, id);
	}

	@Named("departmentSummary")
	public ReferenceSummary departmentSummary(Long id) {
		return referenceSummaries.of(EntityRef.DEPARTMENT, id);
	}

	@Named("employeeSummary")
	public ReferenceSummary employeeSummary(Long id) {
		return referenceSummaries.of(EntityRef.EMPLOYEE, id);
	}

	@Named("eventSummary")
	public ReferenceSummary eventSummary(Long id) {
		return referenceSummaries.of(EntityRef.EVENT, id);
	}

	@Named("examSummary")
	public ReferenceSummary examSummary(Long id) {
		return referenceSummaries.of(EntityRef.EXAM, id);
	}

	@Named("examTypeSummary")
	public ReferenceSummary examTypeSummary(Long id) {
		return referenceSummaries.of(EntityRef.EXAM_TYPE, id);
	}

	@Named("feeStructureSummary")
	public ReferenceSummary feeStructureSummary(Long id) {
		return referenceSummaries.of(EntityRef.FEE_STRUCTURE, id);
	}

	@Named("gradingScaleSummary")
	public ReferenceSummary gradingScaleSummary(Long id) {
		return referenceSummaries.of(EntityRef.GRADING_SCALE, id);
	}

	@Named("guardianSummary")
	public ReferenceSummary guardianSummary(Long id) {
		return referenceSummaries.of(EntityRef.GUARDIAN, id);
	}

	@Named("hostelBuildingSummary")
	public ReferenceSummary hostelBuildingSummary(Long id) {
		return referenceSummaries.of(EntityRef.HOSTEL_BUILDING, id);
	}

	@Named("leaveTypeSummary")
	public ReferenceSummary leaveTypeSummary(Long id) {
		return referenceSummaries.of(EntityRef.LEAVE_TYPE, id);
	}

	@Named("periodSummary")
	public ReferenceSummary periodSummary(Long id) {
		return referenceSummaries.of(EntityRef.PERIOD, id);
	}

	@Named("roomSummary")
	public ReferenceSummary roomSummary(Long id) {
		return referenceSummaries.of(EntityRef.ROOM, id);
	}

	@Named("routeSummary")
	public ReferenceSummary routeSummary(Long id) {
		return referenceSummaries.of(EntityRef.ROUTE, id);
	}

	@Named("routeStopSummary")
	public ReferenceSummary routeStopSummary(Long id) {
		return referenceSummaries.of(EntityRef.ROUTE_STOP, id);
	}

	@Named("studentSummary")
	public ReferenceSummary studentSummary(Long id) {
		return referenceSummaries.of(EntityRef.STUDENT, id);
	}

	@Named("subjectSummary")
	public ReferenceSummary subjectSummary(Long id) {
		return referenceSummaries.of(EntityRef.SUBJECT, id);
	}

	@Named("termSummary")
	public ReferenceSummary termSummary(Long id) {
		return referenceSummaries.of(EntityRef.TERM, id);
	}

	@Named("userSummary")
	public ReferenceSummary userSummary(Long id) {
		return referenceSummaries.of(EntityRef.USER, id);
	}

	@Named("vehicleSummary")
	public ReferenceSummary vehicleSummary(Long id) {
		return referenceSummaries.of(EntityRef.VEHICLE, id);
	}

	@Named("venueSummary")
	public ReferenceSummary venueSummary(Long id) {
		return referenceSummaries.of(EntityRef.VENUE, id);
	}
}
