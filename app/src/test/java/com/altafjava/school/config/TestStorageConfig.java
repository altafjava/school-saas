package com.altafjava.school.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import com.altafjava.platform.domain.file.service.StorageService;

@TestConfiguration
@Profile("test")
public class TestStorageConfig {

	@Bean
	@Primary
	public StorageService storageService() {
		return new InMemoryStorageService();
	}

	// Real (in-process) storage rather than a Mockito stub — ReportCardService's tests need to
	// read back the exact bytes a prior call uploaded, which a stubbed-return mock can't support.
	public static class InMemoryStorageService implements StorageService {

		private final Map<String, byte[]> files = new ConcurrentHashMap<>();

		@Override
		public URL generatePresignedUploadUrl(String key, String contentType, Map<String, String> metadata) {
			return dummyUrl();
		}

		@Override
		public URL generatePresignedDownloadUrl(String key, Duration duration) {
			return dummyUrl();
		}

		@Override
		public void deleteFile(String key) {
			files.remove(key);
		}

		@Override
		public long getFileSize(String key) {
			byte[] content = files.get(key);
			return content == null ? 0 : content.length;
		}

		@Override
		public void uploadFile(String key, byte[] content, String contentType) {
			files.put(key, content);
		}

		@Override
		public void uploadFile(String key, Path file, String contentType) {
			try {
				files.put(key, Files.readAllBytes(file));
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}

		@Override
		public List<String> listKeys(String prefix) {
			return files.keySet().stream().filter(key -> key.startsWith(prefix)).sorted().toList();
		}

		@Override
		public long deleteByPrefix(String prefix) {
			List<String> keys = listKeys(prefix);
			keys.forEach(files::remove);
			return keys.size();
		}

		@Override
		public byte[] downloadFile(String key) {
			return files.getOrDefault(key, new byte[0]);
		}

		private URL dummyUrl() {
			try {
				return URI.create("https://example.com/dummy-file").toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
