package com.altafjava.school.application.home;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.school.application.announcement.AnnouncementService;
import com.altafjava.school.application.messaging.ConversationService;
import com.altafjava.school.domain.security.permission.SchoolPermissions;

@ExtendWith(MockitoExtension.class)
class InboxLoaderTest {

	@Mock
	private NotificationService notificationService;
	@Mock
	private AnnouncementService announcementService;
	@Mock
	private ConversationService conversationService;
	@InjectMocks
	private InboxLoader inboxLoader;

	@Test
	void load_countsEachFeedTheCallerMayRead() {
		when(notificationService.getUnreadCount("7")).thenReturn(3L);
		when(announcementService.unreadCount(7L)).thenReturn(2L);
		when(conversationService.unreadCount(7L)).thenReturn(5L);

		Inbox inbox = inboxLoader.load(new HomeCaller(7L,
				Set.of(SchoolPermissions.ANNOUNCEMENT_READ, SchoolPermissions.MESSAGING_PARTICIPATE)));

		assertEquals(new Inbox(3L, 2L, 5L), inbox);
	}

	@Test
	void load_leavesOutFeedsTheCallersRoleMayNotRead() {
		when(notificationService.getUnreadCount("7")).thenReturn(3L);

		Inbox inbox = inboxLoader.load(new HomeCaller(7L, Set.of()));

		assertEquals(3L, inbox.unreadNotifications());
		assertNull(inbox.unreadAnnouncements());
		assertNull(inbox.unreadMessages());
		verify(announcementService, never()).unreadCount(any());
		verify(conversationService, never()).unreadCount(any());
	}
}
