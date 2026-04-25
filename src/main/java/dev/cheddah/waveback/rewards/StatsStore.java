package dev.cheddah.waveback.rewards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StatsStore {
    private final JavaPlugin plugin;
    private final File statsFile;
    private final Map<UUID, GreetingStats> stats = new ConcurrentHashMap<>();

    public StatsStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        load();
    }

    public void recordGreeting(Player player) {
        GreetingStats playerStats = stats.computeIfAbsent(player.getUniqueId(), ignored -> new GreetingStats(player.getName(), 0));
        playerStats.name = player.getName();
        playerStats.greetings++;
        save();
    }

    public int getGreetings(UUID playerId) {
        GreetingStats playerStats = stats.get(playerId);
        return playerStats == null ? 0 : playerStats.greetings;
    }

    public List<LeaderboardEntry> top(int limit) {
        return stats.entrySet().stream()
                .filter(entry -> entry.getValue().greetings > 0)
                .sorted(Map.Entry.<UUID, GreetingStats>comparingByValue(Comparator.comparingInt(GreetingStats::greetings)).reversed())
                .limit(limit)
                .map(entry -> new LeaderboardEntry(entry.getKey(), entry.getValue().name, entry.getValue().greetings))
                .toList();
    }

    public OptionalInt rank(UUID playerId) {
        List<UUID> rankedPlayerIds = stats.entrySet().stream()
                .filter(entry -> entry.getValue().greetings > 0)
                .sorted(Map.Entry.<UUID, GreetingStats>comparingByValue(Comparator.comparingInt(GreetingStats::greetings)).reversed())
                .map(Map.Entry::getKey)
                .toList();

        for (int index = 0; index < rankedPlayerIds.size(); index++) {
            if (rankedPlayerIds.get(index).equals(playerId)) {
                return OptionalInt.of(index + 1);
            }
        }

        return OptionalInt.empty();
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, GreetingStats> entry : stats.entrySet()) {
            String path = "players." + entry.getKey();
            config.set(path + ".name", entry.getValue().name);
            config.set(path + ".greetings", entry.getValue().greetings);
        }

        try {
            config.save(statsFile);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save greeting stats: " + exception.getMessage());
        }
    }

    private void load() {
        if (!statsFile.exists()) {
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(statsFile);
        ConfigurationSection playersSection = config.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String key : playersSection.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                String name = playersSection.getString(key + ".name", "Unknown");
                int greetings = Math.max(0, playersSection.getInt(key + ".greetings", 0));
                stats.put(playerId, new GreetingStats(name, greetings));
            } catch (IllegalArgumentException exception) {
                plugin.getLogger().warning("Skipping invalid stats entry for UUID '" + key + "'.");
            }
        }
    }

    public record LeaderboardEntry(UUID playerId, String name, int greetings) {
    }

    private static final class GreetingStats {
        private String name;
        private int greetings;

        private GreetingStats(String name, int greetings) {
            this.name = name;
            this.greetings = greetings;
        }

        private int greetings() {
            return greetings;
        }
    }
}
