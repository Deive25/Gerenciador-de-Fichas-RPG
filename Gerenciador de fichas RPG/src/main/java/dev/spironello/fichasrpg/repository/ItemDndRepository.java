package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.ItemDnd;

@Repository
public interface ItemDndRepository extends JpaRepository<ItemDnd, Long> {

	/** Os itens da ficha na ordem de criação. */
	List<ItemDnd> findByFichaIdOrderById(Long fichaId);

	/** Um item da ficha; vazio se não existe ou é de outra ficha. */
	Optional<ItemDnd> findByIdAndFichaId(Long id, Long fichaId);
}
