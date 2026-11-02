package com.altafjava.school.api.controller;

import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.altafjava.platform.api.dto.response.ApiResponse;
import com.altafjava.platform.core.feature.annotation.FeatureEnabled;
import com.altafjava.platform.core.security.Roles;
import com.altafjava.school.api.controller.api.OrganizationRollupApi;
import com.altafjava.school.api.dto.response.OrganizationRollupResponse;
import com.altafjava.school.api.mapper.OrganizationRollupMapper;
import com.altafjava.school.application.rollup.OrganizationRollupService;
import com.altafjava.school.application.security.SchoolFeatures;

/**
 * Organization-level rollup reporting — aggregates attendance, fees, and enrollment across every
 * campus in a school group (see ROADMAP.md Phase 4). {@code SUPER_ADMIN} may read any
 * organization's rollup; an {@code ORG_ADMIN}/{@code ORG_OWNER}/{@code ORG_VIEWER} may read only
 * the one organization their platform {@code OrganizationMembership} grants them access to (the
 * {@code org_id} JWT claim, checked by {@code organizationAccessGuard} against the requested
 * {@code organizationPublicId} path variable) — see {@link OrganizationRollupService}'s Javadoc
 * for how per-campus reads stay correctly isolated for either caller.
 */
@RestController
@FeatureEnabled(SchoolFeatures.ORG_ROLLUP)
@RequestMapping("/api/v1/organizations/{organizationPublicId}/rollup-report")
@PreAuthorize(Roles.HAS_SUPER_ADMIN
		+ " or @organizationAccessGuard.canAccessOrganization(authentication, #organizationPublicId)")
public class OrganizationRollupController implements OrganizationRollupApi {

	private final OrganizationRollupService organizationRollupService;
	private final OrganizationRollupMapper organizationRollupMapper;

	public OrganizationRollupController(OrganizationRollupService organizationRollupService,
			OrganizationRollupMapper organizationRollupMapper) {
		this.organizationRollupService = organizationRollupService;
		this.organizationRollupMapper = organizationRollupMapper;
	}

	@Override
	@GetMapping
	public ApiResponse<OrganizationRollupResponse> get(
			@PathVariable String organizationPublicId,
			@RequestParam LocalDate from,
			@RequestParam LocalDate to) {
		return ApiResponse.success(organizationRollupMapper.toResponse(
				organizationRollupService.generate(organizationPublicId, from, to)));
	}
}
