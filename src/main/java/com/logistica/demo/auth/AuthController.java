package com.logistica.demo.auth;

import com.logistica.demo.auth.dto.LoginRequest;
import com.logistica.demo.auth.dto.LoginResponse;
import com.logistica.demo.auth.dto.RefreshTokenRequest;
import com.logistica.demo.auth.dto.UserResponse;
import com.logistica.demo.sharedkernel.web.ApiPaths;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestController
@RequestMapping({ApiPaths.LEGACY + "/auth", ApiPaths.V1 + "/auth"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.currentUser();
    }
}
