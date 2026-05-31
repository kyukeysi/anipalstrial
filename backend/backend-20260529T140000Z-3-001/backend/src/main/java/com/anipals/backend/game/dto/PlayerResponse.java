package com.anipals.backend.game.dto;

public record PlayerResponse(
        String name,
        String uid,
        int level,
        String farmName,
        String tutorialState,
        boolean tutorialCompleted
) {
}
