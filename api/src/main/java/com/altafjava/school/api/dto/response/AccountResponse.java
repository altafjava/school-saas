package com.altafjava.school.api.dto.response;

import com.altafjava.school.api.support.NoLabel;

public record AccountResponse(@NoLabel("Reply to a single write whose caller just sent this id") String userPublicId,
		String email) {
}
