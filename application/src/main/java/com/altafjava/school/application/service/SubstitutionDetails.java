package com.altafjava.school.application.service;

import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.timetable.model.TimetableEntry;
import com.altafjava.school.domain.timetable.model.TimetableSubstitution;

/** A substitution with the slot it covers and the two teachers involved, for display. */
public record SubstitutionDetails(TimetableSubstitution substitution, TimetableEntry entry, Teacher regularTeacher,
		Teacher substituteTeacher) {
}
