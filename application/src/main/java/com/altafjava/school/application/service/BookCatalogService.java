package com.altafjava.school.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.exception.ResourceNotFoundException;
import com.altafjava.platform.core.search.LikePattern;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.school.application.library.ReservationAllocator;
import com.altafjava.school.domain.library.model.Book;
import com.altafjava.school.domain.library.model.BookCopy;
import com.altafjava.school.domain.library.repository.BookCopyRepository;
import com.altafjava.school.domain.library.repository.BookRepository;

@Service
public class BookCatalogService {

	private final BookRepository bookRepository;
	private final BookCopyRepository bookCopyRepository;
	private final ReservationAllocator reservationAllocator;

	public BookCatalogService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
			ReservationAllocator reservationAllocator) {
		this.bookRepository = bookRepository;
		this.bookCopyRepository = bookCopyRepository;
		this.reservationAllocator = reservationAllocator;
	}

	/** Free-text {@code q} (blank = no filter) over the entity's identifying fields. */
	@Transactional(readOnly = true)
	public Page<Book> searchBooks(Pageable pageable, String q) {
		return bookRepository.search(TenantContext.getCurrentTenantId(), LikePattern.contains(q), pageable);
	}

	@Transactional(readOnly = true)
	public Page<Book> listBooks(Pageable pageable) {
		return bookRepository.findAllByTenantId(TenantContext.getCurrentTenantId(), pageable);
	}

	@Transactional(readOnly = true)
	public Book findByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return bookRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Book not found: " + publicId));
	}

	@Transactional(readOnly = true)
	public List<BookCopy> listCopies(String bookPublicId) {
		Book book = findByPublicId(bookPublicId);
		return bookCopyRepository.findAllByBookIdAndTenantId(book.getId(), TenantContext.getCurrentTenantId());
	}

	@Transactional
	public Book createBook(String isbn, String title, String author, String publisher, String category) {
		return bookRepository.save(Book.create(isbn, title, author, publisher, category));
	}

	@Transactional
	public Book deactivateBook(String publicId) {
		Book book = findByPublicId(publicId);
		book.deactivate();
		return bookRepository.save(book);
	}

	@Transactional
	public BookCopy addCopy(String bookPublicId, String copyCode) {
		Long tenantId = TenantContext.getCurrentTenantId();
		Book book = findByPublicId(bookPublicId);
		if (bookCopyRepository.existsByBookIdAndCopyCodeAndTenantId(book.getId(), copyCode, tenantId)) {
			throw new BusinessException("Copy code already exists for this book: " + copyCode);
		}
		BookCopy copy = bookCopyRepository.save(BookCopy.create(book.getId(), copyCode));
		reservationAllocator.allocate(tenantId, copy, LocalDate.now());
		return copy;
	}

	@Transactional
	public BookCopy markCopyLost(String copyPublicId) {
		BookCopy copy = findCopyByPublicId(copyPublicId);
		copy.markLost();
		return bookCopyRepository.save(copy);
	}

	@Transactional
	public BookCopy markCopyDamaged(String copyPublicId) {
		BookCopy copy = findCopyByPublicId(copyPublicId);
		copy.markDamaged();
		return bookCopyRepository.save(copy);
	}

	private BookCopy findCopyByPublicId(String publicId) {
		Long tenantId = TenantContext.getCurrentTenantId();
		return bookCopyRepository.findByPublicIdAndTenantId(UUID.fromString(publicId), tenantId)
				.orElseThrow(() -> new ResourceNotFoundException("Book copy not found: " + publicId));
	}
}
