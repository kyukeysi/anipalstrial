package com.anipals.backend.gacha.controller;

import com.anipals.backend.gacha.dto.GachaPullRequest;
import com.anipals.backend.gacha.dto.GachaStatusResponse;
import com.anipals.backend.gacha.dto.GemPurchaseRequest;
import com.anipals.backend.gacha.service.GachaService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gacha")
public class GachaController {

    private final GachaService gachaService;

    public GachaController(GachaService gachaService) {
        this.gachaService = gachaService;
    }

    @GetMapping("/status")
    public GachaStatusResponse getStatus(@RequestParam(required = false) String playerKey) {
        return gachaService.getStatus(playerKey);
    }

    @PostMapping("/pull")
    public GachaStatusResponse pull(@RequestBody GachaPullRequest request) {
        return gachaService.pull(request);
    }

    @PostMapping("/gems")
    public GachaStatusResponse buyGems(@RequestBody GemPurchaseRequest request) {
        return gachaService.buyGems(request);
    }
}
