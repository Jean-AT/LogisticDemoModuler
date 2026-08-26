package com.logistica.demo.maestros.repository;

import com.logistica.demo.maestros.domain.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsernameIgnoreCaseAndActiveTrue(String username);

    Optional<Usuario> findByUsernameIgnoreCase(String username);
}
