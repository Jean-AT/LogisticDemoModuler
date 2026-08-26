package com.logistica.demo.auth;

import com.logistica.demo.auth.dto.LoginRequest;
import com.logistica.demo.auth.dto.LoginResponse;
import com.logistica.demo.auth.dto.UserResponse;
import com.logistica.demo.maestros.domain.Usuario;
import com.logistica.demo.maestros.repository.UsuarioRepository;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UsuarioRepository usuarioRepository,
            JwtService jwtService,
            CurrentUserService currentUserService) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()));

        Usuario usuario = findByUsername(authentication.getName());
        UserResponse user = toUserResponse(usuario);
        String token = jwtService.generateToken(
                usuario.getUsername(),
                usuario.getRole().name(),
                usuario.getFullName());
        return new LoginResponse(token, "Bearer", user);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return toUserResponse(findByUsername(currentUserService.getUsername()));
    }

    private Usuario findByUsername(String username) {
        return usuarioRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private UserResponse toUserResponse(Usuario usuario) {
        return new UserResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getFullName(),
                usuario.getRole(),
                usuario.isActive());
    }
}
