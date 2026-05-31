package com.anipals.backend.game.dto;

import java.time.LocalDateTime;

public record PondStatusResponse(
        String state,
        LocalDateTime readyAt,
        long secondsUntilReady
) {
}
