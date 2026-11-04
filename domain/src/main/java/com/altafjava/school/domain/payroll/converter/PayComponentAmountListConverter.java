package com.altafjava.school.domain.payroll.converter;

import java.util.List;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.PlatformErrorCode;
import com.altafjava.school.domain.payroll.model.PayComponentAmount;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Stores a pay-component breakdown as a JSON array column rather than a normalized child table,
 * since it's always read/written whole with its parent, never queried independently.
 */
@Converter
public class PayComponentAmountListConverter implements AttributeConverter<List<PayComponentAmount>, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final TypeReference<List<PayComponentAmount>> LIST_TYPE = new TypeReference<>() {
	};

	@Override
	public String convertToDatabaseColumn(List<PayComponentAmount> attribute) {
		try {
			return OBJECT_MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
		} catch (JacksonException e) {
			throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR, "Failed to serialize pay component amounts");
		}
	}

	@Override
	public List<PayComponentAmount> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) {
			return List.of();
		}
		try {
			return OBJECT_MAPPER.readValue(dbData, LIST_TYPE);
		} catch (JacksonException e) {
			throw new BusinessException(PlatformErrorCode.INTERNAL_ERROR,
					"Failed to deserialize pay component amounts");
		}
	}
}
