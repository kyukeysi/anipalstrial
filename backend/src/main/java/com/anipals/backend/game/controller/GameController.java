package com.anipals.backend.game.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.anipals.backend.game.dto.GameStateResponse;
import com.anipals.backend.game.dto.HarvestRequest;
import com.anipals.backend.game.dto.PlantRequest;
import com.anipals.backend.game.dto.TutorialRequest;
import com.anipals.backend.game.service.GameService;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/state")
    public GameStateResponse getState(@RequestParam(required = false) String playerKey) {
        return gameService.getGameState(playerKey);
    }

    @PostMapping("/farm/harvest")
    public GameStateResponse harvest(@RequestBody HarvestRequest request) {
        return gameService.harvest(request);
    }

    @PostMapping("/farm/plant")
    public GameStateResponse plant(@RequestBody PlantRequest request) {
        return gameService.plant(request);
    }

    @PostMapping("/farm/pond")
    public GameStateResponse collectPond(@RequestParam(required = false) String playerKey) {
        return gameService.collectPond(playerKey);
    }

    @PostMapping("/anipals/{aniPalId}/treat")
    public GameStateResponse giveTreat(
            @PathVariable Long aniPalId,
            @RequestParam(required = false) String playerKey,
            @RequestParam(required = false) String inventoryItemId
    ) {
        return gameService.giveTreat(playerKey, aniPalId, inventoryItemId);
    }

    @PostMapping("/tutorial")
    public GameStateResponse advanceTutorial(@RequestBody TutorialRequest request) {
        return gameService.advanceTutorial(request);
    }
}
