package com.anipals.backend.trade.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TradeOfferResponse(
        String id,
        String status,
        TradePlayerResponse initiator,
        TradePlayerResponse recipient,
        boolean initiatorAccepted,
        boolean recipientAccepted,
        List<TradeItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String message
) {
}
