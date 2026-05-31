package com.anipals.backend.game.controller;

import com.anipals.backend.game.dto.InventoryItemResponse;
import com.anipals.backend.game.dto.GameStateResponse;
import com.anipals.backend.game.dto.InventoryUseRequest;
import com.anipals.backend.game.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final GameService gameService;

    public InventoryController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<InventoryItemResponse> getInventory(@RequestParam(required = false) String playerKey) {
        return gameService.getGameState(playerKey).inventory();
    }

    @PostMapping("/use")
    public GameStateResponse useItem(@RequestBody InventoryUseRequest request) {
        return gameService.useInventoryItem(request);
    }
}
