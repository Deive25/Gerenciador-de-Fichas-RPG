package dev.spironello.fichasrpg.campanha;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParticipacaoRepository extends JpaRepository<Participacao, ParticipacaoId> {

	Optional<Participacao> findByCampanhaIdAndUsuarioId(Long campanhaId, Long usuarioId);

	Optional<Participacao> findByCampanhaIdAndPapel(Long campanhaId, Papel papel);

	/** Os mestres de várias campanhas de uma vez (um por campanha). */
	List<Participacao> findAllByCampanhaIdInAndPapel(Collection<Long> campanhaIds, Papel papel);

	/** Os membros da campanha; quem monta a lista põe o mestre primeiro (ordenar por "papel" no banco poria JOGADOR antes). */
	List<Participacao> findAllByCampanhaId(Long campanhaId);

	List<Participacao> findAllByUsuarioId(Long usuarioId);

	/** O mestre da campanha é uma conta de demonstração? (a campanha demo e a prévia do convite perguntam isto) */
	@Query("select count(p) > 0 from Participacao p, Usuario u where p.campanhaId = :campanhaId and p.papel = :mestre "
			+ "and u.id = p.usuarioId and u.demo = true")
	boolean mestreEhDemo(@Param("campanhaId") Long campanhaId, @Param("mestre") Papel mestre);

	/**
	 * Entra como jogador, uma vez só: se já existe a participação (de jogador ou do mestre) não faz nada e devolve 0. O
	 * "on conflict" torna o aceite idempotente até com dois cliques ao mesmo tempo.
	 */
	@Modifying
	@Query(value = "insert into participacao (campanha_id, usuario_id, papel, entrou_em) "
			+ "values (:campanhaId, :usuarioId, 'JOGADOR', :entrouEm) on conflict do nothing", nativeQuery = true)
	int entrarComoJogador(@Param("campanhaId") Long campanhaId, @Param("usuarioId") Long usuarioId,
			@Param("entrouEm") Instant entrouEm);
}
