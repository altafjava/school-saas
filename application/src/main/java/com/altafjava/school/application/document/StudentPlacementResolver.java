package com.altafjava.school.application.document;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.model.StudentClassroomLink;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.classroom.repository.StudentClassroomLinkRepository;
import lombok.RequiredArgsConstructor;

/**
 * A student's current classroom placement for printing on documents: the current academic year's
 * enrollment, else the most recent one. Shared by ID cards, certificates and report cards so all
 * three print the same class for the same student.
 */
@Component
@RequiredArgsConstructor
public class StudentPlacementResolver {

	public record Placement(StudentClassroomLink link, Optional<Classroom> classroom,
			Optional<AcademicYear> academicYear, List<StudentClassroomLink> allLinks) {

		public String classLabel() {
			return classroom.map(c -> c.getGrade() + " " + c.getSection()).orElse("");
		}
	}

	private final StudentClassroomLinkRepository studentClassroomLinkRepository;
	private final ClassroomRepository classroomRepository;
	private final AcademicYearRepository academicYearRepository;

	public Optional<Placement> resolve(Long tenantId, Long studentId) {
		List<StudentClassroomLink> links = studentClassroomLinkRepository.findByStudentId(tenantId, studentId);
		if (links.isEmpty()) {
			return Optional.empty();
		}
		Optional<Long> currentYearId = academicYearRepository.findByCurrentTrueAndTenantId(tenantId)
				.map(AcademicYear::getId);
		StudentClassroomLink current = currentYearId
				.flatMap(yearId -> links.stream().filter(link -> link.getAcademicYearId().equals(yearId)).findFirst())
				.orElseGet(() -> links.stream().max(Comparator.comparing(StudentClassroomLink::getEnrolledAt))
						.orElseThrow());
		return Optional.of(new Placement(current,
				classroomRepository.findByIdAndTenantId(current.getClassroomId(), tenantId),
				academicYearRepository.findByIdAndTenantId(current.getAcademicYearId(), tenantId), links));
	}
}
