package com.altafjava.school.application.scheduler;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.scheduler.annotation.ScheduledJob;
import com.altafjava.platform.application.scheduler.strategy.JobExecutionStrategy;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionContext;
import com.altafjava.platform.domain.scheduler.model.JobExecutionResult;
import com.altafjava.school.application.announcement.AnnouncementDispatcher;
import com.altafjava.school.application.announcement.AnnouncementExpiry;
import com.altafjava.school.application.announcement.AnnouncementPublisher;
import com.altafjava.school.domain.announcement.model.AnnouncementStatus;
import com.altafjava.school.domain.announcement.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Publishes scheduled announcements, expires old ones, and finishes any interrupted sending. */
@Slf4j
@Component
@RequiredArgsConstructor
@ScheduledJob(name = AnnouncementLifecycleJob.JOB_NAME, group = "school", description = "Publishes scheduled announcements, expires old ones and resumes interrupted sends", cronExpression = "0 * * * * ?", tenantScoped = true, retryEnabled = false)
public class AnnouncementLifecycleJob implements JobExecutionStrategy {

	public static final String JOB_NAME = "AnnouncementLifecycle";

	private final AnnouncementRepository announcementRepository;
	private final AnnouncementPublisher publisher;
	private final AnnouncementDispatcher dispatcher;
	private final AnnouncementExpiry expiry;

	@Override
	public String jobName() {
		return JOB_NAME;
	}

	@Override
	public String jobGroup() {
		return "school";
	}

	@Override
	public boolean isTenantScoped() {
		return true;
	}

	@Override
	public JobExecutionResult execute(JobExecutionContext ctx) {
		Long tenantId = TenantContext.getCurrentTenantId();
		List<Long> due = announcementRepository.findDueIds(tenantId, AnnouncementStatus.SCHEDULED, Instant.now());
		due.forEach(id -> publishSafely(tenantId, id));
		int expired = expiry.expireDue(tenantId);
		announcementRepository.findUndispatchedIds(tenantId).forEach(id -> dispatcher.dispatch(tenantId, id));
		return new JobExecutionResult.Success(Map.of("published", due.size(), "expired", expired), null);
	}

	// One announcement that cannot publish, such as a deleted classroom, must not hold up the others.
	private void publishSafely(Long tenantId, Long announcementId) {
		try {
			publisher.publish(tenantId, announcementId);
		} catch (RuntimeException e) {
			log.error("action=announcement-publish-failed tenantId={} announcementId={}", tenantId, announcementId, e);
		}
	}
}
