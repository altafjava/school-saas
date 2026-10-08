package com.altafjava.school.application.idcard;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;

/**
 * Pulls the document verification code out of what a scanner read from a card's QR code: the QR
 * holds either the bare code or the verification link ending in it. Anything else is refused
 * rather than guessed at.
 */
@Component
public class QrPayloadReader {

	private static final int MAX_PAYLOAD_LENGTH = 2048;
	// Issued codes are 8 URL-safe characters; the minimum keeps a bare word like "verify" from a
	// link that has no code on the end from passing as one.
	private static final Pattern VERIFICATION_CODE = Pattern.compile("[A-Za-z0-9_-]{8,32}");

	public String verificationCodeOf(String payload) {
		if (payload == null || payload.isBlank() || payload.length() > MAX_PAYLOAD_LENGTH) {
			throw new BusinessException("The scanned code is not a valid ID card");
		}
		String candidate = lastPathSegment(payload.strip());
		if (!VERIFICATION_CODE.matcher(candidate).matches()) {
			throw new BusinessException("The scanned code is not a valid ID card");
		}
		return candidate;
	}

	private String lastPathSegment(String payload) {
		String withoutFragment = payload.split("[?#]", 2)[0];
		String trimmed = withoutFragment.endsWith("/")
				? withoutFragment.substring(0, withoutFragment.length() - 1)
				: withoutFragment;
		return trimmed.substring(trimmed.lastIndexOf('/') + 1);
	}
}
