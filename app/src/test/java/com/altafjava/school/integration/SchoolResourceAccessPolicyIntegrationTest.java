package com.altafjava.school.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import com.altafjava.platform.application.dto.RegisterTenantCommand;
import com.altafjava.platform.application.service.TenantOnboardingService;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.school.application.policy.SchoolResourceAccessPolicy;
import com.altafjava.school.base.SchoolIntegrationTestBase;
import com.altafjava.school.config.TestPaymentConfig;
import com.altafjava.school.config.TestRedisConfig;
import com.altafjava.school.domain.classroom.model.Classroom;
import com.altafjava.school.domain.classroom.repository.ClassroomRepository;
import com.altafjava.school.domain.teacher.model.Teacher;
import com.altafjava.school.domain.teacher.repository.TeacherRepository;

/**
 * Verifies that SchoolResourceAccessPolicy is registered with the platform enforcer
 * and that teacher-classroom access restriction rules are applied.
 *
 * Phase 5 validation: ResourceAccessPolicy SPI is discovered and invoked by the platform.
 */
@Import({TestRedisConfig.class, TestPaymentConfig.class})
class SchoolResourceAccessPolicyIntegrationTest extends SchoolIntegrationTestBase {

    @Autowired
    private SchoolResourceAccessPolicy schoolResourceAccessPolicy;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private ClassroomRepository classroomRepository;

    @Autowired
    private TenantOnboardingService onboardingService;

    private Long testTenantId;

    @BeforeEach
    void createTenantContext() {
        TenantContext.clear();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Tenant tenant = onboardingService.registerTenant(new RegisterTenantCommand(
                "Policy Test School", "policy-" + suffix, 1L,
                "admin@policy.test", "Password123!", "USD"));
        testTenantId = tenant.getId();
        TenantContext.setCurrentTenant(testTenantId, tenant.getPublicId(), "policy-" + suffix, TenantType.SHARED);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void schoolResourceAccessPolicy_isRegisteredAsSpringBean() {
        assertNotNull(schoolResourceAccessPolicy,
                "SchoolResourceAccessPolicy must be discovered as a Spring bean via @Component");
    }

    @Test
    void teacher_canAccessOwnClassroom() {
        Teacher teacher = teacherRepository.save(Teacher.create(
                "EMP-" + UUID.randomUUID().toString().substring(0, 6),
                "Ms", "Johnson", "johnson@test.edu", null));
        Classroom classroom = classroomRepository.save(Classroom.create(
                "CLS-" + UUID.randomUUID().toString().substring(0, 6),
                "Grade 5", "A", "2024-25", teacher.getId()));

        boolean allowed = schoolResourceAccessPolicy.isAllowed(
                String.valueOf(teacher.getId()), testTenantId, "CLASSROOM", classroom.getPublicId().toString(), "READ");
        assertTrue(allowed, "Teacher must be allowed to READ their own classroom");
    }

    @Test
    void teacher_cannotAccessAnotherTeachersClassroom() {
        Teacher teacher1 = teacherRepository.save(Teacher.create(
                "EMP-" + UUID.randomUUID().toString().substring(0, 6),
                "Mr", "Smith", "smith@test.edu", null));
        Teacher teacher2 = teacherRepository.save(Teacher.create(
                "EMP-" + UUID.randomUUID().toString().substring(0, 6),
                "Mrs", "Lee", "lee@test.edu", null));
        Classroom classroom = classroomRepository.save(Classroom.create(
                "CLS-" + UUID.randomUUID().toString().substring(0, 6),
                "Grade 6", "B", "2024-25", teacher1.getId()));

        boolean allowed = schoolResourceAccessPolicy.isAllowed(
                String.valueOf(teacher2.getId()), testTenantId, "CLASSROOM", classroom.getPublicId().toString(), "READ");
        assertFalse(allowed, "Teacher2 must be denied READ access to teacher1's classroom");
    }

    @Test
    void nonClassroomResources_areAllowedByDefault() {
        boolean allowed = schoolResourceAccessPolicy.isAllowed(
                "any-user", testTenantId, "STUDENT", "any-public-id", "READ");
        assertTrue(allowed, "Resource types not managed by the school policy must default to allowed");
    }
}
