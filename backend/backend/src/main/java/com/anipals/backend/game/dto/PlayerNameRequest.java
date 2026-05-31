package com.anipals.backend.game.dto;

public record PlayerNameRequest(
        String playerKey,
        String name,
        String farmName
) {
}
