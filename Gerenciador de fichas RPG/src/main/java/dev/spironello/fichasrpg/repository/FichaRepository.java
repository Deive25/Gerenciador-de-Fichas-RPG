package dev.spironello.fichasrpg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.Ficha;

@Repository
public interface FichaRepository extends JpaRepository<Ficha, Long> {
}
