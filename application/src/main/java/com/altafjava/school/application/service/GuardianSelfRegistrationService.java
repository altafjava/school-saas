package com.altafjava.school.application.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.IdentityErrorCode;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.service.UserDomainService;
import com.altafjava.school.application.security.SchoolRoles;
import com.altafjava.school.domain.common.service.PhoneNumberValidator;
import com.altafjava.school.domain.error.StudentErrorCode;
import com.altafjava.school.domain.guardian.model.Guardian;
import com.altafjava.school.domain.guardian.model.GuardianSelfRegistrationMode;
import com.altafjava.school.domain.guardian.model.StudentGuardianLink;
import com.altafjava.school.domain.guardian.repository.GuardianRepository;
import com.altafjava.school.domain.guardian.repository.StudentGuardianLinkRepository;
import lombok.RequiredArgsConstructor;

// Public, unauthenticated self-registration for guardians — dual mode per tenant, see
// GuardianRegistrationSettingsService. Deliberately does not extend GuardianService: this flow's
// zero-role vs. trusted-claim role decision is a security-sensitive concern of its own, not a
// variant of the admin-only GuardianService#create use case.
@Service
@RequiredArgsConstructor
public class GuardianSelfRegistrationService {

	private final GuardianRepository guardianRepository;
	private final StudentGuardianLinkRepository studentGuardianLinkRepository;
	private final GuardianRegistrationSettingsService guardianRegistrationSettingsService;
	private final UserDomainService userService;
	private final PhoneNumberValidator phoneNumberValidator = new PhoneNumberValidator();

	@Transactional
	public Guardian register(String email, String password, String firstName, String lastName, String phone) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Optional<Guardian> pendingGuardian = guardianRepository.findByEmailAndTenantIdAndUserIdIsNull(email,
				tenantId);
		if (pendingGuardian.isPresent()) {
			return claimPendingGuardian(pendingGuardian.get(), tenantId, email, password, firstName, lastName);
		}
		return createNewGuardian(tenantId, email, password, firstName, lastName, phone);
	}

	private Guardian claimPendingGuardian(Guardian guardian, Long tenantId, String email, String password,
			String firstName, String lastName) {
		// PARENT is granted immediately here — unlike AuthController.register()'s zero-role
		// convention — because this binds to a real, already-admin-created guardian/student record:
		// a trusted binding, not an anonymous signup, so immediate usable access is safe. Do not
		// "fix" this to zero roles to match AuthController; that would break the claim flow.
		User user = userService.createUser(tenantId, email, password, firstName, lastName,
				Set.of(SchoolRoles.PARENT));
		guardian.linkUserAccount(user.getId());
		Guardian saved = guardianRepository.save(guardian);
		// Completing self-registration is the guardian's explicit consent to the student link(s) an
		// admin already created for them — stamp consent on every existing link now, not just new ones.
		giveConsentOnExistingLinks(tenantId, saved.getId());
		return saved;
	}

	private void giveConsentOnExistingLinks(Long tenantId, Long guardianId) {
		List<StudentGuardianLink> links = studentGuardianLinkRepository.findAllByGuardianIdAndTenantId(guardianId,
				tenantId);
		links.forEach(StudentGuardianLink::giveConsent);
		studentGuardianLinkRepository.saveAll(links);
	}

	private Guardian createNewGuardian(Long tenantId, String email, String password, String firstName,
			String lastName, String phone) {
		GuardianSelfRegistrationMode mode = guardianRegistrationSettingsService.getMode(tenantId);
		if (mode == GuardianSelfRegistrationMode.CLAIM_ONLY) {
			throw new BusinessException(StudentErrorCode.GUARDIAN_NOT_INVITED,
					"No pending guardian record found for this email — contact the school office");
		}
		// Zero roles here, matching AuthController.register()'s convention exactly — unlike the
		// claim path above, there is no pre-existing trusted binding to a student; an admin must
		// separately link this guardian to a student and grant PARENT access.
		if (!phoneNumberValidator.isValid(phone, null)) {
			throw new BusinessException(IdentityErrorCode.PHONE_INVALID, "Invalid phone number: " + phone);
		}
		User user = userService.createUser(tenantId, email, password, firstName, lastName, Set.of());
		Guardian guardian = Guardian.create(firstName, lastName, email, phone, user.getId());
		return guardianRepository.save(guardian);
	}
}
