package dev.cheddah.waveback.rewards;

import dev.cheddah.waveback.PlaceholderService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandReward implements Reward {
    private final String command;
    private final PlaceholderService placeholderService;

    public CommandReward(String command, PlaceholderService placeholderService) {
        this.command = command;
        this.placeholderService = placeholderService;
    }

    @Override
    public void give(Player greeter, Player joiner) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), placeholderService.apply(greeter, replacePlaceholders(command, greeter, joiner)));
    }

    private String replacePlaceholders(String input, Player greeter, Player joiner) {
        return input
                .replace("{player}", greeter.getName())
                .replace("{joiner}", joiner.getName());
    }
}
