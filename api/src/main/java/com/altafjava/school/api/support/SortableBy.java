package com.altafjava.school.api.support;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lists the entity attributes a paged endpoint accepts in its {@code sort} query parameter, beyond
 * the audit columns every list can be sorted by. Only attributes named here are ever handed to the
 * database, so a client cannot sort by a sensitive or unindexed column.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SortableBy {

	String[] value();
}
