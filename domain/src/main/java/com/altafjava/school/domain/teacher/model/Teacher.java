package com.altafjava.school.domain.teacher.model;

import java.time.LocalDate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import com.altafjava.platform.core.security.annotation.Pii;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "teachers")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Teacher extends SoftDeletableEntity {

    @Column(name = "employee_code", nullable = false, length = 50)
    private String employeeCode;

    @Pii
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Pii
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Pii
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "join_date")
    private LocalDate joinDate;

    public static Teacher create(String employeeCode, String firstName, String lastName,
            String email, LocalDate joinDate) {
        return Teacher.builder()
                .employeeCode(employeeCode)
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .joinDate(joinDate)
                .build();
    }
}
