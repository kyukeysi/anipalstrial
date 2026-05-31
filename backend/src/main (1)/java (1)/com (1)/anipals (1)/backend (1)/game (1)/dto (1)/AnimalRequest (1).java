package com.anipals.backend.game.dto;

public record AnimalRequest(
        String playerKey,
        int animalIndex
) {}