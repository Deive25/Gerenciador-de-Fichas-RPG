package dev.spironello.fichasrpg.dto;

/** Um ataque na resposta. Selada: cada sistema tem o seu record, e um caso novo exige tratar o tipo novo. */
public sealed interface AtaqueResponse permits AtaqueOrdemResponse, AtaqueDndResponse {

	Long id();
}
