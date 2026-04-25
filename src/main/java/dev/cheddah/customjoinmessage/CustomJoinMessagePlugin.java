package dev.cheddah.customjoinmessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CustomJoinMessagePlugin extends JavaPlugin implements Listener, TabExecutor {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private boolean joinEnabled;
    private boolean joinSilent;
    private String joinMessageTemplate;
    private SoundSettings joinSound;
    private FireworkSettings joinFirework;
    private boolean leaveEnabled;
    private boolean leaveSilent;
    private String leaveMessageTemplate;
    private SoundSettings leaveSound;
    private FireworkSettings leaveFirework;

    @Override
    public void onEnable() {
        updateConfigDefaults();
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
        playSoundToOnlinePlayers(joinSound, event.getPlayer().getUniqueId());
        spawnFirework(event.getPlayer(), joinFirework);
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
        // On quit, the player is already disconnected, but keep the exclusion hook for call-site consistency.
        playSoundToOnlinePlayers(leaveSound, event.getPlayer().getUniqueId());
        spawnFirework(event.getPlayer(), leaveFirework);
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
            updateConfigDefaults();
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
        joinFirework = loadFireworkSettings(config, "join.firework");

        leaveEnabled = config.getBoolean("leave.enabled", true);
        leaveSilent = config.getBoolean("leave.silent", false);
        leaveMessageTemplate = config.getString(
                "leave.message",
                "<gray>[<red>-<gray>] <yellow>{player}</yellow> left the server."
        );
        leaveSound = loadSoundSettings(config, "leave.sound", "ENTITY_ITEM_BREAK");
        leaveFirework = loadFireworkSettings(config, "leave.firework");
    }

    private void updateConfigDefaults() {
        saveDefaultConfig();
        getConfig().options().copyDefaults(true);
        saveConfig();
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

    private FireworkSettings loadFireworkSettings(FileConfiguration config, String path) {
        boolean fireworkEnabled = config.getBoolean(path + ".enabled", false);
        int power = Math.max(0, Math.min(3, config.getInt(path + ".power", 1)));
        boolean flicker = config.getBoolean(path + ".flicker", false);
        boolean trail = config.getBoolean(path + ".trail", true);
        boolean instantDetonate = config.getBoolean(path + ".instant-detonate", false);

        FireworkEffect.Type type = FireworkEffect.Type.BALL;
        String typeName = config.getString(path + ".type", "BALL");
        try {
            type = FireworkEffect.Type.valueOf(typeName.toUpperCase());
        } catch (IllegalArgumentException exception) {
            getLogger().warning("Invalid firework type '" + typeName + "' at " + path + ".type; using BALL.");
        }

        List<Color> colors = loadColors(config.getStringList(path + ".colors"), List.of(Color.LIME, Color.YELLOW));
        List<Color> fadeColors = loadColors(config.getStringList(path + ".fade-colors"), List.of(Color.AQUA));

        FireworkEffect effect = FireworkEffect.builder()
                .with(type)
                .withColor(colors)
                .withFade(fadeColors)
                .flicker(flicker)
                .trail(trail)
                .build();

        return new FireworkSettings(fireworkEnabled, power, effect, instantDetonate);
    }

    private List<Color> loadColors(List<String> colorNames, List<Color> defaults) {
        List<Color> colors = new ArrayList<>();
        for (String colorName : colorNames) {
            Color color = parseColor(colorName);
            if (color != null) {
                colors.add(color);
            } else {
                getLogger().warning("Invalid firework color '" + colorName + "'; skipping it.");
            }
        }

        return colors.isEmpty() ? defaults : colors;
    }

    private @Nullable Color parseColor(String colorName) {
        return switch (colorName.toUpperCase()) {
            case "AQUA" -> Color.AQUA;
            case "BLACK" -> Color.BLACK;
            case "BLUE" -> Color.BLUE;
            case "FUCHSIA" -> Color.FUCHSIA;
            case "GRAY", "GREY" -> Color.GRAY;
            case "GREEN" -> Color.GREEN;
            case "LIME" -> Color.LIME;
            case "MAROON" -> Color.MAROON;
            case "NAVY" -> Color.NAVY;
            case "OLIVE" -> Color.OLIVE;
            case "ORANGE" -> Color.ORANGE;
            case "PURPLE" -> Color.PURPLE;
            case "RED" -> Color.RED;
            case "SILVER" -> Color.SILVER;
            case "TEAL" -> Color.TEAL;
            case "WHITE" -> Color.WHITE;
            case "YELLOW" -> Color.YELLOW;
            default -> null;
        };
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

    private void spawnFirework(Player player, FireworkSettings settings) {
        if (!settings.enabled()) {
            return;
        }

        getServer().getScheduler().runTaskLater(this, () -> {
            Firework firework = player.getWorld().spawn(player.getLocation(), Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.setPower(settings.power());
            meta.addEffect(settings.effect());
            firework.setFireworkMeta(meta);
            firework.setShotAtAngle(false);
            if (settings.instantDetonate()) {
                firework.detonate();
            }
        }, 5L);
    }

    private record SoundSettings(boolean enabled, @Nullable Sound sound, float volume, float pitch) {
    }

    private record FireworkSettings(boolean enabled, int power, FireworkEffect effect, boolean instantDetonate) {
    }
}
