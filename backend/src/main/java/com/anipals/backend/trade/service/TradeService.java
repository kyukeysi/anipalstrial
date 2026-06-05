package com.anipals.backend.trade.service;

import com.anipals.backend.game.entity.InventoryItem;
import com.anipals.backend.game.entity.PlayerGameState;
import com.anipals.backend.game.repository.InventoryItemRepository;
import com.anipals.backend.game.repository.PlayerGameStateRepository;
import com.anipals.backend.social.entity.Friendship;
import com.anipals.backend.social.entity.FriendshipStatus;
import com.anipals.backend.social.repository.FriendshipRepository;
import com.anipals.backend.trade.dto.*;
import com.anipals.backend.trade.entity.TradeOffer;
import com.anipals.backend.trade.entity.TradeOfferItem;
import com.anipals.backend.trade.entity.TradeStatus;
import com.anipals.backend.trade.repository.TradeOfferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TradeService {

    private static final String DEFAULT_PLAYER_KEY = "demo-player";
    private static final int MAX_OPEN_OUTGOING_TRADES = 10;
    private static final int MAX_ITEMS_PER_TRADE = 8;

    private final PlayerGameStateRepository playerRepository;
    private final InventoryItemRepository inventoryRepository;
    private final FriendshipRepository friendshipRepository;
    private final TradeOfferRepository tradeOfferRepository;

    public TradeService(
            PlayerGameStateRepository playerRepository,
            InventoryItemRepository inventoryRepository,
            FriendshipRepository friendshipRepository,
            TradeOfferRepository tradeOfferRepository
    ) {
        this.playerRepository = playerRepository;
        this.inventoryRepository = inventoryRepository;
        this.friendshipRepository = friendshipRepository;
        this.tradeOfferRepository = tradeOfferRepository;
    }

    @Transactional(readOnly = true)
    public TradeSummaryResponse summary(String playerKey) {
        PlayerGameState player = requirePlayer(playerKey);
        List<TradePlayerResponse> friends = friendshipRepository
                .findForPlayerByStatus(player.getPlayerKey(), FriendshipStatus.ACCEPTED)
                .stream()
                .map(friendship -> playerResponse(requirePlayerByKey(otherPlayerKey(friendship, player.getPlayerKey()))))
                .toList();

        List<TradeOfferResponse> trades = tradeOfferRepository.findForPlayer(player.getPlayerKey()).stream()
                .map(trade -> response(trade, player.getPlayerKey(), null))
                .toList();

        return new TradeSummaryResponse(
                friends,
                trades.stream().filter(trade -> "OPEN".equals(trade.status()) && trade.recipient().uid().equals(player.getUid())).toList(),
                trades.stream().filter(trade -> "OPEN".equals(trade.status()) && trade.initiator().uid().equals(player.getUid())).toList(),
                trades.stream().filter(trade -> !"OPEN".equals(trade.status())).toList()
        );
    }

    @Transactional(readOnly = true)
    public TradePlayerResponse searchTarget(String playerKey, String uid) {
        PlayerGameState player = requirePlayer(playerKey);
        PlayerGameState target = requirePlayerByUid(uid);
        if (player.getPlayerKey().equals(target.getPlayerKey())) {
            throw new IllegalArgumentException("You cannot trade with yourself.");
        }
        return playerResponse(target);
    }

    @Transactional
    public TradeOfferResponse createOffer(TradeCreateRequest request) {
        PlayerGameState initiator = requirePlayer(request.playerKey());
        PlayerGameState recipient = requirePlayerByUid(request.targetUid());
        if (initiator.getPlayerKey().equals(recipient.getPlayerKey())) {
            throw new IllegalArgumentException("You cannot trade with yourself.");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Choose at least one item to offer.");
        }
        if (tradeOfferRepository.countByInitiatorKeyAndStatus(initiator.getPlayerKey(), TradeStatus.OPEN) >= MAX_OPEN_OUTGOING_TRADES) {
            throw new IllegalArgumentException("You have too many open trade offers.");
        }

        TradeOffer trade = new TradeOffer();
        trade.setInitiatorKey(initiator.getPlayerKey());
        trade.setRecipientKey(recipient.getPlayerKey());

        for (Map.Entry<String, Integer> itemRequest : requestedItemQuantities(request.items()).entrySet()) {
            InventoryItem inventoryItem = requireOfferedItem(initiator.getPlayerKey(), itemRequest.getKey(), itemRequest.getValue());
            TradeOfferItem item = new TradeOfferItem();
            item.setSourceInventoryItemId(inventoryItem.getId());
            item.setItemCode(inventoryItem.getItemCode());
            item.setName(inventoryItem.getName());
            item.setType(inventoryItem.getType());
            item.setRarity(inventoryItem.getRarity());
            item.setQuantity(itemRequest.getValue());
            item.setColor(inventoryItem.getColor());
            trade.addItem(item);
        }

        return response(tradeOfferRepository.save(trade), initiator.getPlayerKey(), "Trade offer sent. Waiting for review.");
    }

    @Transactional
    public TradeOfferResponse accept(String tradeId, String playerKey) {
        PlayerGameState player = requirePlayer(playerKey);
        TradeOffer trade = requireOpenTradeForUpdate(tradeId, player.getPlayerKey());

        if (trade.getInitiatorKey().equals(player.getPlayerKey())) {
            throw new IllegalArgumentException("Trade offer is waiting for the other player.");
        }

        trade.setRecipientAccepted(true);
        executeTrade(trade);
        trade.setStatus(TradeStatus.COMPLETED);
        trade.setCompletedAt(LocalDateTime.now());
        return response(tradeOfferRepository.save(trade), player.getPlayerKey(), "Trade completed. Items were transferred safely.");
    }

    @Transactional
    public TradeOfferResponse decline(String tradeId, String playerKey) {
        PlayerGameState player = requirePlayer(playerKey);
        TradeOffer trade = requireOpenTradeForUpdate(tradeId, player.getPlayerKey());
        trade.setStatus(trade.getInitiatorKey().equals(player.getPlayerKey()) ? TradeStatus.CANCELED : TradeStatus.DECLINED);
        return response(tradeOfferRepository.save(trade), player.getPlayerKey(), "Trade canceled.");
    }

    private void executeTrade(TradeOffer trade) {
        if (!trade.isInitiatorAccepted() || !trade.isRecipientAccepted()) {
            throw new IllegalArgumentException("Both players must accept before the trade can complete.");
        }

        for (TradeOfferItem offeredItem : trade.getItems()) {
            InventoryItem source = inventoryRepository
                    .findWithLockByPlayerKeyAndId(trade.getInitiatorKey(), offeredItem.getSourceInventoryItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Offered item is no longer available."));
            if (!source.getItemCode().equals(offeredItem.getItemCode()) || source.getQuantity() < offeredItem.getQuantity()) {
                throw new IllegalArgumentException("Offered item quantity changed. Trade canceled for safety.");
            }
        }

        for (TradeOfferItem offeredItem : trade.getItems()) {
            InventoryItem source = inventoryRepository
                    .findWithLockByPlayerKeyAndId(trade.getInitiatorKey(), offeredItem.getSourceInventoryItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Offered item is no longer available."));
            source.setQuantity(source.getQuantity() - offeredItem.getQuantity());
            if (source.getQuantity() <= 0) {
                inventoryRepository.delete(source);
            } else {
                inventoryRepository.save(source);
            }

            InventoryItem target = inventoryRepository
                    .findByPlayerKeyAndItemCode(trade.getRecipientKey(), offeredItem.getItemCode())
                    .orElseGet(InventoryItem::new);
            if (target.getId() == null) {
                target.setPlayerKey(trade.getRecipientKey());
                target.setItemCode(offeredItem.getItemCode());
                target.setName(offeredItem.getName());
                target.setType(offeredItem.getType());
                target.setRarity(offeredItem.getRarity());
                target.setColor(offeredItem.getColor());
                target.setQuantity(offeredItem.getQuantity());
            } else {
                target.setQuantity(target.getQuantity() + offeredItem.getQuantity());
            }
            inventoryRepository.save(target);
        }
    }

    private InventoryItem requireOfferedItem(String playerKey, String inventoryItemId, int quantity) {
        Long id = parseInventoryId(inventoryItemId);
        InventoryItem item = inventoryRepository.findByPlayerKeyAndId(playerKey, id)
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found."));
        if (item.getQuantity() < quantity) {
            throw new IllegalArgumentException("Not enough quantity for " + item.getName() + ".");
        }
        return item;
    }

    private Map<String, Integer> requestedItemQuantities(List<TradeItemRequest> items) {
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (TradeItemRequest item : items) {
            if (item.inventoryItemId() == null || item.inventoryItemId().isBlank()) {
                throw new IllegalArgumentException("Inventory item not found.");
            }
            int quantity = item.quantity() <= 0 ? 1 : item.quantity();
            quantities.merge(item.inventoryItemId().trim(), quantity, Integer::sum);
        }
        if (quantities.size() > MAX_ITEMS_PER_TRADE) {
            throw new IllegalArgumentException("A trade can contain at most " + MAX_ITEMS_PER_TRADE + " item stacks.");
        }
        return quantities;
    }

    private TradeOffer requireOpenTradeForUpdate(String tradeId, String playerKey) {
        TradeOffer trade = tradeOfferRepository.findByIdForUpdate(tradeId)
                .orElseThrow(() -> new IllegalArgumentException("Trade not found."));
        boolean involved = trade.getInitiatorKey().equals(playerKey) || trade.getRecipientKey().equals(playerKey);
        if (!involved) {
            throw new IllegalArgumentException("Trade not found.");
        }
        if (trade.getStatus() != TradeStatus.OPEN) {
            throw new IllegalArgumentException("Trade is already closed.");
        }
        return trade;
    }

    private Long parseInventoryId(String inventoryItemId) {
        try {
            return Long.parseLong(inventoryItemId);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid inventory item id.");
        }
    }

    private PlayerGameState requirePlayer(String playerKey) {
        String key = playerKey == null || playerKey.isBlank() ? DEFAULT_PLAYER_KEY : playerKey.trim();
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
                .orElseThrow(() -> new IllegalArgumentException("Player UID not found."));
    }

    private TradeOfferResponse response(TradeOffer trade, String viewerKey, String message) {
        PlayerGameState initiator = requirePlayerByKey(trade.getInitiatorKey());
        PlayerGameState recipient = requirePlayerByKey(trade.getRecipientKey());
        return new TradeOfferResponse(
                trade.getId(),
                trade.getStatus().name(),
                playerResponse(initiator),
                playerResponse(recipient),
                trade.isInitiatorAccepted(),
                trade.isRecipientAccepted(),
                trade.getItems().stream()
                        .sorted(Comparator.comparing(TradeOfferItem::getId))
                        .map(item -> new TradeItemResponse(
                                item.getSourceInventoryItemId().toString(),
                                item.getName(),
                                item.getType(),
                                item.getRarity(),
                                item.getQuantity(),
                                item.getColor()
                        ))
                        .toList(),
                trade.getCreatedAt(),
                trade.getUpdatedAt(),
                messageFor(trade, viewerKey, message)
        );
    }

    private String messageFor(TradeOffer trade, String viewerKey, String explicit) {
        if (explicit != null) {
            return explicit;
        }
        if (trade.getStatus() == TradeStatus.COMPLETED) {
            return "Trade completed.";
        }
        if (trade.getStatus() == TradeStatus.DECLINED || trade.getStatus() == TradeStatus.CANCELED) {
            return "Trade canceled.";
        }
        return trade.getRecipientKey().equals(viewerKey) ? "Review the offer, then accept or decline." : "Waiting for the other player to review.";
    }

    private TradePlayerResponse playerResponse(PlayerGameState player) {
        return new TradePlayerResponse(player.getUid(), player.getName(), player.getFarmName(), onlineStatus(player.getLastSeenAt()));
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

    private String otherPlayerKey(Friendship friendship, String playerKey) {
        return friendship.getPlayerAKey().equals(playerKey) ? friendship.getPlayerBKey() : friendship.getPlayerAKey();
    }
}
