package com.altafjava.school.domain.error;

import com.altafjava.platform.core.exception.ErrorCode;

/** Error codes for the library; each is part of the public API contract. */
public enum LibraryErrorCode implements ErrorCode {

	BOOK_WITHDRAWN(409, "The book is no longer in the catalogue."), //
	BORROWER_NOT_ACTIVE(409, "Only an active student can borrow or reserve a book."), //
	RESERVATION_EXISTS(409, "The student already has a reservation for this book."), //
	BOOK_ALREADY_BORROWED(409, "The student already has a copy of this book."), //
	BOOK_ON_SHELF(409, "A copy is on the shelf; check it out instead of reserving."), //
	BOOK_UNAVAILABLE(409, "No copy of this book can become available."), //
	RENEWAL_BLOCKED_BY_RESERVATION(409, "Another member is waiting for this book, so it cannot be renewed."), //
	BOOK_COPY_ON_HOLD(409, "The copy is held for a member."), //
	BOOK_COPY_STATUS_INVALID(409, "The book copy is not in a status that allows this action."), //
	RESERVATION_STATUS_INVALID(409, "The reservation is not in a status that allows this action."), //
	LOAN_RETURNED(409, "The book has already been returned."), //
	LOAN_OVERDUE(409, "An overdue book cannot be renewed; return it first."), //
	RENEWAL_LIMIT_REACHED(409, "The loan has been renewed the maximum number of times."), //
	RENEWAL_DATE_INVALID(400, "A renewal must extend the due date."), //
	RETURN_DATE_INVALID(400, "A book cannot be returned before it was checked out.");

	private final int httpStatus;
	private final String description;

	LibraryErrorCode(int httpStatus, String description) {
		this.httpStatus = httpStatus;
		this.description = description;
	}

	@Override
	public String code() {
		return name();
	}

	@Override
	public int httpStatus() {
		return httpStatus;
	}

	@Override
	public String description() {
		return description;
	}
}
