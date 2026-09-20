package com.altafjava.school.application.service;

import com.altafjava.platform.application.service.ActivityLogService;

/**
 * Real (non-Mockito) test double for {@link ActivityLogService} — Mockito's inline mock maker
 * cannot instrument this class when it's consumed as a published Maven artifact in this module's
 * test classpath ("Could not modify all classes"), so audit-trail tests here use plain subclassing
 * instead. All four constructor dependencies are unused once {@link #log} is overridden to a no-op.
 */
class NoOpActivityLogService extends ActivityLogService {

	NoOpActivityLogService() {
		super(null, null, null, null);
	}

	@Override
	public void log(Long tenantId, String action, String resourceType, String resourceId, String actorId, String ip,
			String ua, String details, String oldVal, String newVal, String requestId, String sessionId,
			String correlationId) {
		// no-op
	}
}
