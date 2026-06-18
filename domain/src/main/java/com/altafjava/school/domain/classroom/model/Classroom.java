package com.altafjava.school.domain.classroom.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "classrooms")
@SQLRestriction("deleted = false")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Classroom extends SoftDeletableEntity {

    @Column(name = "class_code", nullable = false, length = 50)
    private String classCode;

    @Column(name = "grade", nullable = false, length = 20)
    private String grade;

    @Column(name = "section", nullable = false, length = 10)
    private String section;

    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    // FK to teachers.id — stored as Long to avoid cross-entity coupling in domain layer
    @Column(name = "class_teacher_id")
    private Long classTeacherId;

    public static Classroom create(String classCode, String grade, String section,
            String academicYear, Long classTeacherId) {
        return Classroom.builder()
                .classCode(classCode)
                .grade(grade)
                .section(section)
                .academicYear(academicYear)
                .classTeacherId(classTeacherId)
                .build();
    }
}
