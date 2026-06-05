package com.anipals.backend.trade.controller;

import com.anipals.backend.trade.dto.TradeCreateRequest;
import com.anipals.backend.trade.dto.TradeOfferResponse;
import com.anipals.backend.trade.dto.TradePlayerResponse;
import com.anipals.backend.trade.dto.TradeSummaryResponse;
import com.anipals.backend.trade.service.TradeService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @GetMapping
    public TradeSummaryResponse summary(@RequestParam(required = false) String playerKey) {
        return tradeService.summary(playerKey);
    }

    @GetMapping("/search/{uid}")
    public TradePlayerResponse searchTarget(
            @PathVariable String uid,
            @RequestParam(required = false) String playerKey
    ) {
        return tradeService.searchTarget(playerKey, uid);
    }

    @PostMapping
    public TradeOfferResponse createOffer(@RequestBody TradeCreateRequest request) {
        return tradeService.createOffer(request);
    }

    @PostMapping("/{tradeId}/accept")
    public TradeOfferResponse accept(
            @PathVariable String tradeId,
            @RequestParam(required = false) String playerKey
    ) {
        return tradeService.accept(tradeId, playerKey);
    }

    @PostMapping("/{tradeId}/decline")
    public TradeOfferResponse decline(
            @PathVariable String tradeId,
            @RequestParam(required = false) String playerKey
    ) {
        return tradeService.decline(tradeId, playerKey);
    }
}
