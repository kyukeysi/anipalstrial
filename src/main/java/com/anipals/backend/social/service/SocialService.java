package com.anipals.backend.social.service;

import com.anipals.backend.game.dto.FarmPlotResponse;
import com.anipals.backend.game.dto.PondStatusResponse;
import com.anipals.backend.game.entity.PlayerGameState;
import com.anipals.backend.game.repository.FarmPlotRepository;
import com.anipals.backend.game.repository.PlayerGameStateRepository;
import com.anipals.backend.social.dto.FriendFarmPreviewResponse;
import com.anipals.backend.social.dto.FriendMessageRequest;
import com.anipals.backend.social.dto.FriendMessageResponse;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SocialService {

    private final PlayerGameStateRepository playerRepository;
    private final FarmPlotRepository farmPlotRepository;
    private final List<FriendMessageResponse> messages = new CopyOnWriteArrayList<>();

    public SocialService(PlayerGameStateRepository playerRepository, FarmPlotRepository farmPlotRepository) {
        this.playerRepository = playerRepository;
        this.farmPlotRepository = farmPlotRepository;
    }

    public FriendFarmPreviewResponse previewFarm(String uid) {
        PlayerGameState player = playerRepository.findByUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found."));
        List<FarmPlotResponse> plots = farmPlotRepository.findByPlayerKeyOrderByPlotIndexAsc(player.getPlayerKey()).stream()
                .map(plot -> new FarmPlotResponse(plot.getPlotIndex(), plot.getCrop(), plot.getState().name()))
                .toList();

        return new FriendFarmPreviewResponse(
                player.getUid(),
                player.getName(),
                player.getFarmName(),
                onlineStatus(player.getLastSeenAt()),
                plots,
                pondStatus(player)
        );
    }

    public List<FriendMessageResponse> messagesFor(String uid) {
        return messages.stream()
                .filter(message -> uid.equalsIgnoreCase(message.recipientUid()))
                .toList();
    }

    public FriendMessageResponse sendMessage(FriendMessageRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }

        FriendMessageResponse response = new FriendMessageResponse(
                UUID.randomUUID().toString(),
                request.senderKey() == null || request.senderKey().isBlank() ? "demo-player" : request.senderKey().trim(),
                request.recipientUid().trim().toUpperCase(),
                request.message().trim(),
                LocalDateTime.now()
        );
        messages.add(response);
        return response;
    }

    private String onlineStatus(LocalDateTime lastSeenAt) {
        if (lastSeenAt == null) {
            return "Offline";
        }
        long minutes = Duration.between(lastSeenAt, LocalDateTime.now()).toMinutes();
        if (minutes < 5) {
            return "Online";
        }
        return minutes < 60 ? "Away" : "Offline";
    }

    private PondStatusResponse pondStatus(PlayerGameState player) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime readyAt = player.getPondReadyAt() == null ? now : player.getPondReadyAt();
        long seconds = readyAt.isAfter(now) ? Duration.between(now, readyAt).toSeconds() + 1 : 0;
        return new PondStatusResponse(seconds == 0 ? "READY" : "RESTING", readyAt, seconds);
    }
}
