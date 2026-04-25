package dev.cheddah.waveback;

import dev.cheddah.waveback.rewards.RewardsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.OptionalInt;

public final class WaveBackExpansion extends PlaceholderExpansion {
    private final WaveBackPlugin plugin;
    private final RewardsManager rewardsManager;

    public WaveBackExpansion(WaveBackPlugin plugin, RewardsManager rewardsManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "waveback";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Cheddah";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        return switch (params.toLowerCase()) {
            case "greetings" -> String.valueOf(rewardsManager.getGreetings(player.getUniqueId()));
            case "rank" -> formatRank(rewardsManager.getRank(player.getUniqueId()));
            default -> null;
        };
    }

    private String formatRank(OptionalInt rank) {
        return rank.isPresent() ? String.valueOf(rank.getAsInt()) : "-";
    }
}
