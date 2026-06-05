package com.anipals.backend.trade.dto;

public record TradePlayerResponse(
        String uid,
        String name,
        String farm,
        String status
) {
}
