package com.altafjava.school.demo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.altafjava.school.application.service.BookCatalogService;
import com.altafjava.school.application.service.CirculationService;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import lombok.RequiredArgsConstructor;

/** A small catalogue with copies on the shelf, plus a loan for the demo student. */
@Component
@Profile({ "dev", "test" })
@RequiredArgsConstructor
class DemoLibrarySeeder {

	private static final String[][] BOOKS = { { "9780143333623", "The Jungle Book", "Rudyard Kipling", "Fiction" },
			{ "9780140328721", "Matilda", "Roald Dahl", "Fiction" },
			{ "9788173711466", "Wings of Fire", "A. P. J. Abdul Kalam", "Biography" },
			{ "9780141321073", "A Brief History of Time", "Stephen Hawking", "Science" },
			{ "9788171673407", "Malgudi Days", "R. K. Narayan", "Fiction" } };
	private static final int COPIES_PER_BOOK = 2;

	private final BookCatalogService bookCatalogService;
	private final CirculationService circulationService;

	void seed(DemoSchool school) {
		BookCopy firstCopy = null;
		for (int b = 0; b < BOOKS.length; b++) {
			Book book = bookCatalogService.createBook(BOOKS[b][0], BOOKS[b][1], BOOKS[b][2], "Demo Press",
					BOOKS[b][3]);
			for (int c = 1; c <= COPIES_PER_BOOK; c++) {
				BookCopy copy = bookCatalogService.addCopy(book.getPublicId().toString(), "BK" + (b + 1) + "-" + c);
				if (firstCopy == null) {
					firstCopy = copy;
				}
			}
		}
		circulationService.checkout(firstCopy.getPublicId().toString(),
				school.loginStudent().getPublicId().toString());
	}
}
