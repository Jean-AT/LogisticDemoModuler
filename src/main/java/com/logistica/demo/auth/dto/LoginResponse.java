package com.logistica.demo.auth.dto;

public record LoginResponse(String token, String tokenType, UserResponse user) {
}
