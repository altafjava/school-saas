package com.altafjava.school.domain.certificate.model;

import java.util.regex.Pattern;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLRestriction;
import com.altafjava.platform.core.exception.BusinessException;
import com.altafjava.platform.core.model.SoftDeletableEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * A tenant-defined kind of certificate (Bonafide, Transfer, Character, ...) and its wording.
 * {@code wording} uses {@code {{placeholder}}} tokens resolved per student (see
 * {@code CertificatePlaceholderResolver}). The visual design is not stored here: it is the platform
 * document template for {@link #documentType()} ({@code CERTIFICATE.<code>}), falling back to the
 * tenant's generic {@code CERTIFICATE} design and then the built-in default. {@code code} is
 * immutable because issued documents record that document type.
 */
@Entity
@Table(name = "certificate_types")
@SQLRestriction("deleted = false")
@Getter
@SuperBuilder
@NoArgsConstructor
public class CertificateType extends SoftDeletableEntity {

	public static final String DOCUMENT_TYPE_FAMILY = "CERTIFICATE";

	private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,49}$");

	@Column(name = "code", nullable = false, length = 50, updatable = false)
	private String code;

	@Column(name = "name", nullable = false, length = 150)
	private String name;

	@Column(name = "wording", nullable = false, columnDefinition = "TEXT")
	private String wording;

	@Column(name = "active", nullable = false)
	private boolean active;

	public static CertificateType create(String code, String name, String wording) {
		if (code == null || !CODE.matcher(code).matches()) {
			throw new BusinessException("Certificate type code must be 2-50 characters of A-Z, 0-9 and _, "
					+ "starting with a letter: " + code);
		}
		return CertificateType.builder()
				.code(code)
				.name(name)
				.wording(wording)
				.active(true)
				.build();
	}

	public String documentType() {
		return DOCUMENT_TYPE_FAMILY + "." + code;
	}

	public void updateDetails(String name, String wording) {
		this.name = name;
		this.wording = wording;
	}

	public void activate() {
		this.active = true;
	}

	public void deactivate() {
		this.active = false;
	}
}
