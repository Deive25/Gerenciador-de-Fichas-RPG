package dev.spironello.fichasrpg.campanha;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipacaoRepository extends JpaRepository<Participacao, ParticipacaoId> {

	Optional<Participacao> findByCampanhaIdAndUsuarioId(Long campanhaId, Long usuarioId);

	Optional<Participacao> findByCampanhaIdAndPapel(Long campanhaId, Papel papel);

	/** Os mestres de várias campanhas de uma vez (um por campanha). */
	List<Participacao> findAllByCampanhaIdInAndPapel(Collection<Long> campanhaIds, Papel papel);

	/** Os membros da campanha; quem monta a lista põe o mestre primeiro (ordenar por "papel" no banco poria JOGADOR antes). */
	List<Participacao> findAllByCampanhaId(Long campanhaId);

	List<Participacao> findAllByUsuarioId(Long usuarioId);
}
