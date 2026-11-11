package dev.spironello.fichasrpg.model;

import jakarta.persistence.Embeddable;

/**
 * Espaços de magia de um nível: quantos a ficha tem e quantos já gastou. Valor imutável: para mudar um nível,
 * troca-se o objeto no mapa da {@link FichaDnd}. O banco garante usados &lt;= total (V8).
 */
@Embeddable
public record EspacoMagia(int total, int usados) {
}
