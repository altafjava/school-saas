package com.altafjava.school.api.dto.response.enquiry;

import java.time.Instant;
import java.time.LocalDate;
import com.altafjava.school.domain.enquiry.model.FollowUpChannel;

public record EnquiryFollowUpResponse(String publicId, FollowUpChannel channel, String note, Instant followedUpAt,
		LocalDate nextFollowUpOn) {
}
