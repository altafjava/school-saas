package com.altafjava.school.application.messaging;

import java.time.LocalTime;

/** Null hours mean messaging is open all day. Hours crossing midnight are allowed. */
public record MessagingSettings(boolean enabled, LocalTime hoursStart, LocalTime hoursEnd) {

	public boolean isOpenAt(LocalTime time) {
		if (hoursStart == null || hoursEnd == null) {
			return true;
		}
		return hoursStart.isBefore(hoursEnd)
				? !time.isBefore(hoursStart) && time.isBefore(hoursEnd)
				: !time.isBefore(hoursStart) || time.isBefore(hoursEnd);
	}
}
