package com.altafjava.school.domain.announcement.model;

/** Who an announcement is for. SCHOOL needs no targets; every other type needs at least one. */
public enum AudienceType {
	SCHOOL, GRADE, CLASSROOM, ROLE, GUARDIANS
}
