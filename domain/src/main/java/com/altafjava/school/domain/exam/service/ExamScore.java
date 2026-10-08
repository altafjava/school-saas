package com.altafjava.school.domain.exam.service;

import java.math.BigDecimal;

/** One graded exam's contribution to a weighted result. */
public record ExamScore(BigDecimal marks, BigDecimal maxMarks, BigDecimal weightage) {
}
