package dev.cheddah.customjoinmessage.rewards;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RewardsConfig {
    private final boolean enabled;
    private final int greetingWindowSeconds;
    private final List<String> triggers;
    private final boolean wholeMessageOnly;
    private final int maxRewardsPerJoin;
    private final long rewardCooldownMillis;
    private final boolean rewardOnFirstJoin;
    private final int minimumJoinerPlaytimeMinutes;
    private final List<Reward> rewards;
    private final String rewardReceivedMessage;
    private final String broadcastMessage;

    private RewardsConfig(
            boolean enabled,
            int greetingWindowSeconds,
            List<String> triggers,
            boolean wholeMessageOnly,
            int maxRewardsPerJoin,
            long rewardCooldownMillis,
            boolean rewardOnFirstJoin,
            int minimumJoinerPlaytimeMinutes,
            List<Reward> rewards,
            String rewardReceivedMessage,
            String broadcastMessage
    ) {
        this.enabled = enabled;
        this.greetingWindowSeconds = greetingWindowSeconds;
        this.triggers = triggers;
        this.wholeMessageOnly = wholeMessageOnly;
        this.maxRewardsPerJoin = maxRewardsPerJoin;
        this.rewardCooldownMillis = rewardCooldownMillis;
        this.rewardOnFirstJoin = rewardOnFirstJoin;
        this.minimumJoinerPlaytimeMinutes = minimumJoinerPlaytimeMinutes;
        this.rewards = rewards;
        this.rewardReceivedMessage = rewardReceivedMessage;
        this.broadcastMessage = broadcastMessage;
    }

    public static RewardsConfig load(
            JavaPlugin plugin,
            FileConfiguration config,
            @Nullable Economy economy,
            boolean logMoneyWarnings
    ) {
        boolean enabled = config.getBoolean("rewards.enabled", true);
        int greetingWindowSeconds = Math.max(1, config.getInt("rewards.greeting-window-seconds", 30));
        List<String> triggers = normalizeTriggers(config.getStringList("rewards.triggers"));
        boolean wholeMessageOnly = config.getBoolean("rewards.whole-message-only", true);
        int maxRewardsPerJoin = Math.max(0, config.getInt("rewards.max-rewards-per-join", 3));
        long rewardCooldownMillis = Math.max(0, config.getLong("rewards.reward-cooldown-seconds", 300)) * 1000L;
        boolean rewardOnFirstJoin = config.getBoolean("rewards.reward-on-first-join", false);
        int minimumJoinerPlaytimeMinutes = Math.max(0, config.getInt("rewards.minimum-joiner-playtime-minutes", 10));
        List<Reward> rewards = loadRewards(plugin, config, economy, logMoneyWarnings);
        String rewardReceivedMessage = config.getString(
                "rewards.messages.reward-received",
                "<gray>✦ Thanks for welcoming <yellow>{joiner}</yellow><gray> back!"
        );
        String broadcastMessage = config.getString("rewards.messages.broadcast", "");

        return new RewardsConfig(
                enabled,
                greetingWindowSeconds,
                triggers,
                wholeMessageOnly,
                maxRewardsPerJoin,
                rewardCooldownMillis,
                rewardOnFirstJoin,
                minimumJoinerPlaytimeMinutes,
                rewards,
                rewardReceivedMessage,
                broadcastMessage
        );
    }

    private static List<String> normalizeTriggers(List<String> configuredTriggers) {
        List<String> normalized = new ArrayList<>();
        for (String trigger : configuredTriggers) {
            String value = normalizeMessage(trigger);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }

        if (normalized.isEmpty()) {
            return List.of("wb", "welcome back", "wbb", "dubs");
        }

        return List.copyOf(normalized);
    }

    private static List<Reward> loadRewards(
            JavaPlugin plugin,
            FileConfiguration config,
            @Nullable Economy economy,
            boolean logMoneyWarnings
    ) {
        List<Reward> rewards = new ArrayList<>();
        List<Map<?, ?>> entries = config.getMapList("rewards.bundle");

        for (int index = 0; index < entries.size(); index++) {
            Map<?, ?> section = entries.get(index);

            String type = getString(section, "type", "").toLowerCase(Locale.ROOT);
            switch (type) {
                case "command" -> loadCommandReward(plugin, section, index, rewards);
                case "money" -> loadMoneyReward(plugin, section, economy, logMoneyWarnings, rewards);
                case "item" -> loadItemReward(plugin, section, index, rewards);
                default -> plugin.getLogger().warning("Unknown reward type '" + type + "' at rewards.bundle[" + index + "].");
            }
        }

        return List.copyOf(rewards);
    }

    private static void loadCommandReward(JavaPlugin plugin, Map<?, ?> section, int index, List<Reward> rewards) {
        String command = getString(section, "command", "");
        if (command.isBlank()) {
            plugin.getLogger().warning("Command reward at rewards.bundle[" + index + "] is missing command.");
            return;
        }

        rewards.add(new CommandReward(command));
    }

    private static void loadMoneyReward(
            JavaPlugin plugin,
            Map<?, ?> section,
            @Nullable Economy economy,
            boolean logMoneyWarnings,
            List<Reward> rewards
    ) {
        if (economy == null) {
            if (logMoneyWarnings) {
                plugin.getLogger().warning("Money rewards are configured, but Vault economy is unavailable; money rewards will be skipped.");
            }
            return;
        }

        rewards.add(new MoneyReward(economy, getDouble(section, "amount", 0.0)));
    }

    private static void loadItemReward(JavaPlugin plugin, Map<?, ?> section, int index, List<Reward> rewards) {
        String materialName = getString(section, "material", "");
        Material material = Material.matchMaterial(materialName);
        if (material == null || !material.isItem()) {
            plugin.getLogger().warning("Item reward at rewards.bundle[" + index + "] has invalid material '" + materialName + "'.");
            return;
        }

        int amount = Math.max(1, getInt(section, "amount", 1));
        String name = getNullableString(section, "name");
        List<String> lore = getStringList(section, "lore");
        rewards.add(new ItemReward(material, amount, name, lore));
    }

    private static String getString(Map<?, ?> section, String key, String fallback) {
        String value = getNullableString(section, key);
        return value == null ? fallback : value;
    }

    private static @Nullable String getNullableString(Map<?, ?> section, String key) {
        Object value = section.get(key);
        return value == null ? null : value.toString();
    }

    private static int getInt(Map<?, ?> section, String key, int fallback) {
        Object value = section.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return value == null ? fallback : Integer.parseInt(value.toString());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static double getDouble(Map<?, ?> section, String key, double fallback) {
        Object value = section.get(key);
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return value == null ? fallback : Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static List<String> getStringList(Map<?, ?> section, String key) {
        Object value = section.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }

        return list.stream().map(Object::toString).toList();
    }

    public boolean matchesTrigger(String message) {
        String normalizedMessage = normalizeMessage(message);
        if (normalizedMessage.isBlank()) {
            return false;
        }

        for (String trigger : triggers) {
            if (wholeMessageOnly) {
                if (normalizedMessage.equals(trigger)) {
                    return true;
                }
            } else if (normalizedMessage.contains(trigger)) {
                return true;
            }
        }

        return false;
    }

    private static String normalizeMessage(String message) {
        return message
                .trim()
                .replaceAll("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$", "")
                .toLowerCase(Locale.ROOT);
    }

    public boolean enabled() {
        return enabled;
    }

    public int greetingWindowSeconds() {
        return greetingWindowSeconds;
    }

    public int maxRewardsPerJoin() {
        return maxRewardsPerJoin;
    }

    public long rewardCooldownMillis() {
        return rewardCooldownMillis;
    }

    public boolean rewardOnFirstJoin() {
        return rewardOnFirstJoin;
    }

    public int minimumJoinerPlaytimeMinutes() {
        return minimumJoinerPlaytimeMinutes;
    }

    public List<Reward> rewards() {
        return rewards;
    }

    public String rewardReceivedMessage() {
        return rewardReceivedMessage;
    }

    public String broadcastMessage() {
        return broadcastMessage;
    }
}
