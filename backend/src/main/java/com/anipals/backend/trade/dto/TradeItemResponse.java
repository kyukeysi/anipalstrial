package com.anipals.backend.trade.dto;

public record TradeItemResponse(
        String inventoryItemId,
        String name,
        String type,
        String rarity,
        int quantity,
        String color
) {
}
