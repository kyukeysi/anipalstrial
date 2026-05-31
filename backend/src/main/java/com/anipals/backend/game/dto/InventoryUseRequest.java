package com.anipals.backend.game.dto;

public record InventoryUseRequest(
        String playerKey,
        String inventoryItemId
) {
}
