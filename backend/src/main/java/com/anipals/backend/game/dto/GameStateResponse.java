package com.anipals.backend.game.dto;

import java.util.List;

public record GameStateResponse(
        PlayerResponse player,
        CurrencyResponse currencies,
        WeatherResponse weather,
        List<AniPalResponse> anipals,
        List<InventoryItemResponse> inventory,
        List<QuestResponse> quests,
        List<FarmPlotResponse> farmPlots,
        PondStatusResponse pond,
        String status
) {
}
