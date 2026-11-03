package com.altafjava.school.domain.enquiry.model;

import java.util.Set;

/** An enquiry's life: open while the school follows it up, then converted into an application or closed. */
public enum EnquiryStatus {
	NEW, IN_PROGRESS, CONVERTED, CLOSED;

	public boolean isOpen() {
		return this == NEW || this == IN_PROGRESS;
	}

	public boolean canMoveTo(EnquiryStatus next) {
		return switch (this) {
			case NEW -> Set.of(IN_PROGRESS, CONVERTED, CLOSED).contains(next);
			case IN_PROGRESS -> Set.of(CONVERTED, CLOSED).contains(next);
			case CLOSED -> next == IN_PROGRESS;
			case CONVERTED -> false;
		};
	}
}
