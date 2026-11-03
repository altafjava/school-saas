package com.altafjava.school.application.home;

import java.time.LocalDate;
import java.util.List;

public record ParentHome(LocalDate date, boolean schoolDay, List<StudentOverview> children, Inbox inbox) {

	public ParentHome {
		children = List.copyOf(children);
	}
}
