package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.AtaqueOrdem;

@Repository
public interface AtaqueOrdemRepository extends JpaRepository<AtaqueOrdem, Long> {

	/** Os ataques da ficha na ordem de criação. */
	List<AtaqueOrdem> findByFichaIdOrderById(Long fichaId);

	/** Um ataque da ficha; vazio se não existe ou é de outra ficha. */
	Optional<AtaqueOrdem> findByIdAndFichaId(Long id, Long fichaId);
}
