package com.altafjava.school.application.home;

/**
 * What is waiting to be read. Announcements and messages are absent when the caller's role may not
 * read them.
 */
public record Inbox(long unreadNotifications, Long unreadAnnouncements, Long unreadMessages) {
}
