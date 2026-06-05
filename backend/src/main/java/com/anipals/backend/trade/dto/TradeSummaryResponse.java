package com.anipals.backend.trade.dto;

import java.util.List;

public record TradeSummaryResponse(
        List<TradePlayerResponse> friends,
        List<TradeOfferResponse> incoming,
        List<TradeOfferResponse> outgoing,
        List<TradeOfferResponse> history
) {
}
