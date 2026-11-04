package dev.spironello.fichasrpg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.spironello.fichasrpg.model.SheetPlayer;

@Repository
public interface SheetPlayerRepository extends JpaRepository<SheetPlayer, Long>{

}
