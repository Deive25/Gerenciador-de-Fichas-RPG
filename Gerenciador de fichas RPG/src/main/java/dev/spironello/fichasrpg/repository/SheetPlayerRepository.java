package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.SheetPlayer;

@Repository
public interface SheetPlayerRepository extends JpaRepository<SheetPlayer, Long>{

	// As perícias ficam em outra tabela. Sem o grafo, o Hibernate carregaria o mapa de cada ficha
	// com uma consulta extra (N+1); com ele, ficha e perícias vêm num único SELECT com LEFT JOIN.
	@Override
	@EntityGraph(attributePaths = "pericias")
	List<SheetPlayer> findAll();

	@Override
	@EntityGraph(attributePaths = "pericias")
	Optional<SheetPlayer> findById(Long id);
}
