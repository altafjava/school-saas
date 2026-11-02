package com.altafjava.school.realinfrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.altafjava.platform.application.dto.tenant.DataExportStatusResponse;
import com.altafjava.platform.application.tenantdata.TenantDataPaths;
import com.altafjava.platform.application.tenantdata.TenantDataScope;
import com.altafjava.platform.application.tenantdata.TenantDataStore;
import com.altafjava.platform.application.tenantdata.TenantDeletionService;
import com.altafjava.platform.application.tenantdata.TenantExportService;
import com.altafjava.platform.application.tenantdata.TenantSearchIndex;
import com.altafjava.platform.domain.file.service.StorageService;
import com.altafjava.platform.domain.tenant.model.Tenant;
import com.altafjava.platform.domain.tenant.model.TenantDeletionRecord;
import com.altafjava.platform.domain.tenant.model.TenantDeletionStatus;
import com.altafjava.platform.domain.tenant.model.TenantSummary;
import com.altafjava.platform.domain.tenant.repository.TenantRepository;
import com.altafjava.school.base.SchoolInfrastructureTestBase;
import com.altafjava.school.config.TestStorageConfig;
import com.altafjava.school.demo.DemoDataSeeder;

/**
 * Exports and then deletes a fully seeded school next to the demo one. The real schema's foreign
 * keys decide whether deletion works, so this runs on real data, not a handful of rows.
 */
@Import(SchoolTenantDataLifecycleTest.ObjectStoreConfig.class)
class SchoolTenantDataLifecycleTest extends SchoolInfrastructureTestBase {

	// The dev profile points at a real object store, which this tier does not run.
	@TestConfiguration
	static class ObjectStoreConfig {
		@Bean
		@Primary
		StorageService storageService() {
			return new TestStorageConfig.InMemoryStorageService();
		}
	}

	@Autowired
	private DemoDataSeeder seeder;
	@Autowired
	private TenantRepository tenantRepository;
	@Autowired
	private TenantDataStore dataStore;
	@Autowired
	private TenantExportService exportService;
	@Autowired
	private TenantDeletionService deletionService;
	@Autowired
	private StorageService storageService;
	@MockitoBean
	private TenantSearchIndex tenantSearchIndex;

	@Test
	void aWholeSchool_isExportedAndThenCompletelyRemoved() throws IOException {
		Tenant school = seeder.seed("doomed-" + UUID.randomUUID().toString().substring(0, 8));
		TenantDataScope scope = TenantDataScope.of(TenantSummary.of(school));
		Tenant demo = tenantRepository.findBySubdomain(DemoDataSeeder.SUBDOMAIN).orElseThrow();
		TenantDataScope demoScope = TenantDataScope.of(TenantSummary.of(demo));
		Map<String, Long> before = dataStore.countRows(scope);
		Map<String, Long> demoBefore = dataStore.countRows(demoScope);
		assertThat(before).containsKeys("students", "fee_assignments", "grades", "attendance", "teachers");

		Map<String, String> archive = export(school);
		assertThat(archive).containsKeys("manifest.json", "data/students.jsonl", "data/grades.jsonl",
				"data/fee_assignments.jsonl");
		assertThat(archive.get("data/students.jsonl")).contains("student1@demo.school");

		TenantDeletionRecord proof = deletionService.deleteTenant(school.getId()).orElseThrow();

		assertThat(proof.getStatus()).isEqualTo(TenantDeletionStatus.COMPLETED);
		assertThat(proof.getRowsRemoved()).isGreaterThanOrEqualTo(sum(before));
		assertThat(dataStore.countRows(scope)).isEmpty();
		assertThat(tenantRepository.findById(school.getId())).isEmpty();
		assertThat(storageService.listKeys(TenantDataPaths.tenantPrefix(school.getId()))).isEmpty();
		assertThat(dataStore.countRows(demoScope)).containsAllEntriesOf(demoBefore);
	}

	private Map<String, String> export(Tenant school) throws IOException {
		DataExportStatusResponse requested = exportService.requestExport(school.getId());
		await().atMost(Duration.ofMinutes(2)).until(() -> exportService.findLatest(school.getId()).orElseThrow()
				.status().equals("COMPLETED"));
		byte[] bytes = storageService.downloadFile(TenantDataPaths.exportKey(school.getId(), requested.id()));
		Map<String, String> entries = new HashMap<>();
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				entries.put(entry.getName(), new String(zip.readAllBytes(), StandardCharsets.UTF_8));
			}
		}
		return entries;
	}

	private long sum(Map<String, Long> counts) {
		return counts.values().stream().mapToLong(Long::longValue).sum();
	}
}
