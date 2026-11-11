package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.ItemOrdem;

@Repository
public interface ItemOrdemRepository extends JpaRepository<ItemOrdem, Long> {

	/** Os itens da ficha na ordem de criação. */
	List<ItemOrdem> findByFichaIdOrderById(Long fichaId);

	/** Um item da ficha; vazio se não existe ou é de outra ficha. */
	Optional<ItemOrdem> findByIdAndFichaId(Long id, Long fichaId);
}
