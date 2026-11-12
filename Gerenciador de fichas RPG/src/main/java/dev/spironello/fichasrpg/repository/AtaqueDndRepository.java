package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.AtaqueDnd;

@Repository
public interface AtaqueDndRepository extends JpaRepository<AtaqueDnd, Long> {

	/** Os ataques da ficha na ordem de criação. */
	List<AtaqueDnd> findByFichaIdOrderById(Long fichaId);

	/** Um ataque da ficha; vazio se não existe ou é de outra ficha. */
	Optional<AtaqueDnd> findByIdAndFichaId(Long id, Long fichaId);
}
