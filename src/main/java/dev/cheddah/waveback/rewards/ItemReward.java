package dev.cheddah.waveback.rewards;

import dev.cheddah.waveback.PlaceholderService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public final class ItemReward implements Reward {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private final Material material;
    private final int amount;
    private final String name;
    private final List<String> lore;
    private final PlaceholderService placeholderService;

    public ItemReward(Material material, int amount, String name, List<String> lore, PlaceholderService placeholderService) {
        this.material = material;
        this.amount = amount;
        this.name = name;
        this.lore = lore;
        this.placeholderService = placeholderService;
    }

    @Override
    public void give(Player greeter, Player joiner) {
        ItemStack stack = new ItemStack(material, amount);
        ItemMeta meta = stack.getItemMeta();

        if (meta != null) {
            if (name != null && !name.isBlank()) {
                meta.displayName(deserialize(placeholderService.apply(greeter, replacePlaceholders(name, greeter, joiner))));
            }

            if (!lore.isEmpty()) {
                meta.lore(lore.stream()
                        .map(line -> placeholderService.apply(greeter, replacePlaceholders(line, greeter, joiner)))
                        .map(ItemReward::deserialize)
                        .toList());
            }

            stack.setItemMeta(meta);
        }

        Map<Integer, ItemStack> leftovers = greeter.getInventory().addItem(stack);
        for (ItemStack leftover : leftovers.values()) {
            greeter.getWorld().dropItemNaturally(greeter.getLocation(), leftover);
        }
    }

    private static Component deserialize(String input) {
        return MINI_MESSAGE.deserialize(input).decoration(TextDecoration.ITALIC, false);
    }

    private String replacePlaceholders(String input, Player greeter, Player joiner) {
        return input
                .replace("{player}", greeter.getName())
                .replace("{joiner}", joiner.getName());
    }
}
