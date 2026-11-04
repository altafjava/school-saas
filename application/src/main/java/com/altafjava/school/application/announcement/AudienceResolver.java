package com.altafjava.school.application.announcement;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserStatus;
import com.altafjava.platform.domain.user.repository.RoleRepository;
import com.altafjava.platform.domain.user.repository.UserRepository;
import com.altafjava.school.domain.academicyear.model.AcademicYear;
import com.altafjava.school.domain.academicyear.repository.AcademicYearRepository;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.error.AcademicErrorCode;
import com.altafjava.school.domain.error.CommunicationErrorCode;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * Turns an announcement's audience into user accounts. Only people with a login are reachable, and
 * only currently enrolled students and their guardians count: past classroom links stay on file, so
 * classrooms are limited to the current academic year.
 */
@Component
@RequiredArgsConstructor
public class AudienceResolver {

	private final GuardianRepository guardianRepository;
	private final StudentRepository studentRepository;
	private final EmployeeRepository employeeRepository;
	private final ClassroomRepository classroomRepository;
	private final AcademicYearRepository academicYearRepository;
	private final RoleRepository roleRepository;
	private final UserRepository userRepository;

	public Set<Long> resolve(Long tenantId, Announcement announcement) {
		return switch (announcement.getAudienceType()) {
			case SCHOOL -> wholeSchool(tenantId, announcement.isIncludeStudents());
			case GRADE -> inClassrooms(tenantId, gradeClassrooms(tenantId, announcement.getTargets()),
					announcement.isIncludeStudents());
			case CLASSROOM -> inClassrooms(tenantId, targetClassrooms(tenantId, announcement.getTargets()),
					announcement.isIncludeStudents());
			case ROLE -> withRoles(tenantId, announcement.getTargets());
			case GUARDIANS -> Set.copyOf(guardianRepository.findUserIdsByPublicIds(tenantId,
					toPublicIds(announcement.getTargets())));
		};
	}

	/** Classrooms named by a CLASSROOM audience, all required to exist and be in the current year. */
	public List<Long> targetClassrooms(Long tenantId, Set<String> classroomPublicIds) {
		Long currentYear = currentYear(tenantId);
		List<Classroom> classrooms = classroomRepository.findAllByPublicIdInAndTenantId(
				toPublicIds(classroomPublicIds), tenantId);
		boolean allCurrent = classrooms.stream().allMatch(room -> currentYear.equals(room.getAcademicYearId()));
		if (classrooms.size() != classroomPublicIds.size() || !allCurrent) {
			throw new BusinessException(CommunicationErrorCode.AUDIENCE_CLASSROOM_NOT_CURRENT,
					"Choose classrooms of the current academic year");
		}
		return classrooms.stream().map(Classroom::getId).toList();
	}

	private Set<Long> wholeSchool(Long tenantId, boolean includeStudents) {
		Set<Long> users = new HashSet<>(guardianRepository.findUserIdsOfEnrolledStudents(tenantId));
		users.addAll(employeeRepository.findActiveUserIds(tenantId));
		if (includeStudents) {
			users.addAll(studentRepository.findEnrolledUserIds(tenantId));
		}
		return users;
	}

	private Set<Long> inClassrooms(Long tenantId, List<Long> classroomIds, boolean includeStudents) {
		if (classroomIds.isEmpty()) {
			return Set.of();
		}
		Set<Long> users = new HashSet<>(guardianRepository.findUserIdsByClassroomIds(tenantId, classroomIds));
		if (includeStudents) {
			users.addAll(studentRepository.findUserIdsByClassroomIds(tenantId, classroomIds));
		}
		return users;
	}

	private List<Long> gradeClassrooms(Long tenantId, Collection<String> grades) {
		return classroomRepository.findAllByGradeInAndAcademicYearIdAndTenantId(grades, currentYear(tenantId), tenantId)
				.stream().map(Classroom::getId).toList();
	}

	private Set<Long> withRoles(Long tenantId, Set<String> roleNames) {
		Set<Long> users = new HashSet<>();
		for (String roleName : roleNames) {
			var role = roleRepository.findByName(roleName)
					.orElseThrow(() -> new BusinessException(CommunicationErrorCode.AUDIENCE_ROLE_UNKNOWN,
							"Unknown role: " + roleName, Map.of("role", roleName)));
			userRepository.findAllByRoleId(role.getId()).stream()
					.filter(user -> tenantId.equals(user.getTenantId()) && user.getStatus() == UserStatus.ACTIVE)
					.map(User::getId)
					.forEach(users::add);
		}
		return users;
	}

	private Long currentYear(Long tenantId) {
		return academicYearRepository.findFirstByCurrentTrueAndTenantIdOrderByStartDateDesc(tenantId)
				.map(AcademicYear::getId)
				.orElseThrow(() -> new BusinessException(AcademicErrorCode.NO_CURRENT_ACADEMIC_YEAR,
						"The school has no current academic year"));
	}

	private List<UUID> toPublicIds(Collection<String> values) {
		try {
			return values.stream().map(UUID::fromString).toList();
		} catch (IllegalArgumentException e) {
			throw new BusinessException(PlatformErrorCode.INVALID_ARGUMENT, "An audience target is not a valid id");
		}
	}
}
