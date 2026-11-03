package com.altafjava.school.domain.announcement.event;

/** Published with the announcement's commit; sending starts from here, off the request thread. */
public record AnnouncementPublishedEvent(Long tenantId, Long announcementId) {
}
