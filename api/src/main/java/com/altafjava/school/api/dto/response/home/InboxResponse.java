package com.altafjava.school.api.dto.response.home;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What is waiting to be read. A count the caller's role may not read is absent.")
public record InboxResponse(@Schema(description = "Unread notifications") long unreadNotifications,
		@Schema(description = "Unread announcements still in effect") Long unreadAnnouncements,
		@Schema(description = "Unread messages across the caller's conversations") Long unreadMessages) {
}
