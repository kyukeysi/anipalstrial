package com.anipals.backend.gacha.dto;

public record GemPurchaseRequest(
        String playerKey,
        String bundleId
) {
}
