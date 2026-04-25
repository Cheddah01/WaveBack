package dev.cheddah.customjoinmessage.rewards;

import org.bukkit.entity.Player;

public interface Reward {
    void give(Player greeter, Player joiner);
}
