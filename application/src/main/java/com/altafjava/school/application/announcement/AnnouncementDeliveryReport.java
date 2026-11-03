package com.altafjava.school.application.announcement;

import java.util.List;
import com.altafjava.platform.domain.notification.model.ChannelDeliveryCount;

/** Who an announcement reached and how far it got on each channel, including what was skipped and why. */
public record AnnouncementDeliveryReport(int recipientCount, long readCount, boolean dispatched,
		List<ChannelDeliveryCount> channels) {

	public AnnouncementDeliveryReport {
		channels = List.copyOf(channels);
	}
}
