package dev.cheddah.waveback;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class PlaceholderService {
    private final boolean enabled;

    public PlaceholderService(boolean enabled) {
        this.enabled = enabled;
    }

    public String apply(@Nullable Player player, String input) {
        if (!enabled || player == null || input.isBlank()) {
            return input;
        }

        return PlaceholderAPI.setPlaceholders(player, input);
    }
}
