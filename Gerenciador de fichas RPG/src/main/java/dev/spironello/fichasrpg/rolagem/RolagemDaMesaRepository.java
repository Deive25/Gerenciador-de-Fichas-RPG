package dev.spironello.fichasrpg.rolagem;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolagemDaMesaRepository extends JpaRepository<RolagemDaMesa, Long> {

	/** As mais novas da campanha primeiro (o índice ix_rolagem_campanha segue esta ordem). */
	List<RolagemDaMesa> findByCampanhaIdOrderByIdDesc(Long campanhaId, Limit limite);

	/** A página seguinte do cursor: só as de id menor que o da última que quem pediu já tem. */
	List<RolagemDaMesa> findByCampanhaIdAndIdLessThanOrderByIdDesc(Long campanhaId, Long antes, Limit limite);
}
