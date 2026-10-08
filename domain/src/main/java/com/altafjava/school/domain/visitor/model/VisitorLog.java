package com.altafjava.school.domain.visitor.model;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * One visitor's time on the premises. It is created from an approved {@link VisitorRequest} and
 * copies what the request said, so the log still reads correctly if the request is later edited.
 * {@code hostEmployeeId} references {@code Employee} rather than a raw platform user id, following
 * every other cross-entity reference in this codebase.
 */
@Entity
@Table(name = "visitor_logs")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VisitorLog extends SoftDeletableEntity {

	@Pii
	@Column(name = "visitor_name", nullable = false, length = 150)
	private String visitorName;

	@Pii
	@Column(name = "visitor_phone", length = 50)
	private String visitorPhone;

	@Column(name = "purpose", nullable = false, length = 500)
	private String purpose;

	@Column(name = "host_employee_id", nullable = false)
	private Long hostEmployeeId;

	// FK to visitor_requests.id — null for visits logged before approvals existed.
	@Column(name = "visitor_request_id")
	private Long visitorRequestId;

	// FK to platform file_metadata.public_id — the photo taken or supplied for this visit.
	@Column(name = "photo_file_public_id")
	private UUID photoFilePublicId;

	// FK to platform document_issuances.id — the badge handed to the visitor.
	@Column(name = "badge_issuance_id")
	private Long badgeIssuanceId;

	@Column(name = "check_in_at", nullable = false)
	private LocalDateTime checkInAt;

	@Column(name = "check_out_at")
	private LocalDateTime checkOutAt;

	public static VisitorLog checkIn(VisitorRequest request, UUID photoFilePublicId, LocalDateTime checkInAt) {
		return VisitorLog.builder()
				.visitorName(request.getVisitorName())
				.visitorPhone(request.getVisitorPhone())
				.purpose(request.getPurpose())
				.hostEmployeeId(request.getHostEmployeeId())
				.visitorRequestId(request.getId())
				.photoFilePublicId(photoFilePublicId)
				.checkInAt(checkInAt)
				.build();
	}

	public void attachBadge(Long badgeIssuanceId) {
		this.badgeIssuanceId = badgeIssuanceId;
	}

	public void checkOut(LocalDateTime checkOutAt) {
		if (this.checkOutAt != null) {
			throw new BusinessException("Visitor already checked out at " + this.checkOutAt);
		}
		if (checkOutAt.isBefore(this.checkInAt)) {
			throw new BusinessException("Check-out time cannot be before check-in time");
		}
		this.checkOutAt = checkOutAt;
	}
}
