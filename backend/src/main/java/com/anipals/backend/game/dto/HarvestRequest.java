package com.anipals.backend.game.dto;

public record HarvestRequest(
        String playerKey,
        int plotIndex
) {
}
