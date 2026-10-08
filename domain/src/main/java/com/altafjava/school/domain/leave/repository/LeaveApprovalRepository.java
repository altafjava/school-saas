package com.altafjava.school.domain.leave.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.leave.model.LeaveApproval;

public interface LeaveApprovalRepository extends JpaRepository<LeaveApproval, Long> {

	List<LeaveApproval> findAllByLeaveRequestIdAndTenantIdOrderByDecidedAtAsc(Long leaveRequestId, Long tenantId);

	boolean existsByLeaveRequestIdAndDecidedByUserIdAndTenantId(Long leaveRequestId, Long decidedByUserId,
			Long tenantId);
}
