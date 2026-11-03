package com.altafjava.school.application.messaging;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.application.service.TenantSettingOverrideService;
import com.altafjava.platform.application.service.TenantSettingsService;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import lombok.RequiredArgsConstructor;

/** Each school decides whether messaging is on and when, stored as tenant setting overrides. */
@Service
@RequiredArgsConstructor
public class MessagingSettingsService {

	static final String ENABLED_KEY = "school.messaging.enabled";
	static final String HOURS_START_KEY = "school.messaging.hours-start";
	static final String HOURS_END_KEY = "school.messaging.hours-end";

	private final TenantSettingOverrideService tenantSettingOverrideService;
	private final TenantSettingsService tenantSettingsService;

	@Transactional(readOnly = true)
	public MessagingSettings get() {
		Long tenantId = TenantContext.getCurrentTenantId();
		boolean enabled = tenantSettingOverrideService.get(tenantId, ENABLED_KEY).map(Boolean::parseBoolean)
				.orElse(true);
		return new MessagingSettings(enabled, time(tenantId, HOURS_START_KEY), time(tenantId, HOURS_END_KEY));
	}

	@Transactional
	public MessagingSettings update(MessagingSettings settings) {
		if ((settings.hoursStart() == null) != (settings.hoursEnd() == null)) {
			throw new BusinessException("Set both messaging hours or neither");
		}
		Long tenantId = TenantContext.getCurrentTenantId();
		tenantSettingOverrideService.set(tenantId, ENABLED_KEY, String.valueOf(settings.enabled()));
		store(tenantId, HOURS_START_KEY, settings.hoursStart());
		store(tenantId, HOURS_END_KEY, settings.hoursEnd());
		return settings;
	}

	/** Refuses a send while messaging is switched off or outside the school's hours. */
	@Transactional(readOnly = true)
	public void assertOpenNow() {
		MessagingSettings settings = get();
		if (!settings.enabled()) {
			throw new BusinessException("Messaging is switched off at this school");
		}
		if (!settings.isOpenAt(ZonedDateTime.now(schoolZone()).toLocalTime())) {
			throw new BusinessException(
					"Messaging is open from " + settings.hoursStart() + " to " + settings.hoursEnd());
		}
	}

	private ZoneId schoolZone() {
		String timezone = tenantSettingsService.getSettings(TenantContext.getCurrentTenantId()).timezone();
		return timezone == null || timezone.isBlank() ? ZoneId.of("UTC") : ZoneId.of(timezone);
	}

	private void store(Long tenantId, String key, LocalTime time) {
		if (time == null) {
			tenantSettingOverrideService.remove(tenantId, key);
		} else {
			tenantSettingOverrideService.set(tenantId, key, time.toString());
		}
	}

	// A value that cannot be read leaves that bound unset rather than closing messaging for everyone.
	private LocalTime time(Long tenantId, String key) {
		Optional<String> stored = tenantSettingOverrideService.get(tenantId, key);
		try {
			return stored.map(LocalTime::parse).orElse(null);
		} catch (DateTimeParseException e) {
			return null;
		}
	}
}
