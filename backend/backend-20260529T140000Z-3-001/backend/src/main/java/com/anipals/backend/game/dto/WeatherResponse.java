package com.anipals.backend.game.dto;

public record WeatherResponse(
        String title,
        String detail,
        String temperature
) {
}
