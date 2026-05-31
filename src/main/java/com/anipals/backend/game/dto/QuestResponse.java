package com.anipals.backend.game.dto;

public record QuestResponse(
        String id,
        String title,
        String reward,
        String progress
) {
}
