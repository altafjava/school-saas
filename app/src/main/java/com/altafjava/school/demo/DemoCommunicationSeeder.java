package com.altafjava.school.demo;

import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.announcement.AnnouncementService;
import com.altafjava.school.application.announcement.AnnouncementService.Draft;
import com.altafjava.school.application.messaging.ConversationService;
import com.altafjava.school.application.messaging.ConversationService.NewConversation;
import com.altafjava.school.domain.announcement.model.Announcement;
import com.altafjava.school.domain.announcement.model.AnnouncementChannel;
import com.altafjava.school.domain.announcement.model.AudienceType;
import com.altafjava.school.domain.messaging.model.Conversation;
import lombok.RequiredArgsConstructor;

/** A published notice, a draft, and a parent-teacher conversation with a reply. */
@Component
@Profile({ "dev", "test" })
@RequiredArgsConstructor
class DemoCommunicationSeeder {

	private final AnnouncementService announcementService;
	private final ConversationService conversationService;

	void seed(DemoSchool school) {
		Announcement published = announcementService.create(new Draft("Annual Sports Day",
				"Sports Day is in two weeks. Students wear their house T-shirts.", AudienceType.SCHOOL, Set.of(),
				true, Set.of(AnnouncementChannel.IN_APP), List.of(), null), school.adminUserId());
		announcementService.publish(published.getPublicId().toString(), school.adminUserId());
		announcementService.create(new Draft("Winter uniform", "Winter uniform starts next month.",
				AudienceType.SCHOOL, Set.of(), false, Set.of(AnnouncementChannel.IN_APP), List.of(), null),
				school.adminUserId());

		Long parentUserId = school.loginParent().getUserId();
		Long teacherUserId = school.loginTeacher().getUserId();
		Conversation conversation = conversationService.start(new NewConversation(
				school.loginStudent().getPublicId().toString(), school.loginTeacher().getPublicId().toString(), null,
				"Hello, how is my child doing in mathematics this term?", List.of()), parentUserId);
		conversationService.send(conversation.getPublicId().toString(),
				"Steady progress. Ten minutes of practice at home each day would help.", List.of(), teacherUserId);
	}
}
