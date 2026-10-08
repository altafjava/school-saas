package com.altafjava.school.application.security;

/**
 * The default domain role names school-saas seeds for every tenant (see
 * {@code 008-seed-school-roles.xml}/{@code 019-seed-dashboard-roles.xml} and
 * {@code SchoolPlatformConfigurer#domainPermissionCatalog()}). Access control no longer checks
 * these names directly — every {@code @PreAuthorize} resolves a permission code via
 * {@code PermissionAuthorizationService} instead, so a tenant can grant the same access to a
 * custom role. These constants remain for the few call sites that legitimately need a role by
 * name rather than by permission: routing a notification to "whoever holds FINANCE"
 * ({@code FeeDefaultRiskRuleEvaluator}) and assigning the default role to a newly self-registered
 * account ({@code GuardianSelfRegistrationService}).
 */
public final class SchoolRoles {

	public static final String TEACHER = "TEACHER";
	public static final String PARENT = "PARENT";
	public static final String STUDENT = "STUDENT";
	public static final String PRINCIPAL = "PRINCIPAL";
	public static final String FINANCE = "FINANCE";
	public static final String HR = "HR";
	public static final String ACADEMIC = "ACADEMIC";

	private SchoolRoles() {
	}
}
