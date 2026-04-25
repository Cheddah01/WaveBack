package dev.cheddah.customjoinmessage.rewards;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RewardsManager {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final int PLAY_TICKS_PER_MINUTE = 1200;

    private final JavaPlugin plugin;
    private final @Nullable Economy economy;
    private final ConcurrentHashMap<UUID, GreetingWindow> activeWindows = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Long> rewardCooldowns = new ConcurrentHashMap<>();

    private RewardsConfig config;

    public RewardsManager(JavaPlugin plugin, @Nullable Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.config = RewardsConfig.load(plugin, plugin.getConfig(), economy, false);
    }

    public void reload(boolean logMoneyWarnings) {
        config = RewardsConfig.load(plugin, plugin.getConfig(), economy, logMoneyWarnings);
    }

    public void shutdown() {
        for (GreetingWindow window : activeWindows.values()) {
            window.cancel();
        }
        activeWindows.clear();
    }

    public void openWindow(Player joiner, boolean hasPlayedBefore) {
        if (!config.enabled()) {
            return;
        }

        if (!hasPlayedBefore && !config.rewardOnFirstJoin()) {
            return;
        }

        if (!hasMinimumPlaytime(joiner)) {
            return;
        }

        GreetingWindow previousWindow = activeWindows.remove(joiner.getUniqueId());
        if (previousWindow != null) {
            previousWindow.cancel();
        }

        BukkitTask closeTask = Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> activeWindows.remove(joiner.getUniqueId()),
                config.greetingWindowSeconds() * 20L
        );
        activeWindows.put(joiner.getUniqueId(), new GreetingWindow(joiner.getUniqueId(), joiner.getName(), closeTask));
    }

    public void handleGreeting(Player greeter, String message) {
        if (!config.enabled() || !greeter.hasPermission("customjoinmessage.greet") || !config.matchesTrigger(message)) {
            return;
        }

        for (GreetingWindow window : activeWindows.values()) {
            if (!canReward(greeter, window)) {
                continue;
            }

            Player joiner = Bukkit.getPlayer(window.joinerId());
            if (joiner == null) {
                activeWindows.remove(window.joinerId());
                window.cancel();
                continue;
            }

            window.markRewarded(greeter.getUniqueId());
            if (!greeter.hasPermission("customjoinmessage.bypasscooldown")) {
                rewardCooldowns.put(greeter.getUniqueId(), System.currentTimeMillis());
            }

            giveRewards(greeter, joiner);
            sendMessages(greeter, joiner);
        }
    }

    public void testRewards(Player player) {
        giveRewards(player, player);
        sendMessages(player, player);
    }

    private boolean hasMinimumPlaytime(Player joiner) {
        int minimumMinutes = config.minimumJoinerPlaytimeMinutes();
        if (minimumMinutes <= 0) {
            return true;
        }

        long requiredTicks = (long) minimumMinutes * PLAY_TICKS_PER_MINUTE;
        return joiner.getStatistic(Statistic.PLAY_ONE_MINUTE) >= requiredTicks;
    }

    private boolean canReward(Player greeter, GreetingWindow window) {
        if (greeter.getUniqueId().equals(window.joinerId()) || window.hasRewarded(greeter.getUniqueId())) {
            return false;
        }

        if (!window.hasRewardRoom(config.maxRewardsPerJoin())) {
            return false;
        }

        return greeter.hasPermission("customjoinmessage.bypasscooldown") || !isOnCooldown(greeter.getUniqueId());
    }

    private boolean isOnCooldown(UUID greeterId) {
        long cooldownMillis = config.rewardCooldownMillis();
        if (cooldownMillis <= 0) {
            return false;
        }

        Long lastRewardTime = rewardCooldowns.get(greeterId);
        return lastRewardTime != null && System.currentTimeMillis() - lastRewardTime < cooldownMillis;
    }

    private void giveRewards(Player greeter, Player joiner) {
        for (Reward reward : config.rewards()) {
            reward.give(greeter, joiner);
        }
    }

    private void sendMessages(Player greeter, Player joiner) {
        String personalMessage = replacePlaceholders(config.rewardReceivedMessage(), greeter, joiner);
        if (!personalMessage.isBlank()) {
            greeter.sendMessage(MINI_MESSAGE.deserialize(personalMessage));
        }

        String broadcastMessage = replacePlaceholders(config.broadcastMessage(), greeter, joiner);
        if (!broadcastMessage.isBlank()) {
            Bukkit.broadcast(MINI_MESSAGE.deserialize(broadcastMessage));
        }
    }

    private String replacePlaceholders(String input, Player greeter, Player joiner) {
        return input
                .replace("{player}", greeter.getName())
                .replace("{joiner}", joiner.getName());
    }

    private static final class GreetingWindow {
        private final UUID joinerId;
        private final String joinerName;
        private final BukkitTask closeTask;
        private final Set<UUID> rewardedGreeters = new HashSet<>();
        private int rewardCount;

        private GreetingWindow(UUID joinerId, String joinerName, BukkitTask closeTask) {
            this.joinerId = joinerId;
            this.joinerName = joinerName;
            this.closeTask = closeTask;
        }

        private UUID joinerId() {
            return joinerId;
        }

        private boolean hasRewarded(UUID greeterId) {
            return rewardedGreeters.contains(greeterId);
        }

        private boolean hasRewardRoom(int maxRewardsPerJoin) {
            return maxRewardsPerJoin == 0 || rewardCount < maxRewardsPerJoin;
        }

        private void markRewarded(UUID greeterId) {
            rewardedGreeters.add(greeterId);
            rewardCount++;
        }

        private void cancel() {
            closeTask.cancel();
        }
    }
}
