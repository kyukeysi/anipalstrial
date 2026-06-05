package com.anipals.backend.trade.dto;

public record TradeItemRequest(
        String inventoryItemId,
        int quantity
) {
}
