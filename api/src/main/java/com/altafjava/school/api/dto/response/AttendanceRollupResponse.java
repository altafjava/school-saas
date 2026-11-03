package com.altafjava.school.api.dto.response;

public record AttendanceRollupResponse(long present, long halfDay, long absent, long late, long excused, long total) {
}
