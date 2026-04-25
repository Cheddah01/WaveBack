package dev.cheddah.waveback.rewards;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public final class ChatWatcher implements Listener {
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();

    private final JavaPlugin plugin;
    private final RewardsManager rewardsManager;

    public ChatWatcher(JavaPlugin plugin, RewardsManager rewardsManager) {
        this.plugin = plugin;
        this.rewardsManager = rewardsManager;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        String message = PLAIN_TEXT.serialize(event.message());
        Bukkit.getScheduler().runTask(plugin, () -> rewardsManager.handleGreeting(event.getPlayer(), message));
    }
}
