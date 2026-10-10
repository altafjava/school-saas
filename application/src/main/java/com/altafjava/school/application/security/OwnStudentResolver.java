package com.altafjava.school.application.security;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import com.altafjava.school.domain.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;

/**
 * The students a user may see as their own: the user's own student record, and the children a
 * guardian is linked to. A custody-restricted link stays on file but grants no access.
 */
@Component
@RequiredArgsConstructor
public class OwnStudentResolver {

	private final StudentRepository studentRepository;
	private final GuardianRepository guardianRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;

	public Set<Long> forUser(Long userId, Long tenantId) {
		if (userId == null) {
			return Set.of();
		}
		Set<Long> studentIds = new HashSet<>();
		studentRepository.findByUserIdAndTenantId(userId, tenantId)
				.ifPresent(student -> studentIds.add(student.getId()));
		guardianRepository.findByUserIdAndTenantId(userId, tenantId)
				.ifPresent(guardian -> studentGuardianLinkRepository
						.findAllByGuardianIdAndTenantId(guardian.getId(), tenantId).stream()
						.filter(link -> !link.isCustodyRestricted())
						.forEach(link -> studentIds.add(link.getStudentId())));
		return Set.copyOf(studentIds);
	}
}
