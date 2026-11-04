package com.altafjava.school.application.staffattendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.audit.AuditAction;
import com.altafjava.platform.core.audit.annotation.Audited;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.reference.EntityRef;
import com.altafjava.school.application.reference.PublicIdLookup;
import com.altafjava.school.application.time.SchoolClock;
import com.altafjava.school.domain.employee.model.Employee;
import com.altafjava.school.domain.employee.model.EmployeeStatus;
import com.altafjava.school.domain.employee.repository.EmployeeRepository;
import com.altafjava.school.domain.error.AttendanceErrorCode;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;
import com.altafjava.school.domain.leave.repository.LeaveRequestRepository;
import com.altafjava.school.domain.staffattendance.model.StaffAttendance;
import com.altafjava.school.domain.staffattendance.model.StaffAttendanceStatus;
import com.altafjava.school.domain.staffattendance.repository.StaffAttendanceRepository;
import lombok.RequiredArgsConstructor;

/**
 * Staff check in and out against the school's own clock, never a time they send; an administrator
 * records the days they could not. Approved leave and a working day exclude each other.
 */
@Service
@RequiredArgsConstructor
public class StaffAttendanceService {

	private final StaffAttendanceRepository staffAttendanceRepository;
	private final EmployeeRepository employeeRepository;
	private final LeaveRequestRepository leaveRequestRepository;
	private final StaffWorkdaySettingsService staffWorkdaySettingsService;
	private final SchoolClock schoolClock;
	private final PublicIdLookup publicIdLookup;

	@Transactional
	public StaffAttendance checkIn(Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = requireOwnRecord(tenantId, userId);
		LocalDateTime now = schoolNow(tenantId);
		requireNotOnLeave(tenantId, employee.getId(), now.toLocalDate());
		if (staffAttendanceRepository
				.findByEmployeeIdAndAttendanceDateAndTenantId(employee.getId(), now.toLocalDate(), tenantId)
				.isPresent()) {
			throw new BusinessException(AttendanceErrorCode.ATTENDANCE_ALREADY_RECORDED,
					"Today's attendance is already recorded");
		}
		return staffAttendanceRepository
				.save(StaffAttendance.checkIn(employee.getId(), now, staffWorkdaySettingsService.current()));
	}

	@Transactional
	public StaffAttendance checkOut(Long userId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = requireOwnRecord(tenantId, userId);
		LocalDateTime now = schoolNow(tenantId);
		StaffAttendance day = staffAttendanceRepository
				.findByEmployeeIdAndAttendanceDateAndTenantId(employee.getId(), now.toLocalDate(), tenantId)
				.orElseThrow(() -> new BusinessException(AttendanceErrorCode.CHECK_IN_REQUIRED,
						"Check in before checking out"));
		day.checkOut(now, staffWorkdaySettingsService.current());
		return staffAttendanceRepository.save(day);
	}

	@Transactional(readOnly = true)
	public Page<StaffAttendance> ownHistory(Long userId, StaffAttendanceFilter filter, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = requireOwnRecord(tenantId, userId);
		return staffAttendanceRepository.search(tenantId, employee.getId(), filter.status(), filter.dates().from(),
				filter.dates().to(), pageable);
	}

	@Transactional(readOnly = true)
	public Page<StaffAttendance> list(StaffAttendanceFilter filter, Pageable pageable) {
		return staffAttendanceRepository.search(TenantContext.getCurrentTenantId(),
				publicIdLookup.idOrNull(EntityRef.EMPLOYEE, filter.employeePublicId()), filter.status(),
				filter.dates().from(), filter.dates().to(), pageable);
	}

	/** Records or corrects one employee's day. A day that has not happened yet cannot be recorded. */
	@Transactional
	@Audited(action = AuditAction.UPDATE, resourceType = "StaffAttendance", details = "Staff attendance recorded")
	public StaffAttendance record(String employeePublicId, LocalDate date, StaffAttendance.DayRecord record,
			String markedBy) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Employee employee = employeeRepository.findByPublicIdAndTenantId(UUID.fromString(employeePublicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeePublicId));
		if (date.isAfter(schoolNow(tenantId).toLocalDate())) {
			throw new BusinessException(AttendanceErrorCode.ATTENDANCE_DATE_IN_FUTURE,
					"Attendance cannot be recorded for a day that has not come yet");
		}
		if (record.status() != StaffAttendanceStatus.ABSENT) {
			requireNotOnLeave(tenantId, employee.getId(), date);
		}
		StaffAttendance day = staffAttendanceRepository
				.findByEmployeeIdAndAttendanceDateAndTenantId(employee.getId(), date, tenantId)
				.map(existing -> {
					existing.correct(record, staffWorkdaySettingsService.current(), markedBy);
					return existing;
				})
				.orElseGet(() -> StaffAttendance.recorded(employee.getId(), date, record,
						staffWorkdaySettingsService.current(), markedBy));
		return staffAttendanceRepository.save(day);
	}

	/** Every active employee's line for one day, in the page asked for. */
	@Transactional(readOnly = true)
	public Page<RegisterEntry> register(LocalDate date, Pageable pageable) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Page<Employee> employees = employeeRepository.findAllByTenantIdAndStatus(tenantId, EmployeeStatus.ACTIVE,
				pageable);
		List<Long> employeeIds = employees.map(Employee::getId).getContent();
		if (employeeIds.isEmpty()) {
			return employees.map(employee -> new RegisterEntry(employee, RegisterStatus.NOT_MARKED, null, null));
		}
		Map<Long, StaffAttendance> days = staffAttendanceRepository
				.findAllByTenantIdAndAttendanceDateAndEmployeeIdIn(tenantId, date, employeeIds).stream()
				.collect(Collectors.toMap(StaffAttendance::getEmployeeId, Function.identity()));
		Map<Long, LeaveRequest> leave = leaveRequestRepository
				.findCovering(tenantId, LeaveRequestStatus.APPROVED, employeeIds, date).stream()
				.collect(Collectors.toMap(LeaveRequest::getEmployeeId, Function.identity(), (first, other) -> first));
		return employees.map(employee -> entryOf(employee, days.get(employee.getId()), leave.get(employee.getId())));
	}

	private static RegisterEntry entryOf(Employee employee, StaffAttendance day, LeaveRequest leave) {
		if (day != null) {
			return new RegisterEntry(employee, RegisterStatus.valueOf(day.getStatus().name()), day, leave);
		}
		return new RegisterEntry(employee, leave == null ? RegisterStatus.NOT_MARKED : RegisterStatus.ON_LEAVE, null,
				leave);
	}

	private void requireNotOnLeave(Long tenantId, Long employeeId, LocalDate date) {
		if (!leaveRequestRepository.findCovering(tenantId, LeaveRequestStatus.APPROVED, List.of(employeeId), date)
				.isEmpty()) {
			throw new BusinessException(AttendanceErrorCode.ON_APPROVED_LEAVE,
					"Approved leave covers " + date + "; cancel the leave to record attendance");
		}
	}

	private Employee requireOwnRecord(Long tenantId, Long userId) {
		return employeeRepository.findByUserIdAndTenantId(userId, tenantId)
				.orElseThrow(() -> new AccessDeniedException("No staff record linked to the current user"));
	}

	private LocalDateTime schoolNow(Long tenantId) {
		return schoolClock.now(tenantId);
	}
}
