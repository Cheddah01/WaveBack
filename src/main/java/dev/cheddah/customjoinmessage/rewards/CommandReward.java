package dev.cheddah.customjoinmessage.rewards;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class CommandReward implements Reward {
    private final String command;

    public CommandReward(String command) {
        this.command = command;
    }

    @Override
    public void give(Player greeter, Player joiner) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), replacePlaceholders(command, greeter, joiner));
    }

    private String replacePlaceholders(String input, Player greeter, Player joiner) {
        return input
                .replace("{player}", greeter.getName())
                .replace("{joiner}", joiner.getName());
    }
}
