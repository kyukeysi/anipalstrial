package com.anipals.backend.auth.dto;

public record AuthResponse(
        String message,
        String email,
        String playerKey,
        String uid,
        boolean tutorialCompleted
) {
}
