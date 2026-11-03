package com.altafjava.school.application.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.student.StudentPlacement;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;

/** Which school records the signed-in user is: their student, staff, teacher and guardian records. */
@Service
@RequiredArgsConstructor
public class SchoolProfileService {

	/** A guardian is shown at most this many linked students; a family has few. */
	private static final int LINKED_STUDENTS_LIMIT = 50;

	private final StudentRepository studentRepository;
	private final EmployeeRepository employeeRepository;
	private final TeacherRepository teacherRepository;
	private final GuardianRepository guardianRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final StudentPlacementService studentPlacementService;

	public record LinkedStudent(UUID publicId, String name, String studentCode, StudentPlacement placement) {
	}

	public record SchoolProfile(UUID studentId, StudentPlacement studentPlacement, UUID employeeId, boolean teacher,
			UUID guardianId, List<LinkedStudent> linkedStudents) {

		public SchoolProfile {
			linkedStudents = List.copyOf(linkedStudents);
		}
	}

	@Transactional(readOnly = true)
	public SchoolProfile forUser(Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Optional<Student> student = studentRepository.findByUserIdAndTenantId(userId, tenantId);
		Optional<Employee> employee = employeeRepository.findByUserIdAndTenantId(userId, tenantId);
		boolean teacher = teacherRepository.findByUserIdAndTenantId(userId, tenantId).isPresent();
		Optional<Guardian> guardian = guardianRepository.findByUserIdAndTenantId(userId, tenantId);
		return new SchoolProfile(
				student.map(Student::getPublicId).orElse(null),
				student.map(this::placementOf).orElse(null),
				employee.map(Employee::getPublicId).orElse(null),
				teacher,
				guardian.map(Guardian::getPublicId).orElse(null),
				guardian.map(g -> linkedStudents(tenantId, g)).orElse(List.of()));
	}

	private List<LinkedStudent> linkedStudents(Long tenantId, Guardian guardian) {
		List<Long> studentIds = studentGuardianLinkRepository
				.findByGuardianId(tenantId, guardian.getId(), PageRequest.of(0, LINKED_STUDENTS_LIMIT)).getContent()
				.stream().filter(link -> !link.isCustodyRestricted()).map(StudentGuardianLink::getStudentId).toList();
		List<Student> students = studentRepository.findAllByIdInAndTenantId(studentIds, tenantId);
		Map<Long, StudentPlacement> placements = studentPlacementService.currentPlacements(students);
		return students.stream()
				.map(s -> new LinkedStudent(s.getPublicId(), s.getFirstName() + " " + s.getLastName(),
						s.getStudentCode(), placements.get(s.getId())))
				.toList();
	}

	private StudentPlacement placementOf(Student student) {
		return studentPlacementService.currentPlacements(List.of(student)).get(student.getId());
	}
}
