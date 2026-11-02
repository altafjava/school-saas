package com.altafjava.school.domain.lms.model;

/** That a student has handed in an assignment. */
public record SubmittedWork(Long studentId, Long assignmentId) {
}
