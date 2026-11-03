package com.altafjava.school.application.announcement;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.announcement.event.AnnouncementPublishedEvent;
import lombok.RequiredArgsConstructor;

/** Sends a published announcement batch by batch; the lifecycle job finishes any interrupted run. */
@Component
@RequiredArgsConstructor
public class AnnouncementDispatcher {

	private final AnnouncementDispatchBatch dispatchBatch;

	@Async("platformTaskExecutor")
	@EventListener
	public void onPublished(AnnouncementPublishedEvent event) {
		dispatch(event.tenantId(), event.announcementId());
	}

	public void dispatch(Long tenantId, Long announcementId) {
		boolean more = true;
		while (more) {
			more = dispatchBatch.sendNext(tenantId, announcementId);
		}
	}
}
