package com.urijip.server.global.security;

public record AccessTokenClaims(Long userId, String role) {
}
