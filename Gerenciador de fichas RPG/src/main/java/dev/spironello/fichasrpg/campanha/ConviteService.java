package dev.spironello.fichasrpg.campanha;

import org.springframework.stereotype.Service;

/** Convites por link: gerar o código, renovar, desativar, a prévia pública e aceitar. */
@Service
public class ConviteService {

	private static final int TENTATIVAS = 3;

	private final CampanhaRepository campanhas;

	public ConviteService(CampanhaRepository campanhas) {
		this.campanhas = campanhas;
	}

	/**
	 * Um código que ainda não existe, com até 3 tentativas. A conferência vem antes de gravar de propósito: uma colisão
	 * do {@code unique} só apareceria no flush e deixaria a transação inteira inutilizável. Com 71 bits, a segunda
	 * tentativa nunca acontece na prática; o {@code unique} fica como rede de segurança.
	 */
	String novoCodigoLivre() {
		for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
			String codigo = CodigoDeConvite.gerar();
			if (!campanhas.existsByCodigoConvite(codigo)) {
				return codigo;
			}
		}
		throw new IllegalStateException("Não foi possível gerar um código de convite novo");
	}
}
