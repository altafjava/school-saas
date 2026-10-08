package com.altafjava.school.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;
import com.altafjava.platform.core.tenant.TenantContext;
import com.altafjava.platform.core.tenant.TenantType;
import com.altafjava.platform.domain.file.model.FileMetadata;
import com.altafjava.platform.domain.file.model.FileStatus;
import com.altafjava.platform.domain.file.model.VirusScanStatus;
import com.altafjava.platform.domain.file.repository.FileMetadataRepository;
import com.altafjava.platform.domain.file.service.StorageService;

/** Puts a small real PNG into the tenant's file store, the way an uploaded photo would be. */
@Component
public class TestPhotos {

	private final FileMetadataRepository fileMetadataRepository;
	private final StorageService storageService;

	public TestPhotos(FileMetadataRepository fileMetadataRepository, StorageService storageService) {
		this.fileMetadataRepository = fileMetadataRepository;
		this.storageService = storageService;
	}

	public UUID store(Long tenantId) {
		byte[] png = tinyPng();
		String key = "tenants/" + tenantId + "/test-photos/" + UUID.randomUUID() + ".png";
		storageService.uploadFile(key, png, "image/png");
		TenantContext.ForTesting.setCurrentTenant(tenantId, null, null, TenantType.SHARED);
		try {
			FileMetadata metadata = FileMetadata.builder()
					.tenantId(tenantId)
					.userId("1")
					.entityType("TEST_PHOTO")
					.entityId("1")
					.filename("photo.png")
					.s3Key(key)
					.size((long) png.length)
					.mimeType("image/png")
					.status(FileStatus.ACTIVE)
					.virusScanStatus(VirusScanStatus.CLEAN)
					.build();
			return fileMetadataRepository.save(metadata).getPublicId();
		} finally {
			TenantContext.ForTesting.clear();
		}
	}

	private byte[] tinyPng() {
		BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "png", out);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return out.toByteArray();
	}
}
