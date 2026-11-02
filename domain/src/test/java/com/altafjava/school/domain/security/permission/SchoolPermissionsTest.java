package com.altafjava.school.domain.security.permission;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import com.altafjava.platform.core.security.permission.PermissionDefinition;

class SchoolPermissionsTest {

	@Test
	void everyConstant_namesItsOwnCode_andTheCatalogHoldsExactlyThem() throws Exception {
		List<Field> constants = Arrays.stream(SchoolPermissions.class.getFields())
				.filter(field -> Modifier.isStatic(field.getModifiers()) && field.getType() == String.class)
				.toList();
		for (Field constant : constants) {
			assertEquals(constant.getName(), constant.get(null));
		}

		Set<String> catalog = SchoolPermissions.CATALOG.stream().map(PermissionDefinition::code)
				.collect(Collectors.toSet());
		assertEquals(constants.stream().map(Field::getName).collect(Collectors.toSet()), catalog);
	}
}
