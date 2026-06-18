package com.altafjava.school.domain.teacher.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.altafjava.school.domain.teacher.model.Teacher;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Page<Teacher> findAllByTenantId(Long tenantId, Pageable pageable);

    Optional<Teacher> findByPublicIdAndTenantId(UUID publicId, Long tenantId);

    boolean existsByEmployeeCodeAndTenantId(String employeeCode, Long tenantId);
}
