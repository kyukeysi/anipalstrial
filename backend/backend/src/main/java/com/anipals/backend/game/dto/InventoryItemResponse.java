package com.anipals.backend.game.dto;

public record InventoryItemResponse(
        String id,
        String name,
        String type,
        String rarity,
        int quantity,
        String color
) {
}
