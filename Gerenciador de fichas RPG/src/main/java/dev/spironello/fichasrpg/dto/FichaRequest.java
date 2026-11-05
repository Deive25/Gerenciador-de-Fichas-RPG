package dev.spironello.fichasrpg.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import dev.spironello.fichasrpg.model.Sistema;

/**
 * Corpo de entrada do POST e do PUT de /fichas. O campo "sistema" do JSON escolhe o subtipo: o Jackson
 * lê o valor, cria o record certo e não o repassa ao record. Sistema desconhecido ("DND" por enquanto)
 * ou ausente dá 400. Cada sistema novo é um record novo aqui e uma linha em @JsonSubTypes.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "sistema")
@JsonSubTypes(@Type(value = FichaOrdemRequest.class, name = "ORDEM_PARANORMAL"))
public sealed interface FichaRequest permits FichaOrdemRequest {

	/** O sistema deste pedido. O "sistema" do JSON só escolhe o subtipo; não é um campo do record. */
	Sistema sistema();

	String jogador();

	String personagem();

	String genero();

	Integer idade();
}
