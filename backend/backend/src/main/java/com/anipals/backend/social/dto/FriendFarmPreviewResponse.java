package com.anipals.backend.social.dto;

import com.anipals.backend.game.dto.FarmPlotResponse;
import com.anipals.backend.game.dto.PondStatusResponse;

import java.util.List;

public record FriendFarmPreviewResponse(
        String uid,
        String name,
        String farmName,
        String status,
        List<FarmPlotResponse> farmPlots,
        PondStatusResponse pond
) {
}
