package com.altafjava.school.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.Past;
import com.altafjava.school.domain.common.model.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Details the application needs beyond the enquiry. Both are optional.")
public record ConvertEnquiryRequest(@Past LocalDate childDateOfBirth, Gender childGender) {
}
