package dev.spironello.fichasrpg.campanha;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

	/** Convite pelo código; o serviço confere se está ativo. */
	Optional<Campanha> findByCodigoConvite(String codigoConvite);
}
