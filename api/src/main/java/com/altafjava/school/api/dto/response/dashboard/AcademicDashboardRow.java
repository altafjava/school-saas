package com.altafjava.school.api.dto.response.dashboard;

public record AcademicDashboardRow(String gradeLetter, long count, long totalGradesRecorded, long upcomingExamCount) {
}
