package com.altafjava.school.application.reference;

/** Enough about a referenced record for a client to label it without a second request. */
public record ReferenceSummary(String publicId, String name, String code) {
}
