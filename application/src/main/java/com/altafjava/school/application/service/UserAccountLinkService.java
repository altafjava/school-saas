package com.altafjava.school.application.service;

import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.service.UserDomainService;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.reference.PublicIdResolver;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.error.StaffErrorCode;
import com.altafjava.school.domain.error.StudentErrorCode;
import com.altafjava.school.domain.student.model.Student;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/** Gives a student or staff record a login, by creating one or linking one of the tenant's users. */
@Service
@RequiredArgsConstructor
public class UserAccountLinkService {

	// Admin roles are out of reach here, so holding a record-management permission cannot mint an administrator.
	private static final Set<String> STAFF_ROLES = Set.of(SchoolRoles.TEACHER, SchoolRoles.PRINCIPAL,
			SchoolRoles.FINANCE, SchoolRoles.HR, SchoolRoles.ACADEMIC);

	public record AccountRequest(String userPublicId, String email, String password, String role) {
	}

	public record LinkedAccount(String userPublicId, String email) {
	}

	private final StudentRepository studentRepository;
	private final EmployeeRepository employeeRepository;
	private final UserDomainService userDomainService;
	private final PublicIdLookup publicIdLookup;
	private final PublicIdResolver publicIdResolver;

	@Transactional
	public LinkedAccount linkStudent(String studentPublicId, AccountRequest request) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Student student = studentRepository.findByPublicIdAndTenantId(UUID.fromString(studentPublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentPublicId));
		assertNoAccount(student.getUserId());
		User user = resolveUser(tenantId, request, student.getFirstName(), student.getLastName(), SchoolRoles.STUDENT);
		student.setUserId(user.getId());
		studentRepository.save(student);
		return toLinkedAccount(user);
	}

	@Transactional
	public LinkedAccount linkEmployee(String employeePublicId, AccountRequest request) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = requireEmployee(tenantId, employeePublicId);
		return link(tenantId, employee, request, requireStaffRole(request.role()));
	}

	@Transactional
	public LinkedAccount linkTeacher(String teacherPublicId, AccountRequest request) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return link(tenantId, requireEmployee(tenantId, teacherPublicId), request, SchoolRoles.TEACHER);
	}

	private LinkedAccount link(Long tenantId, Employee employee, AccountRequest request, String role) {
		assertNoAccount(employee.getUserId());
		User user = resolveUser(tenantId, request, employee.getFirstName(), employee.getLastName(), role);
		employee.setUserId(user.getId());
		employeeRepository.save(employee);
		return toLinkedAccount(user);
	}

	private Employee requireEmployee(Long tenantId, String publicId) {
		return employeeRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + publicId));
	}

	private static String requireStaffRole(String role) {
		if (!STAFF_ROLES.contains(role)) {
			throw new BusinessException(StaffErrorCode.STAFF_ROLE_REQUIRED,
					"A staff login needs one of the roles " + STAFF_ROLES);
		}
		return role;
	}

	private static void assertNoAccount(Long userId) {
		if (userId != null) {
			throw new BusinessException(StudentErrorCode.ACCOUNT_EXISTS, "This record already has a login");
		}
	}

	private User resolveUser(Long tenantId, AccountRequest request, String firstName, String lastName, String role) {
		if (request.userPublicId() == null) {
			return userDomainService.createUser(tenantId, request.email(), request.password(), firstName, lastName,
					Set.of(role));
		}
		Long userId = publicIdLookup.idOrNull(EntityRef.USER, request.userPublicId());
		if (studentRepository.findByUserIdAndTenantId(userId, tenantId).isPresent()
				|| employeeRepository.findByUserIdAndTenantId(userId, tenantId).isPresent()) {
			throw new BusinessException(StudentErrorCode.ACCOUNT_LINKED_ELSEWHERE,
					"This login already belongs to another record");
		}
		return userDomainService.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.userPublicId()));
	}

	private LinkedAccount toLinkedAccount(User user) {
		return new LinkedAccount(publicIdResolver.resolve(EntityRef.USER, user.getId()), user.getEmail());
	}
}
