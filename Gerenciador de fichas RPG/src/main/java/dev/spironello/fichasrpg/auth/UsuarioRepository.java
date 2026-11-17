package dev.spironello.fichasrpg.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

	Optional<Usuario> findByEmail(String email);

	Optional<Usuario> findByGoogleSub(String googleSub);

	boolean existsByEmail(String email);
}
