package com.altafjava.school.domain.library.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.library.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

	// Blank q matches everything; pattern comes from LikePattern.contains.
	@Query("""
			SELECT b FROM Book b
			WHERE b.tenantId = :tenantId
			  AND (:pattern IS NULL
			       OR LOWER(b.title) LIKE :pattern ESCAPE '!' OR LOWER(b.author) LIKE :pattern ESCAPE '!' OR LOWER(b.isbn) LIKE :pattern ESCAPE '!' OR LOWER(b.category) LIKE :pattern ESCAPE '!' OR LOWER(b.publisher) LIKE :pattern ESCAPE '!')
			""")
	Page<Book> search(@Param("tenantId") Long tenantId, @Param("pattern") String pattern, Pageable pageable);

	Page<Book> findAllByTenantId(Long tenantId, Pageable pageable);

	Optional<Book> findByPublicIdAndTenantId(UUID publicId, Long tenantId);
}
