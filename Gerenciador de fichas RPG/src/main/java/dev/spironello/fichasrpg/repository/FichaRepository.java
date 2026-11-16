package dev.spironello.fichasrpg.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.campanha.Papel;
import dev.spironello.fichasrpg.model.Ficha;

/**
 * Só o {@code AcessoAFicha} e o {@code FichaService} usam este repository (ADR 0024): quem decide quem pode ver uma
 * ficha é o {@code AcessoAFicha}, e uma consulta feita aqui fora dele fugiria da regra.
 */
@Repository
public interface FichaRepository extends JpaRepository<Ficha, Long> {

	List<Ficha> findAllByDonoId(Long donoId);

	/** Todas as fichas da mesa: o que o mestre vê. */
	List<Ficha> findAllByCampanhaIdOrderById(Long campanhaId);

	/** As fichas da mesa sem as do mestre: o que o jogador vê (as do mestre ficam ocultas). Uma consulta só. */
	@Query("select f from Ficha f where f.campanhaId = :campanhaId and not exists ("
			+ "select 1 from Participacao p where p.campanhaId = f.campanhaId and p.usuarioId = f.donoId and p.papel = :mestre) "
			+ "order by f.id")
	List<Ficha> findDosJogadoresDaCampanha(@Param("campanhaId") Long campanhaId, @Param("mestre") Papel mestre);

	/**
	 * Por campanha: [id, quantas fichas há, quantas delas são do mestre]. O jogador vê a primeira menos a segunda; o
	 * mestre vê a primeira. Uma consulta para todas as campanhas.
	 */
	@Query("select f.campanhaId, count(f), count(case when p.papel = :mestre then 1 end) "
			+ "from Ficha f join Participacao p on p.campanhaId = f.campanhaId and p.usuarioId = f.donoId "
			+ "where f.campanhaId in :campanhaIds group by f.campanhaId")
	List<Object[]> contarPorCampanha(@Param("campanhaIds") Collection<Long> campanhaIds, @Param("mestre") Papel mestre);
}
