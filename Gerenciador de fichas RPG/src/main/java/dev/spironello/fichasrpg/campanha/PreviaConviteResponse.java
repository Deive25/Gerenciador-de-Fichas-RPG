package dev.spironello.fichasrpg.campanha;

/** A prévia pública do convite: só os nomes (da campanha e do mestre) e o id para entrar. */
public record PreviaConviteResponse(Long campanhaId, String campanha, String mestre) {
}
