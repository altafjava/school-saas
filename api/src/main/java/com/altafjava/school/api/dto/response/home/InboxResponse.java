package com.altafjava.school.api.dto.response.home;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "What is waiting to be read.")
public record InboxResponse(@Schema(description = "Unread notifications") long unreadNotifications) {
}
