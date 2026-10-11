package fr.adriencaubel.roadmap.owner;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OwnerRepository extends JpaRepository<Owner, Long> {

	Optional<Owner> findByProviderAndProviderSubject(String provider, String providerSubject);
}
