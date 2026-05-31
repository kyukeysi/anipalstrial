package com.anipals.backend.game.dto;

public record CurrencyResponse(
        int coins,
        int gems,
        int energy,
        int sprouts,
        int tickets
) {
}
