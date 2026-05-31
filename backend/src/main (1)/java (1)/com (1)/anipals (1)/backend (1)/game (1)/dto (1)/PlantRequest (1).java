package com.anipals.backend.game.dto;

public record PlantRequest(
        String playerKey,
        int plotIndex,
        String inventoryItemId
) {
}
