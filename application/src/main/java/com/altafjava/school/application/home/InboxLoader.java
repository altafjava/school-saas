package com.altafjava.school.application.home;

import org.springframework.stereotype.Component;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.school.application.announcement.AnnouncementService;
import com.altafjava.school.application.messaging.ConversationService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;
import lombok.RequiredArgsConstructor;

/** The inbox card shared by every home screen; each count follows the permission of its own feed. */
@Component
@RequiredArgsConstructor
public class InboxLoader {

	private final NotificationService notificationService;
	private final AnnouncementService announcementService;
	private final ConversationService conversationService;

	public Inbox load(HomeCaller caller) {
		return new Inbox(notificationService.getUnreadCount(caller.userId().toString()),
				caller.holds(SchoolPermissions.ANNOUNCEMENT_READ) ? announcementService.unreadCount(caller.userId())
						: null,
				caller.holds(SchoolPermissions.MESSAGING_PARTICIPATE)
						? conversationService.unreadCount(caller.userId())
						: null);
	}
}
