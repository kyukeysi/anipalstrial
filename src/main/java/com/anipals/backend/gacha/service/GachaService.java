package com.anipals.backend.gacha.service;

import com.anipals.backend.gacha.dto.GachaHistoryResponse;
import com.anipals.backend.gacha.dto.GachaPullRequest;
import com.anipals.backend.gacha.dto.GachaStatusResponse;
import com.anipals.backend.gacha.dto.GemBundleResponse;
import com.anipals.backend.gacha.dto.GemPurchaseRequest;
import com.anipals.backend.gacha.entity.GachaPullHistory;
import com.anipals.backend.gacha.repository.GachaPullHistoryRepository;
import com.anipals.backend.game.dto.CurrencyResponse;
import com.anipals.backend.game.entity.InventoryItem;
import com.anipals.backend.game.entity.PlayerAniPal;
import com.anipals.backend.game.entity.PlayerGameState;
import com.anipals.backend.game.entity.TutorialState;
import com.anipals.backend.game.repository.InventoryItemRepository;
import com.anipals.backend.game.repository.PlayerAniPalRepository;
import com.anipals.backend.game.repository.PlayerGameStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class GachaService {

    private static final String DEFAULT_PLAYER_KEY = "demo-player";
    private static final int SINGLE_COST = 160;
    private static final int TEN_PULL_COST = 1600;
    private static final int SSR_HARD_PITY = 100;
    private static final int SR_GUARANTEE = 10;

    private final PlayerGameStateRepository playerRepository;
    private final PlayerAniPalRepository aniPalRepository;
    private final InventoryItemRepository inventoryRepository;
    private final GachaPullHistoryRepository historyRepository;
    private final Random random = new Random();
    private final List<GemBundleResponse> gemBundles = List.of(
            new GemBundleResponse("small", 160, 1600, "Starter pouch"),
            new GemBundleResponse("medium", 800, 7600, "Barn bundle"),
            new GemBundleResponse("large", 1600, 14400, "Harvest chest")
    );

    public GachaService(
            PlayerGameStateRepository playerRepository,
            PlayerAniPalRepository aniPalRepository,
            InventoryItemRepository inventoryRepository,
            GachaPullHistoryRepository historyRepository
    ) {
        this.playerRepository = playerRepository;
        this.aniPalRepository = aniPalRepository;
        this.inventoryRepository = inventoryRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional
    public GachaStatusResponse getStatus(String playerKey) {
        PlayerGameState player = getOrCreatePlayer(normalizePlayerKey(playerKey));
        return buildStatus(player, "Choose Pull 1 or Pull 10.");
    }

    @Transactional
    public GachaStatusResponse pull(GachaPullRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        int count = request.count() == 10 ? 10 : 1;
        int cost = count == 10 ? TEN_PULL_COST : SINGLE_COST;
        PlayerGameState player = getOrCreatePlayer(key);

        if (player.getGems() < cost) {
            return buildStatus(player, "Not enough gems. Pull 1 costs 160 gems and Pull 10 costs 1,600 gems.");
        }

        player.setGems(player.getGems() - cost);

        GachaPullHistory best = null;
        for (int index = 0; index < count; index++) {
            boolean forceSrPlus = count == 10 && index == 9 && player.getSrPity() >= SR_GUARANTEE - 1;
            GachaPullHistory result = rollOne(player, forceSrPlus);
            historyRepository.save(result);
            best = betterResult(best, result);
        }

        if (player.getTutorialState() == TutorialState.GACHA_PULL) {
            player.setTutorialState(TutorialState.FARMING);
        }

        String summary = "Pulled " + count + ". Best result: " + best.getResult() + " (" + best.getRarity() + ").";
        return buildStatus(player, summary);
    }

    @Transactional
    public GachaStatusResponse buyGems(GemPurchaseRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        PlayerGameState player = getOrCreatePlayer(key);
        GemBundleResponse bundle = gemBundles.stream()
                .filter(item -> item.id().equals(request.bundleId()))
                .findFirst()
                .orElse(gemBundles.get(0));

        if (player.getCoins() < bundle.coins()) {
            return buildStatus(player, "Not enough coins. " + bundle.label() + " costs " + bundle.coins() + " coins.");
        }

        player.setCoins(player.getCoins() - bundle.coins());
        player.setGems(player.getGems() + bundle.gems());
        return buildStatus(player, "Bought " + bundle.gems() + " gems for " + bundle.coins() + " coins.");
    }

    private GachaPullHistory rollOne(PlayerGameState player, boolean forceSrPlus) {
        String rarity = chooseRarity(player, forceSrPlus);
        boolean featured = false;
        String result;

        if ("SSR".equals(rarity)) {
            boolean guaranteed = player.isGuaranteedFeatured();
            featured = guaranteed || random.nextBoolean();
            player.setGuaranteedFeatured(!featured);
            player.setSsrPity(0);
            player.setSrPity(0);
            result = featured ? "Sol Harvest Helper" : pickNormalSsr();
            addAniPal(player.getPlayerKey(), result, "SSR", featured);
        } else if ("SR".equals(rarity)) {
            player.setSsrPity(player.getSsrPity() + 1);
            player.setSrPity(0);
            result = pickSr();
            addAniPal(player.getPlayerKey(), result, "SR", false);
        } else {
            player.setSsrPity(player.getSsrPity() + 1);
            player.setSrPity(player.getSrPity() + 1);
            result = pickR();
            grantRReward(player.getPlayerKey(), result);
        }

        GachaPullHistory history = new GachaPullHistory();
        history.setPlayerKey(player.getPlayerKey());
        history.setResult(result);
        history.setRarity(rarity);
        history.setFeatured(featured);
        return history;
    }

    private String chooseRarity(PlayerGameState player, boolean forceSrPlus) {
        if (player.getSsrPity() >= SSR_HARD_PITY - 1) {
            return "SSR";
        }

        int roll = random.nextInt(100);
        if (forceSrPlus) {
            return roll < 10 ? "SSR" : "SR";
        }

        if (roll < 1) {
            return "SSR";
        }

        if (roll < 10) {
            return "SR";
        }

        return "R";
    }

    private void addAniPal(String key, String name, String rarity, boolean featured) {
        boolean duplicate = aniPalRepository.findByPlayerKeyOrderByIdAsc(key).stream()
                .anyMatch(aniPal -> aniPal.getName().equals(name));

        if (duplicate) {
            int shards = switch (rarity) {
                case "SSR" -> 100;
                case "SR" -> 20;
                default -> 5;
            };
            addShardItem(key, "ani-shards", "AniShards", shards);
            return;
        }

        PlayerAniPal aniPal = new PlayerAniPal();
        aniPal.setPlayerKey(key);
        aniPal.setName(name);
        aniPal.setSpecies(featured ? "Phoenix" : "Companion");
        aniPal.setRole(featured ? "Harvest Leader" : "Farm Helper");
        aniPal.setMood("New");
        aniPal.setLevel(1);
        aniPal.setPalette(featured ? "bg-yellow-300" : "bg-cyan-300");
        aniPalRepository.save(aniPal);
    }

    private void addShardItem(String key, String itemCode, String name, int quantity) {
        Optional<InventoryItem> existing = inventoryRepository.findByPlayerKeyAndItemCode(key, itemCode);
        InventoryItem item = existing.orElseGet(InventoryItem::new);
        if (item.getId() == null) {
            item.setPlayerKey(key);
            item.setItemCode(itemCode);
            item.setName(name);
            item.setType("Material");
            item.setRarity("Rare");
            item.setColor("bg-violet-300");
            item.setQuantity(quantity);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        inventoryRepository.save(item);
    }

    private void addInventoryItem(String key, String itemCode, String name, String type, String rarity, int quantity, String color) {
        Optional<InventoryItem> existing = inventoryRepository.findByPlayerKeyAndItemCode(key, itemCode);
        InventoryItem item = existing.orElseGet(InventoryItem::new);
        if (item.getId() == null) {
            item.setPlayerKey(key);
            item.setItemCode(itemCode);
            item.setName(name);
            item.setType(type);
            item.setRarity(rarity);
            item.setColor(color);
            item.setQuantity(quantity);
        } else {
            item.setQuantity(item.getQuantity() + quantity);
        }
        inventoryRepository.save(item);
    }

    private void grantRReward(String key, String result) {
        switch (result) {
            case "Carrot Seeds x12" -> addInventoryItem(key, "carrot-seeds", "Carrot Seeds", "Seed", "Common", 12, "bg-orange-300");
            case "Wheat Seeds x10" -> addInventoryItem(key, "wheat-seeds", "Wheat Seeds", "Seed", "Common", 10, "bg-amber-300");
            case "Rice Grain Seeds x16" -> addInventoryItem(key, "rice-grain-seeds", "Rice Grain Seeds", "Seed", "Common", 16, "bg-lime-300");
            case "Cloud Cotton Seeds x6" -> addInventoryItem(key, "cloud-cotton-seeds", "Cloud Cotton Seeds", "Seed", "Uncommon", 6, "bg-slate-200");
            case "Berry Jam x3" -> addInventoryItem(key, "berry-jam", "Berry Jam", "Treat", "Uncommon", 3, "bg-rose-400");
            case "Honey Biscuit x2" -> addInventoryItem(key, "honey-biscuit", "Honey Biscuit", "Treat", "Rare", 2, "bg-amber-300");
            case "Clover Cookie x2" -> addInventoryItem(key, "clover-cookie", "Clover Cookie", "Treat", "Rare", 2, "bg-emerald-300");
            case "Compost Mix x4" -> addInventoryItem(key, "compost-mix", "Compost Mix", "Tool", "Common", 4, "bg-stone-400");
            default -> addShardItem(key, "ani-shards", "AniShards", 5);
        }
    }

    private PlayerGameState getOrCreatePlayer(String key) {
        Optional<PlayerGameState> existing = playerRepository.findByPlayerKey(key);
        if (existing.isPresent()) {
            PlayerGameState player = existing.get();
            if (player.getGems() == 0 && historyRepository.findTop20ByPlayerKeyOrderByPulledAtDesc(key).isEmpty()) {
                player.setGems(3200);
            }
            return player;
        }

        return playerRepository.findByPlayerKey(key).orElseGet(() -> {
            PlayerGameState player = new PlayerGameState();
            player.setPlayerKey(key);
            player.setName("Mira Sprout");
            player.setFarmName("Sunberry Acres");
            player.setLevel(18);
            player.setCoins(12840);
            player.setGems(3200);
            player.setEnergy(76);
            player.setSprouts(42);
            player.setTickets(9);
            player.setTutorialState(TutorialState.INTRO);
            return playerRepository.save(player);
        });
    }

    private GachaStatusResponse buildStatus(PlayerGameState player, String status) {
        CurrencyResponse currencies = new CurrencyResponse(
                player.getCoins(),
                player.getGems(),
                player.getEnergy(),
                player.getSprouts(),
                player.getTickets()
        );

        return new GachaStatusResponse(
                player.getSsrPity(),
                player.getSrPity(),
                player.isGuaranteedFeatured(),
                SINGLE_COST,
                TEN_PULL_COST,
                currencies,
                historyRepository.findTop20ByPlayerKeyOrderByPulledAtDesc(player.getPlayerKey()).stream()
                        .map(this::toHistoryResponse)
                        .toList(),
                gemBundles,
                status
        );
    }

    private GachaHistoryResponse toHistoryResponse(GachaPullHistory history) {
        return new GachaHistoryResponse(
                history.getId().toString(),
                history.getResult(),
                history.getRarity(),
                history.isFeatured(),
                history.getPulledAt().format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
        );
    }

    private GachaPullHistory betterResult(GachaPullHistory current, GachaPullHistory candidate) {
        if (current == null) {
            return candidate;
        }
        return rarityRank(candidate.getRarity()) > rarityRank(current.getRarity()) ? candidate : current;
    }

    private int rarityRank(String rarity) {
        return switch (rarity) {
            case "SSR" -> 3;
            case "SR" -> 2;
            default -> 1;
        };
    }

    private String pickNormalSsr() {
        String[] pool = {"Luna Orchard Keeper", "Aster Pond Guardian", "Nova Barn Sentinel"};
        return pool[random.nextInt(pool.length)];
    }

    private String pickSr() {
        String[] pool = {"Mochi Cat Helper", "Pebble Turtle Helper", "Honey Biscuit Specialist"};
        return pool[random.nextInt(pool.length)];
    }

    private String pickR() {
        String[] pool = {"Carrot Seeds x12", "Wheat Seeds x10", "Rice Grain Seeds x16", "Cloud Cotton Seeds x6", "Berry Jam x3", "Honey Biscuit x2", "Clover Cookie x2", "Compost Mix x4", "AniShard Bundle"};
        return pool[random.nextInt(pool.length)];
    }

    private String normalizePlayerKey(String playerKey) {
        if (playerKey == null || playerKey.isBlank()) {
            return DEFAULT_PLAYER_KEY;
        }
        return playerKey.trim();
    }
}
