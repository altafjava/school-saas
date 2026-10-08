package com.altafjava.school.domain.timetable.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.altafjava.school.domain.timetable.model.TimetableSubstitution;

public interface TimetableSubstitutionRepository extends JpaRepository<TimetableSubstitution, Long> {

	Optional<TimetableSubstitution> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

	@Query("SELECT s FROM TimetableSubstitution s WHERE s.tenantId = :tenantId AND s.substitutionDate = :date "
			+ "AND s.cancelledAt IS NULL ORDER BY s.periodId, s.id")
	List<TimetableSubstitution> findActiveOn(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

	@Query("SELECT s FROM TimetableSubstitution s WHERE s.tenantId = :tenantId AND s.substitutionDate = :date "
			+ "AND s.periodId = :periodId AND s.cancelledAt IS NULL")
	List<TimetableSubstitution> findActiveOnDateAndPeriod(@Param("tenantId") Long tenantId,
			@Param("date") LocalDate date, @Param("periodId") Long periodId);

	@Query("SELECT COUNT(s) > 0 FROM TimetableSubstitution s WHERE s.tenantId = :tenantId "
			+ "AND s.timetableEntryId = :entryId AND s.substitutionDate = :date AND s.cancelledAt IS NULL")
	boolean existsActiveFor(@Param("tenantId") Long tenantId, @Param("entryId") Long entryId,
			@Param("date") LocalDate date);
}
