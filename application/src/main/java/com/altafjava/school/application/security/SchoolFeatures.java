package com.altafjava.school.application.security;

import java.util.Set;
import com.altafjava.platform.core.feature.FeatureDefinition;

/** The school modules a tenant's plan switches on, used with {@code FeatureEnabled}. */
public final class SchoolFeatures {

	public static final String PAYROLL = "SCHOOL_PAYROLL";
	public static final String CERTIFICATES = "SCHOOL_CERTIFICATES";
	public static final String BULK_IMPORT = "SCHOOL_BULK_IMPORT";
	public static final String ORG_ROLLUP = "SCHOOL_ORG_ROLLUP";
	public static final String CAMPUS_OPS = "SCHOOL_CAMPUS_OPS";

	private static final String PROFESSIONAL = "Professional";
	private static final String ENTERPRISE = "Enterprise";

	public static final Set<FeatureDefinition> CATALOG = Set.of(
			new FeatureDefinition(PAYROLL, "HR payroll module: salary structures, payslips, pay-component catalog",
					Set.of(ENTERPRISE)),
			new FeatureDefinition(CERTIFICATES, "Digital certificate templates, issuance, and third-party verification",
					Set.of(PROFESSIONAL, ENTERPRISE)),
			new FeatureDefinition(BULK_IMPORT,
					"CSV import of students, staff, guardians and opening fee balances, for onboarding and migration",
					Set.of(PROFESSIONAL, ENTERPRISE)),
			new FeatureDefinition(ORG_ROLLUP, "Organization-wide multi-campus rollup reporting", Set.of(ENTERPRISE)),
			new FeatureDefinition(CAMPUS_OPS, "Hostel and student health-record modules",
					Set.of(PROFESSIONAL, ENTERPRISE)));

	private SchoolFeatures() {
	}
}
