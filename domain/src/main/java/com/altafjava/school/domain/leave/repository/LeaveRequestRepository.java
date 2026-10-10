package com.altafjava.school.domain.leave.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.leave.model.LeaveRequest;
import com.altafjava.school.domain.leave.model.LeaveRequestStatus;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

	Page<LeaveRequest> findAllByTenantId(Long tenantId, Pageable pageable);

	Page<LeaveRequest> findAllByEmployeeIdAndTenantId(Long employeeId, Long tenantId, Pageable pageable);

	// Every filter is optional (null matches all); the dates match any leave overlapping [from, to].
	@Query("""
			SELECT lr FROM LeaveRequest lr
			WHERE lr.tenantId = :tenantId
			  AND (:employeeId IS NULL OR lr.employeeId = :employeeId)
			  AND (:leaveTypeId IS NULL OR lr.leaveTypeId = :leaveTypeId)
			  AND (:status IS NULL OR lr.status = :status)
			  AND (:from IS NULL OR lr.endDate >= :from)
			  AND (:to IS NULL OR lr.startDate <= :to)
			""")
	Page<LeaveRequest> search(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId,
			@Param("leaveTypeId") Long leaveTypeId, @Param("status") LeaveRequestStatus status,
			@Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

	Optional<LeaveRequest> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	long countByTenantIdAndStatus(Long tenantId, LeaveRequestStatus status);

	// Pending requests still waiting on the department head's approval, from the given employees.
	default Page<LeaveRequest> findAwaitingDepartmentHead(Long tenantId, Collection<Long> employeeIds,
			Pageable pageable) {
		return findAwaitingFirstApproval(tenantId, LeaveRequestStatus.PENDING, employeeIds, pageable);
	}

	@Query("""
			SELECT lr FROM LeaveRequest lr
			WHERE lr.tenantId = :tenantId AND lr.status = :status
			  AND lr.approvalsRequired - lr.approvalsGranted > 1
			  AND lr.employeeId IN :employeeIds
			""")
	Page<LeaveRequest> findAwaitingFirstApproval(@Param("tenantId") Long tenantId,
			@Param("status") LeaveRequestStatus status, @Param("employeeIds") Collection<Long> employeeIds,
			Pageable pageable);

	// Who cannot be asked to cover a class on {@code date}.
	default List<Long> findEmployeeIdsOnApprovedLeaveOn(Long tenantId, LocalDate date) {
		return findEmployeeIdsOnLeaveOn(tenantId, LeaveRequestStatus.APPROVED, date);
	}

	@Query("""
			SELECT DISTINCT lr.employeeId FROM LeaveRequest lr
			WHERE lr.tenantId = :tenantId AND lr.status = :status
			  AND lr.startDate <= :date AND lr.endDate >= :date
			""")
	List<Long> findEmployeeIdsOnLeaveOn(@Param("tenantId") Long tenantId,
			@Param("status") LeaveRequestStatus status, @Param("date") LocalDate date);

	// A second request over days already requested or approved would deduct the same days twice.
	default boolean existsOverlapping(Long tenantId, Long employeeId, LocalDate startDate, LocalDate endDate) {
		return existsOverlappingWithStatusIn(tenantId, employeeId,
				List.of(LeaveRequestStatus.PENDING, LeaveRequestStatus.APPROVED), startDate, endDate);
	}

	@Query("""
			SELECT COUNT(lr) > 0 FROM LeaveRequest lr
			WHERE lr.tenantId = :tenantId AND lr.employeeId = :employeeId AND lr.status IN :statuses
			  AND lr.startDate <= :endDate AND lr.endDate >= :startDate
			""")
	boolean existsOverlappingWithStatusIn(@Param("tenantId") Long tenantId, @Param("employeeId") Long employeeId,
			@Param("statuses") Collection<LeaveRequestStatus> statuses, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	// Monthly leave-utilization trend (see LeaveUtilizationTrendDataProvider) — summed at the DB
	// per period rather than pulled row-by-row.
	@Query("SELECT COALESCE(SUM(lr.daysRequested), 0) FROM LeaveRequest lr WHERE lr.tenantId = :tenantId "
			+ "AND lr.status = :status AND lr.startDate BETWEEN :from AND :to")
	BigDecimal sumDaysRequestedByTenantIdAndStatusAndStartDateBetween(@Param("tenantId") Long tenantId,
			@Param("status") LeaveRequestStatus status, @Param("from") LocalDate from, @Param("to") LocalDate to);

	// Feeds PayrollCalculator's loss-of-pay computation (see PayslipService) — leaveTypeIds is
	// pre-filtered by the caller to unpaid leave types only; date range overlap (rather than an
	// exact match) is required because a request can span a month boundary.
	@Query("SELECT lr FROM LeaveRequest lr WHERE lr.tenantId = :tenantId AND lr.employeeId = :employeeId "
			+ "AND lr.status = :status AND lr.leaveTypeId IN :leaveTypeIds "
			+ "AND lr.startDate <= :monthEnd AND lr.endDate >= :monthStart")
	List<LeaveRequest> findOverlappingByEmployeeIdAndStatusAndLeaveTypeIdIn(@Param("employeeId") Long employeeId,
			@Param("tenantId") Long tenantId, @Param("status") LeaveRequestStatus status,
			@Param("leaveTypeIds") List<Long> leaveTypeIds, @Param("monthStart") LocalDate monthStart,
			@Param("monthEnd") LocalDate monthEnd);
}
