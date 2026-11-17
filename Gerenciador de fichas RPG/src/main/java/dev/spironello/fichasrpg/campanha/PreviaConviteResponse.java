package dev.spironello.fichasrpg.campanha;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A prévia pública do convite: só os nomes (da campanha e do mestre) e o id para entrar. {@code demo} diz se o mestre
 * é uma conta de demonstração (o front então oferece entrar como jogador de demonstração). Aditivo de verdade: só
 * aparece quando é {@code true}, então a prévia de uma campanha real continua com as mesmas três chaves de antes
 * (o front lê a ausência como false).
 */
public record PreviaConviteResponse(Long campanhaId, String campanha, String mestre,
		@JsonInclude(JsonInclude.Include.NON_DEFAULT) boolean demo) {
}
