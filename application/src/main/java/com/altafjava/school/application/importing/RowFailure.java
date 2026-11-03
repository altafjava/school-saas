package com.altafjava.school.application.importing;

/** Why one line was not, or would not be, imported. */
record RowFailure(int lineNumber, String field, String message) {
}
