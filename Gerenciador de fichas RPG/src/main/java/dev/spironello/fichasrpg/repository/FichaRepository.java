package dev.spironello.fichasrpg.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.Ficha;

/**
 * Só o {@code AcessoAFicha} e o {@code FichaService} usam este repository (ADR 0024): quem decide quem pode ver uma
 * ficha é o {@code AcessoAFicha}, e uma consulta feita aqui fora dele fugiria da regra.
 */
@Repository
public interface FichaRepository extends JpaRepository<Ficha, Long> {

	List<Ficha> findAllByDonoId(Long donoId);
}
