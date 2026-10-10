package com.altafjava.school.api.mapper;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdResolver;
import lombok.RequiredArgsConstructor;

/**
 * Lets a mapper publish a reference as the public id of the record it points at, e.g.
 * {@code @Mapping(target = "studentPublicId", source = "studentId", qualifiedByName = "student")}.
 */
@Component
@RequiredArgsConstructor
public class PublicIdMapping {

	private final PublicIdResolver publicIdResolver;

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
}
