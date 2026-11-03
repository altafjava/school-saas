package com.altafjava.school.api.dto.response;

import java.util.List;

/** A channel row with status {@code SKIPPED} says the channel was not attempted, and why. */
public record AnnouncementDeliveryResponse(
		int recipientCount,
		long readCount,
		long unreadCount,
		boolean dispatched,
		List<ChannelDelivery> channels) {

	public AnnouncementDeliveryResponse {
		channels = List.copyOf(channels);
	}

	public record ChannelDelivery(String channel, String status, String skipReason, long count) {
	}
}
