package com.altafjava.school.application.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.student.StudentPlacement;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import com.altafjava.school.domain.student.model.Student;

/** Resolves where students currently sit, for any number of students in three queries. */
@Service
public class StudentPlacementService {

	private final AcademicYearRepository academicYearRepository;
	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;

	public StudentPlacementService(AcademicYearRepository academicYearRepository,
			StudentClassroomLinkRepository studentClassroomLinkRepository, ClassroomRepository classroomRepository) {
		this.academicYearRepository = academicYearRepository;
		this.studentClassroomLinkRepository = studentClassroomLinkRepository;
		this.classroomRepository = classroomRepository;
	}

	/** Keyed by student id; a student without a place in the current academic year has no entry. */
	@Transactional(readOnly = true)
	public Map<Long, StudentPlacement> currentPlacements(Collection<Student> students) {
		if (students.isEmpty()) {
			return Map.of();
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		AcademicYear currentYear = academicYearRepository
				.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(tenantId).orElse(null);
		if (currentYear == null) {
			return Map.of();
		}
		List<Long> studentIds = students.stream().map(Student::getId).toList();
		List<StudentClassroomLink> links = studentClassroomLinkRepository
				.findByStudentIdsAndAcademicYearId(tenantId, studentIds, currentYear.getId());
		Map<Long, Classroom> classrooms = classroomRepository
				.findAllByIdInAndTenantId(links.stream().map(StudentClassroomLink::getClassroomId).distinct().toList(),
						tenantId)
				.stream().collect(Collectors.toMap(Classroom::getId, Function.identity()));
		return links.stream()
				.filter(link -> classrooms.containsKey(link.getClassroomId()))
				.collect(Collectors.toMap(StudentClassroomLink::getStudentId,
						link -> placement(link, classrooms.get(link.getClassroomId()), currentYear),
						(first, duplicate) -> first));
	}

	private static StudentPlacement placement(StudentClassroomLink link, Classroom classroom, AcademicYear year) {
		return new StudentPlacement(classroom.getPublicId().toString(),
				classroom.getGrade() + " " + classroom.getSection(), link.getRollNumber(),
				year.getPublicId().toString());
	}
}
