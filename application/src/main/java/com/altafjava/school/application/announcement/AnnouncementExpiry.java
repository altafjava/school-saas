package com.altafjava.school.application.announcement;

import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AnnouncementExpiry {

	private final AnnouncementRepository announcementRepository;

	@Transactional
	public int expireDue(Long tenantId) {
		var due = announcementRepository.findExpired(tenantId, Instant.now());
		due.forEach(announcement -> {
			announcement.expire();
			announcementRepository.save(announcement);
		});
		return due.size();
	}
}
