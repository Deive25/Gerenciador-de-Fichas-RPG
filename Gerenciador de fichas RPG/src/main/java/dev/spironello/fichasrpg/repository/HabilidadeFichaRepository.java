package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.HabilidadeFicha;

@Repository
public interface HabilidadeFichaRepository extends JpaRepository<HabilidadeFicha, Long> {

	/** As habilidades da ficha na ordem de criação. */
	List<HabilidadeFicha> findByFichaIdOrderById(Long fichaId);

	/** Uma habilidade da ficha; vazio se não existe ou é de outra ficha. */
	Optional<HabilidadeFicha> findByIdAndFichaId(Long id, Long fichaId);
}
