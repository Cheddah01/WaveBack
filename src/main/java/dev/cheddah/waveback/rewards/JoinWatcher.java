package dev.cheddah.waveback.rewards;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class JoinWatcher implements Listener {
    private final RewardsManager rewardsManager;

    public JoinWatcher(RewardsManager rewardsManager) {
        this.rewardsManager = rewardsManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        rewardsManager.openWindow(event.getPlayer(), event.getPlayer().hasPlayedBefore());
    }
}
