package com.anipals.backend.trade.dto;

import java.util.List;

public record TradeCreateRequest(
        String playerKey,
        String targetUid,
        List<TradeItemRequest> items
) {
}
