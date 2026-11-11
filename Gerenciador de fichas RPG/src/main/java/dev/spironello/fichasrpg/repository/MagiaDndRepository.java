package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.MagiaDnd;

@Repository
public interface MagiaDndRepository extends JpaRepository<MagiaDnd, Long> {

	/** As magias da ficha por nível (truques primeiro) e, dentro do nível, na ordem de criação. */
	List<MagiaDnd> findByFichaIdOrderByNivelAscIdAsc(Long fichaId);

	/** Uma magia da ficha; vazio se não existe ou é de outra ficha. */
	Optional<MagiaDnd> findByIdAndFichaId(Long id, Long fichaId);
}
