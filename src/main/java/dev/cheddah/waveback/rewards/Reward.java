package dev.cheddah.waveback.rewards;

import org.bukkit.entity.Player;

public interface Reward {
    void give(Player greeter, Player joiner);
}
