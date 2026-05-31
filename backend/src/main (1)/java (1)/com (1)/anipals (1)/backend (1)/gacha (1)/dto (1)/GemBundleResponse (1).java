package com.anipals.backend.gacha.dto;

public record GemBundleResponse(
        String id,
        int gems,
        int coins,
        String label
) {
}
