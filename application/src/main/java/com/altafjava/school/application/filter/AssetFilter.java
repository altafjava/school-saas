package com.altafjava.school.application.filter;

import com.altafjava.school.domain.inventory.model.AssetStatus;

/** Narrows the asset list; every part is optional. */
public record AssetFilter(AssetStatus status, String q) {

	public static final AssetFilter NONE = new AssetFilter(null, null);
}
