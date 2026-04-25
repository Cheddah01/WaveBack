package dev.cheddah.waveback.rewards;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

public final class MoneyReward implements Reward {
    private final Economy economy;
    private final double amount;

    public MoneyReward(Economy economy, double amount) {
        this.economy = economy;
        this.amount = amount;
    }

    @Override
    public void give(Player greeter, Player joiner) {
        economy.depositPlayer(greeter, amount);
    }
}
