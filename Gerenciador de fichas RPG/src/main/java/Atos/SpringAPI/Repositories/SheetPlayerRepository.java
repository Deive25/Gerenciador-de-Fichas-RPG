package Atos.SpringAPI.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import Atos.SpringAPI.Models.SheetPlayer;

@Repository
public interface SheetPlayerRepository extends JpaRepository<SheetPlayer, Long>{

}
