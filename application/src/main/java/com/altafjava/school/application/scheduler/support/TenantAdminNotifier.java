package com.altafjava.school.application.scheduler.support;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.altafjava.platform.application.dto.notification.SendNotificationCommand;
import com.altafjava.platform.application.service.NotificationService;
import com.altafjava.platform.core.model.Pageable;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.platform.domain.notification.model.NotificationPriority;
import com.altafjava.platform.domain.notification.model.NotificationTypes;
import com.altafjava.platform.domain.user.model.User;
import com.altafjava.platform.domain.user.model.UserSearchCriteria;
import com.altafjava.platform.domain.user.repository.UserRepository;

/** Sends an operational notification to every TENANT_ADMIN in the current tenant. */
@Component
public class TenantAdminNotifier {

	private static final int MAX_ADMIN_RECIPIENTS = 200;

	private final UserRepository userRepository;
	private final NotificationService notificationService;

	public TenantAdminNotifier(UserRepository userRepository, NotificationService notificationService) {
		this.userRepository = userRepository;
		this.notificationService = notificationService;
	}

	public int notifyAll(Long tenantId, String title, String message) {
		return notifyAll(tenantId, NotificationTypes.ANNOUNCEMENT, title, message, Map.of());
	}

	public int notifyAll(Long tenantId, String type, String title, String message,
			Map<String, String> templateVariables) {
		UserSearchCriteria criteria = new UserSearchCriteria(null, Roles.TENANT_ADMIN, null, null);
		var admins = userRepository.findAll(criteria, Pageable.of(0, MAX_ADMIN_RECIPIENTS));

		int notifiedCount = 0;
		for (User admin : admins.content()) {
			notificationService.send(SendNotificationCommand.builder()
					.tenantId(tenantId)
					.userId(admin.getId())
					.type(type)
					.title(title)
					.message(message)
					.templateVariables(templateVariables)
					.priority(NotificationPriority.LOW)
					.build());
			notifiedCount++;
		}
		return notifiedCount;
	}
}
