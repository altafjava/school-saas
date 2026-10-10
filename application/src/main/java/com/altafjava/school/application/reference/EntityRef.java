package com.altafjava.school.application.reference;

import jakarta.persistence.Table;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.counseling.model.CounselingSession;
import com.altafjava.school.domain.curriculum.model.Board;
import com.altafjava.school.domain.curriculum.model.Curriculum;
import com.altafjava.school.domain.curriculum.model.GradingScale;
import com.altafjava.school.domain.department.model.Department;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.event.model.Event;
import com.altafjava.school.domain.exam.model.Exam;
import com.altafjava.school.domain.exam.model.ExamTypeDefinition;
import com.altafjava.school.domain.fee.model.FeeStructure;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.hostel.model.HostelBuilding;
import com.altafjava.school.domain.hostel.model.Room;
import com.altafjava.school.domain.inventory.model.Asset;
import com.altafjava.school.domain.leave.model.LeaveType;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.lms.model.Assignment;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.subject.model.Subject;
import com.altafjava.school.domain.term.model.Term;
import com.altafjava.school.domain.timetable.model.Period;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.Venue;
import com.altafjava.school.domain.transport.model.Route;
import com.altafjava.school.domain.transport.model.RouteStop;
import com.altafjava.school.domain.transport.model.Vehicle;

/** The kinds of record an API response can point at; each maps to the table that holds its public id. */
public enum EntityRef {

	ACADEMIC_YEAR(AcademicYear.class), ASSET(Asset.class), ASSIGNMENT(Assignment.class), BOARD(Board.class), BOOK(
			Book.class), BOOK_COPY(BookCopy.class), CLASSROOM(Classroom.class), COUNSELING_SESSION(
					CounselingSession.class), CURRICULUM(Curriculum.class), DEPARTMENT(Department.class),
	// Teachers are employees (joined inheritance): same id, and the public id lives on the employee row.
	EMPLOYEE(Employee.class), EVENT(Event.class), EXAM(Exam.class), EXAM_TYPE(ExamTypeDefinition.class), FEE_STRUCTURE(
			FeeStructure.class), GRADING_SCALE(GradingScale.class), GUARDIAN(Guardian.class), HOSTEL_BUILDING(
					HostelBuilding.class), LEAVE_TYPE(LeaveType.class), PERIOD(Period.class), ROOM(Room.class), ROUTE(
							Route.class), ROUTE_STOP(RouteStop.class), STUDENT(Student.class), SUBJECT(
									Subject.class), TERM(Term.class), TIMETABLE_ENTRY(TimetableEntry.class), USER(
											User.class), VEHICLE(Vehicle.class), VENUE(Venue.class);

	private final String table;

	EntityRef(Class<?> entityClass) {
		this.table = entityClass.getAnnotation(Table.class).name();
	}

	String table() {
		return table;
	}
}
