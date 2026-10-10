package com.altafjava.school.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.altafjava.platform.core.concurrency.Versioned;

// Every component is optional, mirroring the domain's Address embeddable: many countries have no
// state/province concept, and a partial address is still a useful capture during data entry.
public record AddressRequest(
		@Size(max = 255) String line1,
		@Size(max = 255) String line2,
		@Size(max = 100) String locality,
		@Size(max = 100) String region,
		@Size(max = 20) String postalCode,
		@Pattern(regexp = "^[A-Z]{2}$", message = "countryCode must be an ISO 3166-1 alpha-2 code") String countryCode,
		@NotNull Long version) implements Versioned {
}
