package com.anipals.backend.game.controller;

import com.anipals.backend.game.dto.GameStateResponse;
import com.anipals.backend.game.dto.PlayerNameRequest;
import com.anipals.backend.game.service.GameService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    private final GameService gameService;

    public PlayerController(GameService gameService) {
        this.gameService = gameService;
    }

    @PatchMapping("/name")
    public GameStateResponse updateName(@RequestBody PlayerNameRequest request) {
        return gameService.updatePlayerName(request);
    }
}
