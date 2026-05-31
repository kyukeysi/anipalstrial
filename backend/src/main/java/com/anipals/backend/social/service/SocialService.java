package com.anipals.backend.social.service;

import com.anipals.backend.game.dto.FarmPlotResponse;
import com.anipals.backend.game.dto.PondStatusResponse;
import com.anipals.backend.game.entity.FarmPlotState;
import com.anipals.backend.game.entity.PlayerGameState;
import com.anipals.backend.game.repository.FarmPlotRepository;
import com.anipals.backend.game.repository.PlayerGameStateRepository;
import com.anipals.backend.social.dto.*;
import com.anipals.backend.social.entity.Friendship;
import com.anipals.backend.social.entity.FriendshipStatus;
import com.anipals.backend.social.entity.FriendMessage;
import com.anipals.backend.social.repository.FriendMessageRepository;
import com.anipals.backend.social.repository.FriendshipRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SocialService {

    private static final int MAX_FRIENDS = 100;

    private final PlayerGameStateRepository playerRepository;
    private final FarmPlotRepository farmPlotRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendMessageRepository friendMessageRepository;

    public SocialService(
            PlayerGameStateRepository playerRepository,
            FarmPlotRepository farmPlotRepository,
            FriendshipRepository friendshipRepository,
            FriendMessageRepository friendMessageRepository
    ) {
        this.playerRepository = playerRepository;
        this.farmPlotRepository = farmPlotRepository;
        this.friendshipRepository = friendshipRepository;
        this.friendMessageRepository = friendMessageRepository;
    }

    public FriendSummaryResponse summary(String playerKey) {
        PlayerGameState player = requirePlayer(playerKey);
        String normalizedPlayerKey = player.getPlayerKey();

        List<FriendPlayerResponse> friends = friendshipRepository
                .findForPlayerByStatus(normalizedPlayerKey, FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> playerResponse(otherPlayer(friendship, normalizedPlayerKey)))
                .toList();

        List<FriendRequestResponse> incoming = friendshipRepository
                .findByRecipientKeyAndStatusOrderByCreatedAtDesc(normalizedPlayerKey, FriendshipStatus.PENDING)
                .stream()
                .map(friendship -> requestResponse(friendship, friendship.getRequesterKey()))
                .toList();

        List<FriendRequestResponse> outgoing = friendshipRepository
                .findByRequesterKeyAndStatusOrderByCreatedAtDesc(normalizedPlayerKey, FriendshipStatus.PENDING)
                .stream()
                .map(friendship -> requestResponse(friendship, friendship.getRecipientKey()))
                .toList();

        List<FriendPlayerResponse> blocked = friendshipRepository
                .findByRequesterKeyAndStatusOrderByCreatedAtDesc(normalizedPlayerKey, FriendshipStatus.BLOCKED)
                .stream()
                .map(friendship -> playerResponse(requirePlayerByKey(friendship.getRecipientKey())))
                .toList();

        return new FriendSummaryResponse(MAX_FRIENDS, friends.size(), friends, incoming, outgoing, blocked);
    }

    public List<PlayerSearchResponse> searchPlayers(String playerKey, String query) {
        PlayerGameState player = requirePlayer(playerKey);
        String trimmed = query == null ? "" : query.trim();
        if (trimmed.length() < 2) {
            return List.of();
        }

        return playerRepository.searchByUidOrName(trimmed, PageRequest.of(0, 10)).stream()
                .filter(candidate -> !candidate.getPlayerKey().equals(player.getPlayerKey()))
                .map(candidate -> new PlayerSearchResponse(
                        candidate.getUid(),
                        candidate.getName(),
                        candidate.getFarmName(),
                        relationshipStatus(player.getPlayerKey(), candidate.getPlayerKey())
                ))
                .toList();
    }

    @Transactional
    public FriendRequestResponse sendFriendRequest(FriendActionRequest request) {
        PlayerGameState requester = requirePlayer(request.playerKey());
        PlayerGameState recipient = requirePlayerByUidOrName(request.targetUid());
        if (requester.getPlayerKey().equals(recipient.getPlayerKey())) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");
        }

        enforceFriendCapacity(requester.getPlayerKey());
        enforceFriendCapacity(recipient.getPlayerKey());

        Pair pair = pairFor(requester.getPlayerKey(), recipient.getPlayerKey());
        Friendship friendship = friendshipRepository.findByPlayerAKeyAndPlayerBKey(pair.playerAKey(), pair.playerBKey())
                .orElse(null);

        if (friendship != null) {
            if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                throw new IllegalArgumentException("Friend request cannot be sent.");
            }
            if (friendship.getStatus() == FriendshipStatus.ACCEPTED) {
                throw new IllegalArgumentException("You are already friends.");
            }
            if (friendship.getRequesterKey().equals(recipient.getPlayerKey())) {
                friendship.setStatus(FriendshipStatus.ACCEPTED);
                return requestResponse(friendshipRepository.save(friendship), recipient.getPlayerKey());
            }
            throw new IllegalArgumentException("Friend request is already pending.");
        }

        Friendship newFriendship = new Friendship();
        newFriendship.setPlayerAKey(pair.playerAKey());
        newFriendship.setPlayerBKey(pair.playerBKey());
        newFriendship.setRequesterKey(requester.getPlayerKey());
        newFriendship.setRecipientKey(recipient.getPlayerKey());
        newFriendship.setStatus(FriendshipStatus.PENDING);
        return requestResponse(friendshipRepository.save(newFriendship), recipient.getPlayerKey());
    }

    @Transactional
    public FriendPlayerResponse acceptRequest(Long requestId, String playerKey) {
        PlayerGameState player = requirePlayer(playerKey);
        Friendship friendship = requireFriendship(requestId);
        if (friendship.getStatus() != FriendshipStatus.PENDING || !friendship.getRecipientKey().equals(player.getPlayerKey())) {
            throw new IllegalArgumentException("Pending request not found.");
        }

        enforceFriendCapacity(friendship.getRequesterKey());
        enforceFriendCapacity(friendship.getRecipientKey());
        friendship.setStatus(FriendshipStatus.ACCEPTED);
        friendshipRepository.save(friendship);
        return playerResponse(requirePlayerByKey(friendship.getRequesterKey()));
    }

    @Transactional
    public void declineOrCancelRequest(Long requestId, String playerKey) {
        PlayerGameState player = requirePlayer(playerKey);
        Friendship friendship = requireFriendship(requestId);
        boolean involved = friendship.getRequesterKey().equals(player.getPlayerKey()) || friendship.getRecipientKey().equals(player.getPlayerKey());
        if (friendship.getStatus() != FriendshipStatus.PENDING || !involved) {
            throw new IllegalArgumentException("Pending request not found.");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public void removeFriend(String playerKey, String friendUid) {
        PlayerGameState player = requirePlayer(playerKey);
        PlayerGameState friend = requirePlayerByUid(friendUid);
        Pair pair = pairFor(player.getPlayerKey(), friend.getPlayerKey());
        Friendship friendship = friendshipRepository.findByPlayerAKeyAndPlayerBKey(pair.playerAKey(), pair.playerBKey())
                .orElseThrow(() -> new IllegalArgumentException("Friendship not found."));
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new IllegalArgumentException("Friendship not found.");
        }
        friendshipRepository.delete(friendship);
    }

    @Transactional
    public FriendPlayerResponse blockPlayer(String playerKey, String targetUid) {
        PlayerGameState blocker = requirePlayer(playerKey);
        PlayerGameState blocked = requirePlayerByUid(targetUid);
        if (blocker.getPlayerKey().equals(blocked.getPlayerKey())) {
            throw new IllegalArgumentException("You cannot block yourself.");
        }

        Pair pair = pairFor(blocker.getPlayerKey(), blocked.getPlayerKey());
        Friendship friendship = friendshipRepository.findByPlayerAKeyAndPlayerBKey(pair.playerAKey(), pair.playerBKey())
                .orElseGet(Friendship::new);
        friendship.setPlayerAKey(pair.playerAKey());
        friendship.setPlayerBKey(pair.playerBKey());
        friendship.setRequesterKey(blocker.getPlayerKey());
        friendship.setRecipientKey(blocked.getPlayerKey());
        friendship.setStatus(FriendshipStatus.BLOCKED);
        friendshipRepository.save(friendship);
        return playerResponse(blocked);
    }

    public FriendFarmPreviewResponse previewFarm(String uid) {
        PlayerGameState player = playerRepository.findByUidIgnoreCase(uid)
                .orElseThrow(() -> new IllegalArgumentException("Friend not found."));
        refreshFarmPlots(player.getPlayerKey());
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

    public List<FriendMessageResponse> messagesFor(String uid, String playerKey) {
        PlayerGameState currentPlayer = requirePlayer(playerKey);
        PlayerGameState friend = requirePlayerByUid(uid);
        String currentKey = currentPlayer.getPlayerKey();
        String friendKey = friend.getPlayerKey();

        return friendMessageRepository.findConversation(currentPlayer.getUid(), friend.getUid()).stream()
                .filter(message ->
                        (message.getSenderKey().equals(currentKey) && message.getRecipientKey().equals(friendKey))
                                || (message.getSenderKey().equals(friendKey) && message.getRecipientKey().equals(currentKey))
                )
                .map(this::messageResponse)
                .toList();
    }

    @Transactional
    public FriendMessageResponse sendMessage(FriendMessageRequest request) {
        if (request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Message cannot be blank.");
        }
        PlayerGameState sender = requirePlayer(request.senderKey());
        PlayerGameState recipient = requirePlayerByUid(request.recipientUid());
        if (sender.getPlayerKey().equals(recipient.getPlayerKey())) {
            throw new IllegalArgumentException("You cannot message yourself.");
        }

        FriendMessage message = new FriendMessage();
        message.setId(UUID.randomUUID().toString());
        message.setSenderKey(sender.getPlayerKey());
        message.setSenderUid(sender.getUid());
        message.setSenderName(sender.getName());
        message.setRecipientKey(recipient.getPlayerKey());
        message.setRecipientUid(recipient.getUid());
        message.setMessage(request.message().trim());
        message.setSentAt(LocalDateTime.now());
        return messageResponse(friendMessageRepository.save(message));
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

    private void enforceFriendCapacity(String playerKey) {
        long friends = friendshipRepository.countByPlayerAKeyAndStatusOrPlayerBKeyAndStatus(
                playerKey,
                FriendshipStatus.ACCEPTED,
                playerKey,
                FriendshipStatus.ACCEPTED
        );
        if (friends >= MAX_FRIENDS) {
            throw new IllegalArgumentException("Friend list is full.");
        }
    }

    private PlayerGameState requirePlayer(String playerKey) {
        String key = playerKey == null || playerKey.isBlank() ? "demo-player" : playerKey.trim();
        return playerRepository.findByPlayerKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Player not found."));
    }

    private PlayerGameState requirePlayerByKey(String playerKey) {
        return playerRepository.findByPlayerKey(playerKey)
                .orElseThrow(() -> new IllegalArgumentException("Player not found."));
    }

    private PlayerGameState requirePlayerByUid(String uid) {
        if (uid == null || uid.isBlank()) {
            throw new IllegalArgumentException("Player UID is required.");
        }
        return playerRepository.findByUidIgnoreCase(uid.trim())
                .orElseThrow(() -> new IllegalArgumentException("Player not found."));
    }

    private PlayerGameState requirePlayerByUidOrName(String uidOrName) {
        if (uidOrName == null || uidOrName.isBlank()) {
            throw new IllegalArgumentException("Player UID or username is required.");
        }
        String query = uidOrName.trim();
        return playerRepository.findByUidIgnoreCase(query)
                .or(() -> playerRepository.findFirstByNameIgnoreCase(query))
                .orElseThrow(() -> new IllegalArgumentException("Player not found."));
    }

    private void refreshFarmPlots(String key) {
        LocalDateTime now = LocalDateTime.now();
        farmPlotRepository.findByPlayerKeyOrderByPlotIndexAsc(key).stream()
                .filter(plot -> plot.getState() == FarmPlotState.PLANTED)
                .filter(plot -> plot.getReadyAt() == null || !plot.getReadyAt().isAfter(now))
                .forEach(plot -> {
                    plot.setState(FarmPlotState.READY);
                    farmPlotRepository.save(plot);
                });
    }

    private Friendship requireFriendship(Long id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Friend request not found."));
    }

    private FriendRequestResponse requestResponse(Friendship friendship, String otherPlayerKey) {
        return new FriendRequestResponse(friendship.getId(), playerResponse(requirePlayerByKey(otherPlayerKey)), friendship.getCreatedAt());
    }

    private FriendPlayerResponse playerResponse(PlayerGameState player) {
        return new FriendPlayerResponse(player.getUid(), player.getName(), player.getFarmName(), onlineStatus(player.getLastSeenAt()));
    }

    private PlayerGameState otherPlayer(Friendship friendship, String playerKey) {
        String otherKey = friendship.getPlayerAKey().equals(playerKey) ? friendship.getPlayerBKey() : friendship.getPlayerAKey();
        return requirePlayerByKey(otherKey);
    }

    private String relationshipStatus(String playerKey, String candidateKey) {
        Pair pair = pairFor(playerKey, candidateKey);
        return friendshipRepository.findByPlayerAKeyAndPlayerBKey(pair.playerAKey(), pair.playerBKey())
                .map(friendship -> {
                    if (friendship.getStatus() == FriendshipStatus.PENDING && friendship.getRequesterKey().equals(playerKey)) {
                        return "OUTGOING_PENDING";
                    }
                    if (friendship.getStatus() == FriendshipStatus.PENDING && friendship.getRecipientKey().equals(playerKey)) {
                        return "INCOMING_PENDING";
                    }
                    return friendship.getStatus().name();
                })
                .orElse("NONE");
    }

    private Pair pairFor(String firstPlayerKey, String secondPlayerKey) {
        if (firstPlayerKey.compareTo(secondPlayerKey) <= 0) {
            return new Pair(firstPlayerKey, secondPlayerKey);
        }
        return new Pair(secondPlayerKey, firstPlayerKey);
    }

    private record Pair(String playerAKey, String playerBKey) {
    }

    private FriendMessageResponse messageResponse(FriendMessage message) {
        return new FriendMessageResponse(
                message.getId(),
                message.getSenderKey(),
                message.getSenderUid(),
                message.getSenderName(),
                message.getRecipientKey(),
                message.getRecipientUid(),
                message.getMessage(),
                message.getSentAt()
        );
    }
}
