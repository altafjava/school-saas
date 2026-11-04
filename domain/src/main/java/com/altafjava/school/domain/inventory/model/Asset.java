package com.altafjava.school.domain.inventory.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "assets")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Asset extends SoftDeletableEntity {

	@Column(name = "asset_code", nullable = false, length = 50)
	private String assetCode;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	// Free string, not a separate catalog entity — asset categories (Furniture, Electronics, Lab
	// Equipment...) are simple labels, not structured data like a Board/Curriculum.
	@Column(name = "category", length = 100)
	private String category;

	@Column(name = "purchase_date")
	private LocalDate purchaseDate;

	@Column(name = "purchase_cost", precision = 12, scale = 2)
	private BigDecimal purchaseCost;

	@Column(name = "location", length = 150)
	private String location;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 30)
	private AssetStatus status;

	public static Asset create(String assetCode, String name, String category, LocalDate purchaseDate,
			BigDecimal purchaseCost, String location) {
		return Asset.builder()
				.assetCode(assetCode)
				.name(name)
				.category(category)
				.purchaseDate(purchaseDate)
				.purchaseCost(purchaseCost)
				.location(location)
				.status(AssetStatus.AVAILABLE)
				.build();
	}

	public void updateLocation(String location) {
		this.location = location;
	}

	public void markInUse() {
		requireStatus(AssetStatus.AVAILABLE);
		this.status = AssetStatus.IN_USE;
	}

	public void markAvailable() {
		if (this.status == AssetStatus.DISPOSED) {
			throw new BusinessException(PlatformErrorCode.CONFLICT,
					"Cannot return a disposed asset to available status");
		}
		this.status = AssetStatus.AVAILABLE;
	}

	public void markUnderMaintenance() {
		if (this.status == AssetStatus.DISPOSED) {
			throw new BusinessException(PlatformErrorCode.CONFLICT, "Cannot send a disposed asset for maintenance");
		}
		this.status = AssetStatus.UNDER_MAINTENANCE;
	}

	public void markDisposed() {
		this.status = AssetStatus.DISPOSED;
	}

	private void requireStatus(AssetStatus required) {
		if (this.status != required) {
			throw new BusinessException(PlatformErrorCode.CONFLICT,
					"Asset must be " + required + " but is " + this.status);
		}
	}
}
