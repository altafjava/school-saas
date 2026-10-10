package com.altafjava.school.application.lookup;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import com.altafjava.platform.application.lookup.LookupOption;
import com.altafjava.platform.application.lookup.LookupProvider;
import com.altafjava.school.application.filter.TermFilter;
import com.altafjava.school.application.service.AcademicYearService;
import com.altafjava.school.application.service.ClassroomService;
import com.altafjava.school.application.service.DepartmentService;
import com.altafjava.school.application.service.EmployeeService;
import com.altafjava.school.application.service.FeeStructureService;
import com.altafjava.school.application.service.GuardianService;
import com.altafjava.school.application.service.LeaveTypeService;
import com.altafjava.school.application.service.StudentService;
import com.altafjava.school.application.service.SubjectService;
import com.altafjava.school.application.service.TeacherService;
import com.altafjava.school.application.service.TermService;
import com.altafjava.school.application.service.VenueService;

/** The school's dropdown/autocomplete lookups; each is guarded by the permission of its entity's list. */
@Configuration
public class SchoolLookupConfiguration {

	/** Upper bound on a reference catalog read for in-memory filtering. */
	private static final int CATALOG_LIMIT = 500;

	@Bean
	LookupProvider studentLookup(StudentService students) {
		return searching("students", "STUDENT_READ",
				(q, size) -> students.searchStudents(page(size, "firstName"), null, null, q),
				s -> new LookupOption(s.getPublicId().toString(), s.getFirstName() + " " + s.getLastName(),
						s.getStudentCode()));
	}

	@Bean
	LookupProvider guardianLookup(GuardianService guardians) {
		return searching("guardians", "GUARDIAN_MANAGE",
				(q, size) -> guardians.searchGuardians(page(size, "firstName"), q),
				g -> new LookupOption(g.getPublicId().toString(), g.getFirstName() + " " + g.getLastName(), null));
	}

	@Bean
	LookupProvider teacherLookup(TeacherService teachers) {
		return searching("teachers", "TEACHER_MANAGE",
				(q, size) -> teachers.searchTeachers(page(size, "firstName"), q),
				t -> new LookupOption(t.getPublicId().toString(), t.getFirstName() + " " + t.getLastName(),
						t.getEmployeeCode()));
	}

	@Bean
	LookupProvider employeeLookup(EmployeeService employees) {
		return searching("employees", "EMPLOYEE_READ",
				(q, size) -> employees.search(null, null, null, q, page(size, "firstName")),
				e -> new LookupOption(e.getPublicId().toString(), e.getFirstName() + " " + e.getLastName(),
						e.getEmployeeCode()));
	}

	@Bean
	LookupProvider classroomLookup(ClassroomService classrooms) {
		return searching("classrooms", "CLASSROOM_READ",
				(q, size) -> classrooms.searchClassrooms(page(size, "grade"), null, null, q),
				c -> new LookupOption(c.getPublicId().toString(), c.getGrade() + " " + c.getSection(),
						c.getClassCode()));
	}

	@Bean
	LookupProvider subjectLookup(SubjectService subjects) {
		return searching("subjects", "SUBJECT_READ", (q, size) -> subjects.searchSubjects(page(size, "name"), q),
				s -> new LookupOption(s.getPublicId().toString(), s.getName(), s.getCode()));
	}

	@Bean
	LookupProvider academicYearLookup(AcademicYearService academicYears) {
		return new CatalogLookupProvider<>("academic-years", "ACADEMIC_YEAR_READ",
				() -> academicYears.listAcademicYears(null, catalogPage()).getContent(),
				y -> LookupOption.of(y.getPublicId().toString(), y.getName()));
	}

	@Bean
	LookupProvider termLookup(TermService terms) {
		return new CatalogLookupProvider<>("terms", "TERM_READ",
				() -> terms.listTerms(TermFilter.NONE, catalogPage()).getContent(),
				t -> LookupOption.of(t.getPublicId().toString(), t.getName()));
	}

	@Bean
	LookupProvider departmentLookup(DepartmentService departments) {
		return new CatalogLookupProvider<>("departments", "DEPARTMENT_MANAGE",
				() -> departments.list(null, catalogPage()).getContent(),
				d -> new LookupOption(d.getPublicId().toString(), d.getName(), d.getCode()));
	}

	@Bean
	LookupProvider feeStructureLookup(FeeStructureService feeStructures) {
		return new CatalogLookupProvider<>("fee-structures", "FEE_STRUCTURE_MANAGE",
				() -> feeStructures.listFeeStructures(null, catalogPage()).getContent(),
				f -> LookupOption.of(f.getPublicId().toString(), f.getName()));
	}

	@Bean
	LookupProvider leaveTypeLookup(LeaveTypeService leaveTypes) {
		return new CatalogLookupProvider<>("leave-types", "LEAVE_TYPE_READ",
				() -> leaveTypes.list(null, catalogPage()).getContent(),
				l -> LookupOption.of(l.getPublicId().toString(), l.getName()));
	}

	@Bean
	LookupProvider venueLookup(VenueService venues) {
		return new CatalogLookupProvider<>("venues", "TIMETABLE_READ",
				() -> venues.list(null, catalogPage()).getContent(),
				v -> new LookupOption(v.getPublicId().toString(), v.getName(), v.getCode()));
	}

	private static Pageable page(int size, String sortProperty) {
		return PageRequest.of(0, size, Sort.by(sortProperty).ascending());
	}

	private static Pageable catalogPage() {
		return PageRequest.of(0, CATALOG_LIMIT);
	}

	private static <T> LookupProvider searching(String type, String permission,
			java.util.function.BiFunction<String, Integer, Page<T>> search, Function<T, LookupOption> toOption) {
		return new LookupProvider() {
			@Override
			public String type() {
				return type;
			}

			@Override
			public String requiredPermission() {
				return permission;
			}

			@Override
			public List<LookupOption> search(String query, int limit) {
				String q = query == null ? null : query.strip().toLowerCase(Locale.ROOT);
				return search.apply(q, limit).getContent().stream().map(toOption).toList();
			}
		};
	}
}
