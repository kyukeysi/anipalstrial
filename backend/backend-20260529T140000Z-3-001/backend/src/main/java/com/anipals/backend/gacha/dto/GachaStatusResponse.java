package com.anipals.backend.gacha.dto;

import com.anipals.backend.game.dto.CurrencyResponse;

import java.util.List;

public record GachaStatusResponse(
        int ssrPity,
        int srPity,
        boolean guaranteedFeatured,
        int singleCost,
        int tenPullCost,
        CurrencyResponse currencies,
        List<GachaHistoryResponse> history,
        List<GemBundleResponse> gemBundles,
        String status
) {
}
