package com.altafjava.school.config;

import java.util.Map;
import java.util.UUID;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import com.altafjava.platform.domain.payment.model.PaymentCardDetails;
import com.altafjava.platform.domain.payment.model.PaymentMethodVerificationStatus;
import com.altafjava.platform.domain.payment.service.PaymentProvider;

@TestConfiguration
@Profile("test")
public class TestPaymentConfig {

	@Bean
	@Primary
	public PaymentProvider paymentProvider() {
		return new MockPaymentProvider();
	}

	static class MockPaymentProvider implements PaymentProvider {

		@Override
		public String createCustomer(String email, String name, Map<String, String> metadata) {
			return "cus_" + UUID.randomUUID();
		}

		@Override
		public void updateCustomer(String customerId, Map<String, String> metadata) {
		}

		@Override
		public void deleteCustomer(String customerId) {
		}

		@Override
		public String createSetupIntent(String customerId) {
			return "seti_" + UUID.randomUUID() + "_secret_" + UUID.randomUUID();
		}

		@Override
		public PaymentCardDetails attachPaymentMethod(String customerId, String paymentMethodId) {
			return new PaymentCardDetails("mastercard", "1881", 3, 2031);
		}

		@Override
		public PaymentMethodVerificationStatus verifyPaymentMethod(String customerId, String paymentMethodId) {
			return PaymentMethodVerificationStatus.VERIFIED;
		}

		@Override
		public boolean payInvoice(String invoiceId) {
			return true;
		}

		@Override
		public String refundInvoice(String invoiceId, java.math.BigDecimal amount, String currency) {
			return "re_" + UUID.randomUUID();
		}

		@Override
		public void applySubscriptionCoupon(String subscriptionId, String couponId) {
		}

		@Override
		public void removeSubscriptionCoupon(String subscriptionId) {
		}

		@Override
		public void detachPaymentMethod(String paymentMethodId) {
		}

		@Override
		public void setDefaultPaymentMethod(String customerId, String paymentMethodId) {
		}

		@Override
		public String createSubscription(String customerId, String priceId, Map<String, String> metadata) {
			return "sub_" + UUID.randomUUID();
		}

		@Override
		public void cancelSubscription(String subscriptionId) {
		}

		@Override
		public String createCustomerPortalSession(String customerId, String returnUrl) {
			return "https://billing.stripe.com/session/test";
		}

		@Override
		public void setCustomerTaxId(String customerId, String type, String value) {
		}

		@Override
		public boolean verifyWebhookSignature(String payload, String signature, String secret) {
			return true;
		}

		@Override
		public String createCoupon(String duration, Integer durationInMonths, String name,
				Double percentOff, Double amountOff, String currency, Integer maxRedemptions) {
			return "coup_" + UUID.randomUUID();
		}

		@Override
		public void deleteCoupon(String couponId) {
		}

		@Override
		public String createPromotionCode(String couponId, String code) {
			return "promo_" + UUID.randomUUID();
		}
	}
}
