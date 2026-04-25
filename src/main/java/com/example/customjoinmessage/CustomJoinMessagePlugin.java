package com.example.customjoinmessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Sound;
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
import java.util.UUID;

public final class CustomJoinMessagePlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private boolean joinEnabled;
    private boolean joinSilent;
    private String joinMessageTemplate;
    private SoundSettings joinSound;
    private boolean leaveEnabled;
    private boolean leaveSilent;
    private String leaveMessageTemplate;
    private SoundSettings leaveSound;

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
        playSoundToOnlinePlayers(joinSound, null);
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
        playSoundToOnlinePlayers(leaveSound, event.getPlayer().getUniqueId());
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
        joinSound = loadSoundSettings(config, "join.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");

        leaveEnabled = config.getBoolean("leave.enabled", true);
        leaveSilent = config.getBoolean("leave.silent", false);
        leaveMessageTemplate = config.getString(
                "leave.message",
                "<gray>[<red>-<gray>] <yellow>{player}</yellow> left the server."
        );
        leaveSound = loadSoundSettings(config, "leave.sound", "ENTITY_ITEM_BREAK");
    }

    private Component renderMessage(String template, Player player) {
        String rendered = template
                .replace("{player}", player.getName())
                .replace("{display_name}", PLAIN_TEXT.serialize(player.displayName()));

        return MINI_MESSAGE.deserialize(rendered);
    }

    private SoundSettings loadSoundSettings(FileConfiguration config, String path, String defaultName) {
        boolean soundEnabled = config.getBoolean(path + ".enabled", false);
        String soundName = config.getString(path + ".name", defaultName);
        float volume = (float) config.getDouble(path + ".volume", 1.0);
        float pitch = (float) config.getDouble(path + ".pitch", 1.0);

        Sound sound = null;
        if (soundEnabled) {
            try {
                sound = Sound.valueOf(soundName.toUpperCase());
            } catch (IllegalArgumentException exception) {
                getLogger().warning("Invalid sound '" + soundName + "' at " + path + ".name; sound disabled.");
                soundEnabled = false;
            }
        }

        return new SoundSettings(soundEnabled, sound, volume, pitch);
    }

    private void playSoundToOnlinePlayers(SoundSettings settings, @Nullable UUID excludedPlayerId) {
        if (!settings.enabled() || settings.sound() == null) {
            return;
        }

        for (Player player : getServer().getOnlinePlayers()) {
            if (excludedPlayerId != null && player.getUniqueId().equals(excludedPlayerId)) {
                continue;
            }

            player.playSound(player.getLocation(), settings.sound(), settings.volume(), settings.pitch());
        }
    }

    private record SoundSettings(boolean enabled, @Nullable Sound sound, float volume, float pitch) {
    }
}
