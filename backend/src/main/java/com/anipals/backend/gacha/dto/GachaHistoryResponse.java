package com.anipals.backend.gacha.dto;

public record GachaHistoryResponse(
        String id,
        String result,
        String rarity,
        boolean featured,
        String time
) {
}
