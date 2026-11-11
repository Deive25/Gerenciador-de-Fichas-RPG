package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.RitualOrdem;

@Repository
public interface RitualOrdemRepository extends JpaRepository<RitualOrdem, Long> {

	/** Os rituais da ficha por círculo e, dentro do círculo, na ordem de criação. */
	List<RitualOrdem> findByFichaIdOrderByCirculoAscIdAsc(Long fichaId);

	/** Um ritual da ficha; vazio se não existe ou é de outra ficha. */
	Optional<RitualOrdem> findByIdAndFichaId(Long id, Long fichaId);
}
