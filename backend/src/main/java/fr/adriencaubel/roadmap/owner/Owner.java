package fr.adriencaubel.roadmap.owner;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** A person signed in with a provider; identified by (provider, providerSubject). */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Owner {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String provider;

	@Column(nullable = false)
	private String providerSubject;

	private String email;

	private String name;

	@Column(nullable = false)
	private Instant createdAt;

	public Owner(String provider, String providerSubject, String email, String name) {
		this.provider = provider;
		this.providerSubject = providerSubject;
		this.email = email;
		this.name = name;
		this.createdAt = Instant.now();
	}
}
