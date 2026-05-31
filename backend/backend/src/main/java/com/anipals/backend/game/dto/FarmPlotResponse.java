package com.anipals.backend.game.dto;

public record FarmPlotResponse(
        int plotIndex,
        String crop,
        String state
) {
}
