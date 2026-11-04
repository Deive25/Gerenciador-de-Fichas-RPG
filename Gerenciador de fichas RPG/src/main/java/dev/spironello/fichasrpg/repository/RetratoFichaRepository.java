package dev.spironello.fichasrpg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.RetratoFicha;

@Repository
public interface RetratoFichaRepository extends JpaRepository<RetratoFicha, Long> {
}
