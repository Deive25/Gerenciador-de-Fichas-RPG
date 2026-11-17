package dev.spironello.fichasrpg.rolagem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.spironello.fichasrpg.auth.Usuario;
import dev.spironello.fichasrpg.auth.UsuarioRepository;
import dev.spironello.fichasrpg.campanha.AcessoACampanha;
import dev.spironello.fichasrpg.dto.Referencia;

/** O histórico da campanha, para qualquer membro (quem está fora recebe o 404 do {@link AcessoACampanha}). */
@Service
public class HistoricoDeRolagensService {

	private final AcessoACampanha acesso;
	private final RolagemDaMesaRepository rolagens;
	private final UsuarioRepository usuarios;

	public HistoricoDeRolagensService(AcessoACampanha acesso, RolagemDaMesaRepository rolagens, UsuarioRepository usuarios) {
		this.acesso = acesso;
		this.rolagens = rolagens;
		this.usuarios = usuarios;
	}

	/** Da mais nova para a mais antiga; {@code antes} (opcional) é o cursor: só os ids menores que ele. */
	@Transactional(readOnly = true)
	public List<RolagemDaMesaResponse> listar(Long campanhaId, Long antes, int limite) {
		acesso.exigirMembro(campanhaId);
		Limit pagina = Limit.of(limite);
		List<RolagemDaMesa> encontradas = antes == null
				? rolagens.findByCampanhaIdOrderByIdDesc(campanhaId, pagina)
				: rolagens.findByCampanhaIdAndIdLessThanOrderByIdDesc(campanhaId, antes, pagina);
		Set<Long> autores = encontradas.stream().map(RolagemDaMesa::getUsuarioId).collect(Collectors.toSet());
		Map<Long, String> nomes = new HashMap<>();
		for (Usuario usuario : usuarios.findAllById(autores)) {
			nomes.put(usuario.getId(), usuario.getNome());
		}
		return encontradas.stream()
				.map(r -> RolagemDaMesaResponse.de(r, new Referencia(r.getUsuarioId(), nomes.get(r.getUsuarioId()))))
				.toList();
	}
}
