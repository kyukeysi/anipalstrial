package com.anipals.backend.game.dto;

public record MiniGameRewardRequest(
        String playerKey,
        String gameId,
        int coins
) {
}
