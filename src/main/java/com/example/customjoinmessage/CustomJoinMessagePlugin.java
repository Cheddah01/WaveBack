package com.example.customjoinmessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class CustomJoinMessagePlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private boolean joinEnabled;
    private boolean joinSilent;
    private String joinMessageTemplate;
    private boolean leaveEnabled;
    private boolean leaveSilent;
    private String leaveMessageTemplate;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();

        getServer().getPluginManager().registerEvents(this, this);

        var command = getCommand("customjoinmessage");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!joinEnabled) {
            return;
        }

        if (joinSilent) {
            event.joinMessage(null);
            return;
        }

        event.joinMessage(renderMessage(joinMessageTemplate, event.getPlayer()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!leaveEnabled) {
            return;
        }

        if (leaveSilent) {
            event.quitMessage(null);
            return;
        }

        event.quitMessage(renderMessage(leaveMessageTemplate, event.getPlayer()));
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            loadSettings();
            sender.sendMessage(MINI_MESSAGE.deserialize("<green>CustomJoinMessage config reloaded."));
            return true;
        }

        sender.sendMessage(MINI_MESSAGE.deserialize("<red>Usage: /" + label + " reload"));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 1) {
            return List.of("reload");
        }

        return List.of();
    }

    private void loadSettings() {
        FileConfiguration config = getConfig();
        joinEnabled = config.getBoolean("join.enabled", config.getBoolean("enabled", true));
        joinSilent = config.getBoolean("join.silent", config.getBoolean("silent", false));
        joinMessageTemplate = config.getString(
                "join.message",
                config.getString(
                        "join-message",
                        "<gray>[<green>+<gray>] <yellow>{player}</yellow> joined the server!"
                )
        );

        leaveEnabled = config.getBoolean("leave.enabled", true);
        leaveSilent = config.getBoolean("leave.silent", false);
        leaveMessageTemplate = config.getString(
                "leave.message",
                "<gray>[<red>-<gray>] <yellow>{player}</yellow> left the server."
        );
    }

    private Component renderMessage(String template, Player player) {
        String rendered = template
                .replace("{player}", player.getName())
                .replace("{display_name}", PLAIN_TEXT.serialize(player.displayName()));

        return MINI_MESSAGE.deserialize(rendered);
    }
}
