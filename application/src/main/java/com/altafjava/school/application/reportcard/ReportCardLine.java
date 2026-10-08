package com.altafjava.school.application.reportcard;

import java.math.BigDecimal;

// One row of a report card's grade table — a student's result in one published exam within the term.
public record ReportCardLine(String subjectName, String examTitle, BigDecimal marks, BigDecimal maxMarks,
		BigDecimal weightage, String gradeLetter) {
}
