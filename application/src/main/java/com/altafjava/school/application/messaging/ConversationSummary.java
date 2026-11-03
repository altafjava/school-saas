package com.altafjava.school.application.messaging;

import com.altafjava.school.domain.messaging.model.Conversation;

public record ConversationSummary(Conversation conversation, long unreadCount) {
}
