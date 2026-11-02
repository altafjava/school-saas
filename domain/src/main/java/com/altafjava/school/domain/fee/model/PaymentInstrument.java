package com.altafjava.school.domain.fee.model;

import java.time.LocalDate;
import com.altafjava.platform.core.exception.BusinessException;

/**
 * The details a school needs to match a payment against its bank statement: a transaction
 * reference for electronic modes, and number, date and bank for a cheque.
 */
public record PaymentInstrument(PaymentMode mode, String transactionReference, String chequeNumber,
		LocalDate chequeDate, String chequeBank) {

	public static final PaymentInstrument CASH = new PaymentInstrument(PaymentMode.CASH, null, null, null, null);

	public PaymentInstrument {
		if (mode == null) {
			throw new BusinessException("A payment needs a payment mode");
		}
		switch (mode) {
			case CHEQUE -> requireChequeDetails(chequeNumber, chequeDate, chequeBank);
			case UPI, BANK_TRANSFER, CARD -> requireReference(mode, transactionReference);
			case CASH, GATEWAY -> {
			}
		}
	}

	private static void requireChequeDetails(String number, LocalDate date, String bank) {
		if (isBlank(number) || date == null || isBlank(bank)) {
			throw new BusinessException("A cheque payment needs the cheque number, date and bank");
		}
	}

	private static void requireReference(PaymentMode mode, String reference) {
		if (isBlank(reference)) {
			throw new BusinessException("A " + mode + " payment needs a transaction reference");
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
