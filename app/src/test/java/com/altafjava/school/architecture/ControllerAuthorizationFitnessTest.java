package com.altafjava.school.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Architecture fitness test guarding against a controller shipping with no {@code @PreAuthorize}
 * at all, relying solely on the platform's blanket {@code anyRequest().authenticated()} rule —
 * which blocks anonymous access but not cross-role or cross-tenant access within the authenticated
 * population. Mirrors {@code platform-saas}'s own {@code ControllerAuthorizationFitnessTest},
 * which exists specifically because that exact gap was found and fixed across 13+ platform
 * controllers in the 2026-08-14/15 production-readiness reviews (see
 * {@code platform-saas/CHANGELOG.md}). Every school-saas controller currently declares
 * {@code @PreAuthorize} on every endpoint, so both allowlists below start empty — add an entry
 * only alongside a matching {@code SecurityConfig}/{@code permitAll()} change, never silently.
 * <p>
 * {@code AdmissionController#apply} is the one deliberate exception to "never silently": a
 * prospective guardian applying before any account exists cannot present a JWT, so it is
 * {@code permitAll()} via a literal entry in platform-saas's {@code SecurityConfig}.
 * {@code GuardianSelfRegistrationController} is the same kind of deliberate, flagged exception,
 * mirroring platform's {@code AuthController#register} for the same reason (no account yet to
 * carry a JWT). {@code FeePaymentWebhookController} is the same kind of deliberate, flagged
 * exception: an inbound payment-gateway webhook callback carries no JWT at all, so it is
 * {@code permitAll()} via a generic {@code /api/v1/*}{@code /webhooks/**} pattern in platform's
 * {@code SecurityConfig}, and enforces its own trust boundary via gateway signature verification
 * instead of {@code @PreAuthorize}.
 */
@AnalyzeClasses(packages = "com.altafjava.school.api")
class ControllerAuthorizationFitnessTest {

	private static final Set<String> KNOWN_PUBLIC_CONTROLLERS = Set.of(
			"com.altafjava.school.api.controller.GuardianSelfRegistrationController",
			"com.altafjava.school.api.controller.FeePaymentWebhookController");

	private static final Set<String> KNOWN_PUBLIC_ENDPOINTS = Set.of(
			"com.altafjava.school.api.controller.AdmissionController#apply");

	@ArchTest
	static final ArchRule controllersMustDeclareAuthorization = classes()
			.that().areAnnotatedWith(RestController.class)
			.and().resideInAPackage("com.altafjava.school.api.controller..")
			.should(haveAuthorizationOnClassOrEveryHttpMethod())
			.because("a controller with no @PreAuthorize anywhere relies solely on the blanket "
					+ "anyRequest().authenticated() rule, which blocks anonymous access but not cross-role "
					+ "or cross-tenant access within the authenticated population");

	private static ArchCondition<JavaClass> haveAuthorizationOnClassOrEveryHttpMethod() {
		return new ArchCondition<>("declare @PreAuthorize at class level, be a known-public controller, "
				+ "or annotate every HTTP-mapped method (itself or as a known-public endpoint)") {
			@Override
			public void check(JavaClass javaClass, ConditionEvents events) {
				if (javaClass.isAnnotatedWith(PreAuthorize.class)) {
					return;
				}
				if (KNOWN_PUBLIC_CONTROLLERS.contains(javaClass.getFullName())) {
					return;
				}
				for (JavaMethod method : javaClass.getMethods()) {
					if (isHttpEndpoint(method) && !isAuthorized(javaClass, method)) {
						events.add(SimpleConditionEvent.violated(method,
								method.getFullName()
										+ "() is an HTTP endpoint with no @PreAuthorize, its class has none "
										+ "either, and it is not listed in KNOWN_PUBLIC_ENDPOINTS"));
					}
				}
			}

			private boolean isHttpEndpoint(JavaMethod method) {
				return method.isAnnotatedWith(GetMapping.class)
						|| method.isAnnotatedWith(PostMapping.class)
						|| method.isAnnotatedWith(PutMapping.class)
						|| method.isAnnotatedWith(PatchMapping.class)
						|| method.isAnnotatedWith(DeleteMapping.class)
						|| method.isAnnotatedWith(RequestMapping.class);
			}

			private boolean isAuthorized(JavaClass javaClass, JavaMethod method) {
				return method.isAnnotatedWith(PreAuthorize.class)
						|| KNOWN_PUBLIC_ENDPOINTS.contains(javaClass.getFullName() + "#" + method.getName());
			}
		};
	}
}
