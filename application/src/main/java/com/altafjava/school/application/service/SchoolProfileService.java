package com.altafjava.school.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.tenant.TenantContext;
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

	public record LinkedStudent(UUID publicId, String name, String studentCode) {
	}

	public record SchoolProfile(UUID studentId, UUID employeeId, boolean teacher, UUID guardianId,
			List<LinkedStudent> linkedStudents) {

		public SchoolProfile {
			linkedStudents = List.copyOf(linkedStudents);
		}
	}

	@Transactional(readOnly = true)
	public SchoolProfile forUser(Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		UUID studentId = studentRepository.findByUserIdAndTenantId(userId, tenantId).map(Student::getPublicId)
				.orElse(null);
		Optional<Employee> employee = employeeRepository.findByUserIdAndTenantId(userId, tenantId);
		boolean teacher = teacherRepository.findByUserIdAndTenantId(userId, tenantId).isPresent();
		Optional<Guardian> guardian = guardianRepository.findByUserIdAndTenantId(userId, tenantId);
		return new SchoolProfile(
				studentId,
				employee.map(Employee::getPublicId).orElse(null),
				teacher,
				guardian.map(Guardian::getPublicId).orElse(null),
				guardian.map(g -> linkedStudents(tenantId, g)).orElse(List.of()));
	}

	private List<LinkedStudent> linkedStudents(Long tenantId, Guardian guardian) {
		List<Long> studentIds = studentGuardianLinkRepository
				.findByGuardianId(tenantId, guardian.getId(), PageRequest.of(0, LINKED_STUDENTS_LIMIT)).getContent()
				.stream().map(StudentGuardianLink::getStudentId).toList();
		return studentRepository.findAllByIdInAndTenantId(studentIds, tenantId).stream()
				.map(s -> new LinkedStudent(s.getPublicId(), s.getFirstName() + " " + s.getLastName(),
						s.getStudentCode()))
				.toList();
	}
}
