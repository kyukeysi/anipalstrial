package com.anipals.backend.game.service;

import com.anipals.backend.game.dto.*;
import com.anipals.backend.game.entity.*;
import com.anipals.backend.game.repository.FarmPlotRepository;
import com.anipals.backend.game.repository.InventoryItemRepository;
import com.anipals.backend.game.repository.PlayerAniPalRepository;
import com.anipals.backend.game.repository.PlayerGameStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class GameService {

    private static final String DEFAULT_PLAYER_KEY = "demo-player";
    private static final long POND_COOLDOWN_SECONDS = 90;

    private final PlayerGameStateRepository playerRepository;
    private final InventoryItemRepository inventoryRepository;
    private final PlayerAniPalRepository aniPalRepository;
    private final FarmPlotRepository farmPlotRepository;

    public GameService(
            PlayerGameStateRepository playerRepository,
            InventoryItemRepository inventoryRepository,
            PlayerAniPalRepository aniPalRepository,
            FarmPlotRepository farmPlotRepository
    ) {
        this.playerRepository = playerRepository;
        this.inventoryRepository = inventoryRepository;
        this.aniPalRepository = aniPalRepository;
        this.farmPlotRepository = farmPlotRepository;
    }

    @Transactional
    public GameStateResponse getGameState(String playerKey) {
        String key = normalizePlayerKey(playerKey);
        ensurePlayerSeeded(key);
        refreshFarmPlots(key);
        PlayerGameState player = touchPlayer(key);
        return buildState(player, "Farm state loaded.");
    }

    @Transactional
    public PlayerGameState createFreshPlayer(String playerKey) {
        String key = normalizePlayerKey(playerKey);
        playerRepository.findByPlayerKey(key).ifPresent(existing -> {
            throw new IllegalArgumentException("Player state already exists.");
        });
        ensurePlayerSeeded(key);
        return touchPlayer(key);
    }

    @Transactional
    public PlayerGameState ensurePlayerExists(String playerKey) {
        String key = normalizePlayerKey(playerKey);
        ensurePlayerSeeded(key);
        return touchPlayer(key);
    }

    @Transactional
    public GameStateResponse updatePlayerName(PlayerNameRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        ensurePlayerSeeded(key);
        PlayerGameState player = touchPlayer(key);

        if (request.name() != null && !request.name().isBlank()) {
            player.setName(request.name().trim());
        }

        if (request.farmName() != null && !request.farmName().isBlank()) {
            player.setFarmName(request.farmName().trim());
        }

        return buildState(player, "Player profile updated.");
    }

    @Transactional
    public GameStateResponse harvest(HarvestRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        ensurePlayerSeeded(key);
        PlayerGameState player = touchPlayer(key);
        FarmPlot plot = farmPlotRepository.findByPlayerKeyAndPlotIndex(key, request.plotIndex())
                .orElseThrow(() -> new IllegalArgumentException("Farm plot not found."));

        if (plot.getState() == FarmPlotState.CLEARED) {
            return buildState(player, "That plot has already been harvested.");
        }

        if (plot.getState() == FarmPlotState.PLANTED && plot.getReadyAt().isAfter(LocalDateTime.now())) {
            return buildState(player, "That crop is still growing.");
        }

        int reward = switch (plot.getCrop()) {
            case "rice-grain" -> 55;
            case "wheat" -> 120;
            case "carrots" -> 90;
            case "moon-turnip" -> 170;
            case "star-melon" -> 250;
            case "cloud-cotton" -> 140;
            default -> 60;
        };

        plot.setState(FarmPlotState.CLEARED);
        player.setCoins(player.getCoins() + reward);
        player.setSprouts(player.getSprouts() + 1);
        addOrIncreaseInventory(key, plot.getCrop(), displayCropName(plot.getCrop()), "Crop", "Common", 1, cropColor(plot.getCrop()));

        return buildState(player, "Harvested " + plot.getCrop() + " for " + reward + " coins.");
    }

    @Transactional
    public GameStateResponse plant(PlantRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        ensurePlayerSeeded(key);
        refreshFarmPlots(key);
        PlayerGameState player = touchPlayer(key);
        FarmPlot plot = farmPlotRepository.findByPlayerKeyAndPlotIndex(key, request.plotIndex())
                .orElseThrow(() -> new IllegalArgumentException("Farm plot not found."));

        if (plot.getState() != FarmPlotState.CLEARED) {
            return buildState(player, "Only cleared plots can be planted.");
        }

        InventoryItem seed = findSeedForPlanting(key, request.inventoryItemId());
        if (seed == null) {
            return buildState(player, "No seeds available. Get seeds from inventory or gacha.");
        }

        String crop = cropFromSeed(seed);
        seed.setQuantity(seed.getQuantity() - 1);
        if (seed.getQuantity() <= 0) {
            inventoryRepository.delete(seed);
        } else {
            inventoryRepository.save(seed);
        }

        LocalDateTime now = LocalDateTime.now();
        plot.setCrop(crop);
        plot.setState(FarmPlotState.PLANTED);
        plot.setPlantedAt(now);
        plot.setReadyAt(now.plusSeconds(30));

        return buildState(player, "Planted " + seed.getName() + ". Crop will be ready soon.");
    }

    @Transactional
    public GameStateResponse useInventoryItem(InventoryUseRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        ensurePlayerSeeded(key);
        refreshFarmPlots(key);
        PlayerGameState player = touchPlayer(key);
        InventoryItem item = inventoryRepository.findByPlayerKeyAndId(key, Long.parseLong(request.inventoryItemId()))
                .orElseThrow(() -> new IllegalArgumentException("Inventory item not found."));

        if ("Seed".equals(item.getType())) {
            FarmPlot clearedPlot = farmPlotRepository.findByPlayerKeyOrderByPlotIndexAsc(key).stream()
                    .filter(plot -> plot.getState() == FarmPlotState.CLEARED)
                    .findFirst()
                    .orElse(null);

            if (clearedPlot == null) {
                return buildState(player, "No cleared plot is available for planting.");
            }

            return plant(new PlantRequest(key, clearedPlot.getPlotIndex(), item.getId().toString()));
        }

        if ("Treat".equals(item.getType())) {
            consumeItem(item, 1);
            player.setEnergy(Math.min(player.getEnergy() + 8, 100));
            return buildState(player, "Used " + item.getName() + ": +8 energy.");
        }

        if ("Crop".equals(item.getType())) {
            consumeItem(item, 1);
            player.setCoins(player.getCoins() + 35);
            return buildState(player, "Sold " + item.getName() + " for 35 coins.");
        }

        return buildState(player, item.getName() + " is saved for upgrades or trading.");
    }

    @Transactional
    public GameStateResponse collectPond(String playerKey) {
        String key = normalizePlayerKey(playerKey);
        ensurePlayerSeeded(key);
        PlayerGameState player = touchPlayer(key);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime readyAt = player.getPondReadyAt();
        if (readyAt != null && readyAt.isAfter(now)) {
            long seconds = java.time.Duration.between(now, readyAt).toSeconds() + 1;
            return buildState(player, "Pond is resting. Fish return in " + seconds + " seconds.");
        }

        player.setCoins(player.getCoins() + 180);
        player.setEnergy(Math.min(player.getEnergy() + 4, 100));
        player.setPondReadyAt(now.plusSeconds(POND_COOLDOWN_SECONDS));
        return buildState(player, "Collected pond fish: +180 coins and +4 energy.");
    }

    @Transactional
    public GameStateResponse giveTreat(String playerKey, Long aniPalId, String inventoryItemId) {
        String key = normalizePlayerKey(playerKey);
        ensurePlayerSeeded(key);
        PlayerGameState player = touchPlayer(key);
        PlayerAniPal aniPal = aniPalRepository.findByPlayerKeyAndId(key, aniPalId)
                .orElseThrow(() -> new IllegalArgumentException("AniPal not found."));
        InventoryItem treat = findTreatForFeeding(key, inventoryItemId);

        if (treat == null) {
            return buildState(player, "No animal treats are available.");
        }

        TreatEffect effect = treatEffect(treat.getName());
        consumeItem(treat, 1);
        aniPal.setMood(effect.mood());
        aniPal.setActiveBoost(effect.boost());
        player.setEnergy(Math.min(player.getEnergy() + effect.energy(), 100));
        return buildState(player, aniPal.getName() + " enjoyed " + treat.getName() + ": " + effect.boost() + ".");
    }

    @Transactional
    public GameStateResponse advanceTutorial(TutorialRequest request) {
        String key = normalizePlayerKey(request.playerKey());
        ensurePlayerSeeded(key);
        PlayerGameState player = touchPlayer(key);
        TutorialState state = TutorialState.valueOf(request.state().toUpperCase());

        player.setTutorialState(state);
        player.setTutorialCompleted(state == TutorialState.COMPLETE);

        return buildState(player, "Tutorial moved to " + state + ".");
    }

    private PlayerGameState touchPlayer(String key) {
        PlayerGameState player = playerRepository.findByPlayerKey(key)
                .orElseThrow(() -> new IllegalStateException("Player state was not initialized."));
        player.setLastSeenAt(LocalDateTime.now());
        return player;
    }

    private void ensurePlayerSeeded(String key) {
        if (playerRepository.findByPlayerKey(key).isPresent()) {
            return;
        }

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
        player.setPondReadyAt(LocalDateTime.now());
        player.setTutorialState(TutorialState.INTRO);
        player.setTutorialCompleted(false);
        playerRepository.save(player);

        seedInventory(key);
        seedAniPals(key);
        seedFarmPlots(key);
    }

    private void seedInventory(String key) {
        addOrIncreaseInventory(key, "carrot-seeds", "Carrot Seeds", "Seed", "Common", 24, "bg-orange-300");
        addOrIncreaseInventory(key, "wheat-seeds", "Wheat Seeds", "Seed", "Common", 22, "bg-amber-300");
        addOrIncreaseInventory(key, "rice-grain-seeds", "Rice Grain Seeds", "Seed", "Common", 38, "bg-lime-300");
        addOrIncreaseInventory(key, "moon-turnip-seeds", "Moon Turnip Seeds", "Seed", "Rare", 8, "bg-violet-300");
        addOrIncreaseInventory(key, "star-melon-seeds", "Star Melon Seeds", "Seed", "Epic", 4, "bg-yellow-300");
        addOrIncreaseInventory(key, "cloud-cotton-seeds", "Cloud Cotton Seeds", "Seed", "Uncommon", 12, "bg-slate-200");
        addOrIncreaseInventory(key, "copper-hoe", "Copper Hoe", "Tool", "Uncommon", 1, "bg-amber-500");
        addOrIncreaseInventory(key, "berry-jam", "Berry Jam", "Treat", "Uncommon", 11, "bg-rose-400");
        addOrIncreaseInventory(key, "honey-biscuit", "Honey Biscuit", "Treat", "Rare", 5, "bg-amber-300");
        addOrIncreaseInventory(key, "clover-cookie", "Clover Cookie", "Treat", "Rare", 4, "bg-emerald-300");
        addOrIncreaseInventory(key, "moon-milk", "Moon Milk", "Treat", "Epic", 2, "bg-sky-200");
        addOrIncreaseInventory(key, "pond-lantern", "Pond Lantern", "Decor", "Epic", 2, "bg-cyan-300");
    }

    private void seedAniPals(String key) {
        saveAniPal(key, "Pip", "Bunny", "Planter", "Cheerful", 12, "bg-pink-300");
        saveAniPal(key, "Mochi", "Cat", "Forager", "Curious", 9, "bg-yellow-300");
        saveAniPal(key, "Pebble", "Turtle", "Watering", "Calm", 15, "bg-emerald-300");
    }

    private void seedFarmPlots(String key) {
        String[] crops = {"rice-grain", "carrots", "wheat", "moon-turnip", "star-melon", "cloud-cotton", "rice-grain", "wheat", "carrots", "moon-turnip", "cloud-cotton", "star-melon"};
        for (int i = 0; i < crops.length; i++) {
            FarmPlot plot = new FarmPlot();
            plot.setPlayerKey(key);
            plot.setPlotIndex(i);
            plot.setCrop(crops[i]);
            plot.setState(FarmPlotState.READY);
            plot.setPlantedAt(LocalDateTime.now().minusHours(2));
            plot.setReadyAt(LocalDateTime.now().minusMinutes(20));
            farmPlotRepository.save(plot);
        }
    }

    private void saveAniPal(String key, String name, String species, String role, String mood, int level, String palette) {
        PlayerAniPal aniPal = new PlayerAniPal();
        aniPal.setPlayerKey(key);
        aniPal.setName(name);
        aniPal.setSpecies(species);
        aniPal.setRole(role);
        aniPal.setMood(mood);
        aniPal.setLevel(level);
        aniPal.setPalette(palette);
        aniPalRepository.save(aniPal);
    }

    private void addOrIncreaseInventory(String key, String itemCode, String name, String type, String rarity, int quantity, String color) {
        InventoryItem item = inventoryRepository.findByPlayerKeyAndItemCode(key, itemCode)
                .orElseGet(InventoryItem::new);

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

    private GameStateResponse buildState(PlayerGameState player, String status) {
        String key = player.getPlayerKey();
        refreshFarmPlots(key);
        return new GameStateResponse(
                new PlayerResponse(
                        player.getName(),
                        player.getUid(),
                        player.getLevel(),
                        player.getFarmName(),
                        player.getTutorialState().name(),
                        player.isTutorialCompleted()
                ),
                new CurrencyResponse(player.getCoins(), player.getGems(), player.getEnergy(), player.getSprouts(), player.getTickets()),
                new WeatherResponse("Sun Shower", "Crops grow 15% faster and pond fish appear more often today.", "24 C"),
                aniPalRepository.findByPlayerKeyOrderByIdAsc(key).stream()
                        .map(item -> new AniPalResponse(item.getId().toString(), item.getName(), item.getSpecies(), item.getRole(), item.getMood(), item.getLevel(), item.getPalette(), item.getActiveBoost()))
                        .toList(),
                inventoryRepository.findByPlayerKeyOrderByIdAsc(key).stream()
                        .map(item -> new InventoryItemResponse(item.getId().toString(), item.getName(), item.getType(), item.getRarity(), item.getQuantity(), item.getColor()))
                        .toList(),
                List.of(
                        new QuestResponse("q1", "Harvest 20 crops", "800 coins", harvestedCount(key) + " / 20"),
                        new QuestResponse("q2", "Gift treats to AniPals", "3 gacha tickets", delightedCount(key) + " / 3"),
                        new QuestResponse("q3", "Reach 50 sprouts", "Friendship crate", player.getSprouts() + " / 50")
                ),
                farmPlotRepository.findByPlayerKeyOrderByPlotIndexAsc(key).stream()
                        .map(plot -> new FarmPlotResponse(plot.getPlotIndex(), plot.getCrop(), plot.getState().name()))
                        .toList(),
                pondStatus(player),
                status
        );
    }

    private PondStatusResponse pondStatus(PlayerGameState player) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime readyAt = player.getPondReadyAt() == null ? now : player.getPondReadyAt();
        long seconds = readyAt.isAfter(now) ? java.time.Duration.between(now, readyAt).toSeconds() + 1 : 0;
        return new PondStatusResponse(seconds == 0 ? "READY" : "RESTING", readyAt, seconds);
    }

    private long harvestedCount(String key) {
        return farmPlotRepository.findByPlayerKeyOrderByPlotIndexAsc(key).stream()
                .filter(plot -> plot.getState() == FarmPlotState.CLEARED)
                .count();
    }

    private long delightedCount(String key) {
        return aniPalRepository.findByPlayerKeyOrderByIdAsc(key).stream()
                .filter(aniPal -> "Delighted".equals(aniPal.getMood()))
                .count();
    }

    private String normalizePlayerKey(String playerKey) {
        if (playerKey == null || playerKey.isBlank()) {
            return DEFAULT_PLAYER_KEY;
        }
        return playerKey.trim();
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

    private InventoryItem findSeedForPlanting(String key, String inventoryItemId) {
        if (inventoryItemId != null && !inventoryItemId.isBlank()) {
            return inventoryRepository.findByPlayerKeyAndId(key, Long.parseLong(inventoryItemId))
                    .filter(item -> "Seed".equals(item.getType()))
                    .filter(item -> item.getQuantity() > 0)
                    .orElse(null);
        }

        return inventoryRepository.findByPlayerKeyOrderByIdAsc(key).stream()
                .filter(item -> "Seed".equals(item.getType()))
                .filter(item -> item.getQuantity() > 0)
                .findFirst()
                .orElse(null);
    }

    private String cropFromSeed(InventoryItem seed) {
        String code = seed.getItemCode();
        if (code.contains("carrot")) {
            return "carrots";
        }
        if (code.contains("wheat")) {
            return "wheat";
        }
        if (code.contains("rice")) {
            return "rice-grain";
        }
        if (code.contains("moon-turnip")) {
            return "moon-turnip";
        }
        if (code.contains("star-melon")) {
            return "star-melon";
        }
        if (code.contains("cloud-cotton")) {
            return "cloud-cotton";
        }
        return "rice-grain";
    }

    private void consumeItem(InventoryItem item, int quantity) {
        item.setQuantity(item.getQuantity() - quantity);
        if (item.getQuantity() <= 0) {
            inventoryRepository.delete(item);
        } else {
            inventoryRepository.save(item);
        }
    }

    private String displayCropName(String crop) {
        return switch (crop) {
            case "wheat" -> "Wheat";
            case "carrots" -> "Carrots";
            case "moon-turnip" -> "Moon Turnip";
            case "star-melon" -> "Star Melon";
            case "cloud-cotton" -> "Cloud Cotton";
            default -> "Rice Grain";
        };
    }

    private String cropColor(String crop) {
        return switch (crop) {
            case "wheat" -> "bg-amber-300";
            case "carrots" -> "bg-orange-300";
            case "moon-turnip" -> "bg-violet-300";
            case "star-melon" -> "bg-yellow-300";
            case "cloud-cotton" -> "bg-slate-200";
            default -> "bg-lime-300";
        };
    }

    private InventoryItem findTreatForFeeding(String key, String inventoryItemId) {
        if (inventoryItemId != null && !inventoryItemId.isBlank()) {
            return inventoryRepository.findByPlayerKeyAndId(key, Long.parseLong(inventoryItemId))
                    .filter(item -> "Treat".equals(item.getType()))
                    .filter(item -> item.getQuantity() > 0)
                    .orElse(null);
        }

        return inventoryRepository.findByPlayerKeyOrderByIdAsc(key).stream()
                .filter(item -> "Treat".equals(item.getType()))
                .filter(item -> item.getQuantity() > 0)
                .findFirst()
                .orElse(null);
    }

    private TreatEffect treatEffect(String treatName) {
        return switch (treatName) {
            case "Honey Biscuit" -> new TreatEffect("Focused", "Faster production", 12);
            case "Clover Cookie" -> new TreatEffect("Lucky", "Increased happiness", 10);
            case "Moon Milk" -> new TreatEffect("Calm", "Longer boost duration", 16);
            default -> new TreatEffect("Delighted", "Higher yield next harvest", 8);
        };
    }

    private record TreatEffect(String mood, String boost, int energy) {
    }
}
