package com.altafjava.school.application.announcement;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.domain.file.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;

/** Attachments are uploaded through the file API first; this proves each one is this tenant's file. */
@Component
@RequiredArgsConstructor
public class AttachmentValidator {

	private final FileMetadataRepository fileMetadataRepository;

	public List<UUID> requireExisting(Collection<String> filePublicIds) {
		try {
			List<UUID> ids = filePublicIds.stream().map(UUID::fromString).toList();
			if (!ids.stream().allMatch(id -> fileMetadataRepository.findByPublicId(id).isPresent())) {
				throw new BusinessException("An attachment was not found");
			}
			return ids;
		} catch (IllegalArgumentException e) {
			throw new BusinessException("An attachment id is not valid");
		}
	}
}
