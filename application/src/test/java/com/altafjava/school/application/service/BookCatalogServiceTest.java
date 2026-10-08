package com.altafjava.school.application.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.school.application.library.ReservationAllocator;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookRepository;

@ExtendWith(MockitoExtension.class)
class BookCatalogServiceTest {

	private static final UUID BOOK_PUBLIC_ID = UUID.randomUUID();

	@Mock
	private BookRepository bookRepository;
	@Mock
	private BookCopyRepository bookCopyRepository;
	@Mock
	private ReservationAllocator reservationAllocator;

	private BookCatalogService bookCatalogService;

	@BeforeEach
	void setUp() {
		bookCatalogService = new BookCatalogService(bookRepository, bookCopyRepository, reservationAllocator);
		TenantContext.ForTesting.setCurrentTenant(1L, null, null, TenantType.SHARED);
	}

	@AfterEach
	void clearContext() {
		TenantContext.ForTesting.clear();
	}

	@Test
	void createBook_succeeds() {
		when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

		Book book = bookCatalogService.createBook("978-0", "Book Title", "Author", "Publisher", "Fiction");

		assertEquals("Book Title", book.getTitle());
	}

	@Test
	void addCopy_withDuplicateCopyCode_throwsBusinessException() {
		Book book = Book.create("978-0", "Book Title", "Author", "Publisher", "Fiction");
		book.setId(5L);
		when(bookRepository.findByPublicIdAndTenantId(BOOK_PUBLIC_ID, 1L)).thenReturn(Optional.of(book));
		when(bookCopyRepository.existsByBookIdAndCopyCodeAndTenantId(5L, "COPY-1", 1L)).thenReturn(true);

		assertThrows(BusinessException.class, () -> bookCatalogService.addCopy(BOOK_PUBLIC_ID.toString(), "COPY-1"));
	}

	@Test
	void addCopy_withNewCopyCode_succeeds() {
		Book book = Book.create("978-0", "Book Title", "Author", "Publisher", "Fiction");
		book.setId(5L);
		when(bookRepository.findByPublicIdAndTenantId(BOOK_PUBLIC_ID, 1L)).thenReturn(Optional.of(book));
		when(bookCopyRepository.existsByBookIdAndCopyCodeAndTenantId(5L, "COPY-1", 1L)).thenReturn(false);
		when(bookCopyRepository.save(any(BookCopy.class))).thenAnswer(inv -> inv.getArgument(0));

		BookCopy copy = assertDoesNotThrow(() -> bookCatalogService.addCopy(BOOK_PUBLIC_ID.toString(), "COPY-1"));

		assertEquals("COPY-1", copy.getCopyCode());
	}
}
