package dev.spironello.fichasrpg.dto;

import dev.spironello.fichasrpg.model.Pericia;
import dev.spironello.fichasrpg.model.ResultadoTeste;

/** Resultado do teste: a perícia, o valor usado (0 se a ficha não tem a perícia), a rolagem e o veredito. */
public record TestePericiaResponse(Pericia pericia, String nome, int valor, int rolagem, ResultadoTeste resultado) {
}
