package com.altafjava.school.application.enquiry;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.service.AdmissionService;
import com.altafjava.school.domain.admission.model.Admission;
import com.altafjava.school.domain.common.model.Gender;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.enquiry.model.Enquiry;
import com.altafjava.school.domain.enquiry.model.EnquiryDetails;
import com.altafjava.school.domain.enquiry.model.EnquiryFollowUp;
import com.altafjava.school.domain.enquiry.model.EnquirySource;
import com.altafjava.school.domain.enquiry.model.EnquiryStatus;
import com.altafjava.school.domain.enquiry.model.FollowUpChannel;
import com.altafjava.school.domain.enquiry.repository.EnquiryFollowUpRepository;
import com.altafjava.school.domain.enquiry.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;

/**
 * Admission enquiries: received from any channel, assigned to staff, followed up, and either
 * converted into an admission application with the family's details carried over, or closed.
 */
@Service
@RequiredArgsConstructor
public class EnquiryService {

	private static final List<EnquiryStatus> OPEN = List.of(EnquiryStatus.NEW, EnquiryStatus.IN_PROGRESS);

	private final EnquiryRepository enquiryRepository;
	private final EnquiryFollowUpRepository enquiryFollowUpRepository;
	private final EmployeeRepository employeeRepository;
	private final AdmissionService admissionService;
	private final PublicIdLookup publicIdLookup;

	/** What converting adds to the enquiry: the date of birth when it was not given, and the gender. */
	public record Conversion(LocalDate childDateOfBirth, Gender childGender) {
	}

	@Transactional
	@Audited(action = AuditAction.CREATE, resourceType = "Enquiry", details = "Enquiry received")
	public Enquiry receive(EnquiryDetails details, EnquirySource source, String notes,
			String assignedEmployeePublicId) {
		Enquiry enquiry = Enquiry.receive(details, source, notes);
		if (assignedEmployeePublicId != null) {
			enquiry.assignTo(requireActiveEmployee(assignedEmployeePublicId).getId());
		}
		return enquiryRepository.save(enquiry);
	}

	@Transactional(readOnly = true)
	public Page<Enquiry> list(EnquiryFilter filter, Pageable pageable) {
		return enquiryRepository.search(TenantContext.getCurrentTenantId(), filter.status(), filter.source(),
				publicIdLookup.idOrNull(EntityRef.EMPLOYEE, filter.assignedEmployeePublicId()), filter.followUpDueBy(),
				OPEN, LikePattern.contains(filter.q()), pageable);
	}

	@Transactional(readOnly = true)
	public Enquiry find(String publicId) {
		return require(TenantContext.getCurrentTenantId(), publicId);
	}

	@Transactional
	public Enquiry assign(String publicId, String employeePublicId) {
		Enquiry enquiry = require(TenantContext.getCurrentTenantId(), publicId);
		enquiry.assignTo(requireActiveEmployee(employeePublicId).getId());
		return enquiryRepository.save(enquiry);
	}

	@Transactional
	public EnquiryFollowUp recordFollowUp(String publicId, FollowUpChannel channel, String note,
			LocalDate nextFollowUpOn, Long recordedByUserId) {
		Enquiry enquiry = require(TenantContext.getCurrentTenantId(), publicId);
		enquiry.recordFollowUp(nextFollowUpOn);
		enquiryRepository.save(enquiry);
		return enquiryFollowUpRepository
				.save(EnquiryFollowUp.record(enquiry.getId(), channel, note, nextFollowUpOn, recordedByUserId));
	}

	@Transactional(readOnly = true)
	public Page<EnquiryFollowUp> followUps(String publicId, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return enquiryFollowUpRepository.findByEnquiry(tenantId, require(tenantId, publicId).getId(), pageable);
	}

	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Enquiry", details = "Enquiry closed")
	public Enquiry close(String publicId, String reason) {
		Enquiry enquiry = require(TenantContext.getCurrentTenantId(), publicId);
		enquiry.close(reason);
		return enquiryRepository.save(enquiry);
	}

	@Transactional
	public Enquiry reopen(String publicId) {
		Enquiry enquiry = require(TenantContext.getCurrentTenantId(), publicId);
		enquiry.reopen();
		return enquiryRepository.save(enquiry);
	}

	/** Submits an admission application from the enquiry, so the family is not asked again. */
	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "Enquiry", details = "Enquiry converted to an application")
	public Enquiry convert(String publicId, Conversion conversion) {
		Enquiry enquiry = require(TenantContext.getCurrentTenantId(), publicId);
		if (!enquiry.getStatus().canMoveTo(EnquiryStatus.CONVERTED)) {
			throw new BusinessException("Only an open enquiry can be converted into an application");
		}
		LocalDate dateOfBirth = conversion.childDateOfBirth() != null ? conversion.childDateOfBirth()
				: enquiry.getChildDateOfBirth();
		Admission admission = admissionService.submit(enquiry.getChildFirstName(), enquiry.getChildLastName(),
				dateOfBirth, conversion.childGender(), enquiry.getGuardianFirstName(), enquiry.getGuardianLastName(),
				enquiry.getGuardianEmail(), enquiry.getGuardianPhone(), enquiry.getGradeSought());
		enquiry.convertedInto(admission.getId());
		return enquiryRepository.save(enquiry);
	}

	private Employee requireActiveEmployee(String employeePublicId) {
		Employee employee = employeeRepository
				.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), TenantContext.getCurrentTenantId())
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeePublicId));
		if (employee.getStatus() != EmployeeStatus.ACTIVE) {
			throw new BusinessException("Only an active staff member can follow up an enquiry");
		}
		return employee;
	}

	private Enquiry require(Long tenantId, String publicId) {
		return enquiryRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Enquiry not found: " + publicId));
	}
}
