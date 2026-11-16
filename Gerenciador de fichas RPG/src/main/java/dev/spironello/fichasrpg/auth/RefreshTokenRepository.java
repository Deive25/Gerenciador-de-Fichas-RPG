package dev.spironello.fichasrpg.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/** SELECT ... FOR UPDATE: duas renovações simultâneas com o mesmo token esperam uma pela outra (sem dois filhos). */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	Optional<RefreshToken> findByHash(String hash);

	/** Revoga todos os tokens ainda vivos da família (reuso detectado). Devolve quantos mudaram. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update RefreshToken t set t.revogadoEm = :quando where t.familia = :familia and t.revogadoEm is null")
	int revogarFamilia(@Param("familia") UUID familia, @Param("quando") Instant quando);
}
