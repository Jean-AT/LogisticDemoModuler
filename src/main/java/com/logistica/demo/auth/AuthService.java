package com.logistica.demo.auth;

import com.logistica.demo.auth.dto.LoginRequest;
import com.logistica.demo.auth.dto.LoginResponse;
import com.logistica.demo.auth.dto.UserResponse;
import com.logistica.demo.platform.api.AccessScope;
import com.logistica.demo.shared.exception.ResourceNotFoundException;
import com.logistica.demo.shared.security.CurrentUserService;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PlatformIdentityRepository identityRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final CurrentUserService currentUserService;

    public AuthService(
            AuthenticationManager authenticationManager,
            PlatformIdentityRepository identityRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            CurrentUserService currentUserService) {
        this.authenticationManager = authenticationManager;
        this.identityRepository = identityRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        PlatformIdentity identity = findByUsername(authentication.getName());
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(identity.id());
        return toLoginResponse(identity, refreshToken.rawToken());
    }

    @Transactional
    public LoginResponse refresh(String rawToken) {
        RefreshTokenService.RefreshGrant grant = refreshTokenService.rotate(rawToken);
        return toLoginResponse(grant.identity(), grant.refreshToken());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenService.revoke(rawToken);
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser() {
        return toUserResponse(findByUsername(currentUserService.getUsername()));
    }

    private PlatformIdentity findByUsername(String username) {
        return identityRepository.findActiveByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + username));
    }

    private LoginResponse toLoginResponse(PlatformIdentity identity, String refreshToken) {
        return new LoginResponse(
                jwtService.generateAccessToken(identity), refreshToken, "Bearer",
                jwtService.expirationSeconds(), toUserResponse(identity));
    }

    private UserResponse toUserResponse(PlatformIdentity identity) {
        Set<AccessScope> scopes = identity.accessProfile().grants().stream()
                .flatMap(grant -> grant.scopes().stream())
                .collect(Collectors.toUnmodifiableSet());
        return new UserResponse(
                identity.id(), identity.username(), identity.fullName(),
                identity.accessProfile().roleCodes(), identity.accessProfile().permissions(),
                scopes, identity.active());
    }
}
