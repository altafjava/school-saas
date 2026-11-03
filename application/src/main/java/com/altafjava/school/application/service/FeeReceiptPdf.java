package com.altafjava.school.application.service;

/** A fee receipt ready to download. */
public record FeeReceiptPdf(String fileName, byte[] content) {

	public FeeReceiptPdf {
		content = content.clone();
	}

	@Override
	public byte[] content() {
		return content.clone();
	}
}
