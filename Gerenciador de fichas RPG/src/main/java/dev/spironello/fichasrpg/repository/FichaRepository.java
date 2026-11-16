package dev.spironello.fichasrpg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.Ficha;

@Repository
public interface FichaRepository extends JpaRepository<Ficha, Long> {

	/** Ficha do usuário: quando o id existe mas o dono é outro, volta vazio (404, nunca 403). */
	Optional<Ficha> findByIdAndDonoId(Long id, Long donoId);

	boolean existsByIdAndDonoId(Long id, Long donoId);

	List<Ficha> findAllByDonoId(Long donoId);
}
