package com.altafjava.school.api.support;

import java.util.Locale;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import com.altafjava.school.domain.customfield.model.CustomFieldEntityType;

/**
 * Lets the {@code {entityType}} path segment be written {@code student} or {@code fee-structure} as well as
 * {@code FEE_STRUCTURE}.
 */
@Component
public class CustomFieldEntityTypeConverter implements Converter<String, CustomFieldEntityType> {

	@Override
	public CustomFieldEntityType convert(String source) {
		return CustomFieldEntityType.valueOf(source.trim().replace('-', '_').toUpperCase(Locale.ROOT));
	}
}
