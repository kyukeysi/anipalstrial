package com.anipals.backend.game.dto;

public record AniPalResponse(
        String id,
        String name,
        String species,
        String role,
        String mood,
        int level,
        String palette,
        String activeBoost
) {
}
