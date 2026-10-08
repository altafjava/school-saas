package com.altafjava.school.api.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Either an even split ({@code count} + {@code firstDueDate} [+ {@code intervalMonths}]) or an
 * explicit {@code schedule} whose percentages total 100 — exactly one of the two.
 */
public record SetFeeInstallmentPlanRequest(
		@Schema(description = "Number of equal installments (2-24); use instead of schedule") @Min(2) @Max(24) Integer count,
		@Schema(description = "Due date of the first equal installment") LocalDate firstDueDate,
		@Schema(description = "Months between equal installments, default 1") @Min(1) @Max(12) Integer intervalMonths,
		@Schema(description = "Explicit due dates and percentage shares, totalling 100") @Valid List<Slot> schedule) {

	@Schema(name = "FeeInstallmentSlotRequest", description = "One installment: when it is due and its share of the net fee")
	public record Slot(
			@NotNull LocalDate dueDate,
			@NotNull @DecimalMin("0.01") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal percentage) {
	}

	@Schema(hidden = true)
	@AssertTrue(message = "provide either count with firstDueDate, or a schedule — not both, not neither")
	public boolean isEvenSplitXorSchedule() {
		boolean even = count != null;
		boolean custom = schedule != null && !schedule.isEmpty();
		return even != custom && (!even || firstDueDate != null);
	}
}
