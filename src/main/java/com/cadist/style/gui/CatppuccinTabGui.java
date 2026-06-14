package com.cadist.style.gui;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
import com.cadist.style.tab.TabIntegration;
import com.cadist.style.util.CatppuccinTheme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class CatppuccinTabGui implements InventoryHolder {

    private final CatppuccinStyler plugin;
    private final Player player;
    private Inventory inventory;

    public CatppuccinTabGui(CatppuccinStyler plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void openMain() {
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Catppuccin TAB \u25C6", CatppuccinTheme.MAUVE));
        fillBorder();

        TabIntegration tab = plugin.getTabIntegration();
        boolean available = tab != null;
        boolean enabled = available && tab.isEnabled();
        boolean tabList = available && tab.isTabListEnabled();
        boolean scoreboard = available && tab.isScoreboardEnabled();

        setItem(10, toggleItem(enabled, available));
        setItem(12, subToggleItem("Tab List Styling", tabList, available, Material.ENDER_EYE));
        setItem(14, subToggleItem("Scoreboard Styling", scoreboard, available, Material.PAPER));
        setItem(28, item(Material.MAP, name("Browse Tab Lines", CatppuccinTheme.PEACH),
                lore("Edit remembered header/footer gradients", CatppuccinTheme.SUBTEXT0)));
        setItem(30, item(Material.BOOK, name("Browse Scoreboard Lines", CatppuccinTheme.SKY),
                lore("Edit remembered scoreboard gradients", CatppuccinTheme.SUBTEXT0)));

        if (!available) {
            setItem(22, item(Material.BARRIER, name("TAB not installed", CatppuccinTheme.RED),
                    lore("Install NEZNAMY/TAB to use this feature", CatppuccinTheme.SUBTEXT0)));
        }

        setItem(49, item(Material.ARROW, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        TabIntegration tab = plugin.getTabIntegration();
        if (tab == null) {
            if (slot == 49) new CatppuccinGui(plugin, player).openMain();
            return;
        }

        switch (slot) {
            case 10 -> {
                tab.setEnabled(!tab.isEnabled());
                player.sendMessage(Component.text("Catppuccin TAB " + (tab.isEnabled() ? "enabled" : "disabled"), CatppuccinTheme.GREEN));
                openMain();
            }
            case 12 -> {
                tab.setTabListEnabled(!tab.isTabListEnabled());
                player.sendMessage(Component.text("Tab list styling " + (tab.isTabListEnabled() ? "enabled" : "disabled"), CatppuccinTheme.GREEN));
                openMain();
            }
            case 14 -> {
                tab.setScoreboardEnabled(!tab.isScoreboardEnabled());
                player.sendMessage(Component.text("Scoreboard styling " + (tab.isScoreboardEnabled() ? "enabled" : "disabled"), CatppuccinTheme.GREEN));
                openMain();
            }
            case 28 -> {
                if (tab.isEnabled() && tab.isTabListEnabled()) {
                    new LineBrowserGui(plugin, player, tab.getTabLineRegistry(), "\u25C6 Tab Lines \u25C6", this::openMain).open();
                } else {
                    player.sendMessage(Component.text("Enable Catppuccin TAB and Tab List styling first.", CatppuccinTheme.RED));
                }
            }
            case 30 -> {
                if (tab.isEnabled() && tab.isScoreboardEnabled()) {
                    new LineBrowserGui(plugin, player, tab.getScoreboardLineRegistry(), "\u25C6 Scoreboard Lines \u25C6", this::openMain).open();
                } else {
                    player.sendMessage(Component.text("Enable Catppuccin TAB and Scoreboard styling first.", CatppuccinTheme.RED));
                }
            }
            case 49 -> new CatppuccinGui(plugin, player).openMain();
        }
    }

    private ItemStack toggleItem(boolean enabled, boolean available) {
        Material mat = available ? (enabled ? Material.LIME_WOOL : Material.RED_WOOL) : Material.GRAY_WOOL;
        String status = available ? (enabled ? "ON" : "OFF") : "N/A";
        return item(mat, name("Catppuccin TAB: " + status, enabled ? CatppuccinTheme.GREEN : CatppuccinTheme.RED),
                lore(available ? "Click to toggle" : "TAB plugin not found", CatppuccinTheme.SUBTEXT0));
    }

    private ItemStack subToggleItem(String label, boolean enabled, boolean available, Material material) {
        Material mat = available ? (enabled ? Material.LIME_WOOL : Material.RED_WOOL) : Material.GRAY_WOOL;
        String status = available ? (enabled ? "ON" : "OFF") : "N/A";
        return item(mat, name(label + ": " + status, enabled ? CatppuccinTheme.GREEN : CatppuccinTheme.RED),
                lore("Click to toggle", CatppuccinTheme.SUBTEXT0));
    }

    private Material gradientMaterial(String gradientId) {
        return switch (gradientId.toLowerCase()) {
            case "rose", "flame", "coral", "ember", "cherry" -> Material.RED_WOOL;
            case "sunset", "peachy", "citrus" -> Material.ORANGE_WOOL;
            case "meadow", "forest", "spring", "mint" -> Material.LIME_WOOL;
            case "ocean", "sea", "ice" -> Material.CYAN_WOOL;
            case "twilight", "galaxy", "violet", "lavender_preset" -> Material.PURPLE_WOOL;
            case "berry", "bubblegum", "dusk" -> Material.MAGENTA_WOOL;
            case "frost", "sky" -> Material.LIGHT_BLUE_WOOL;
            case "aurora", "neon", "rainbow" -> Material.YELLOW_WOOL;
            case "royal", "cotton" -> Material.PINK_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private void fillBorder() {
        ItemStack glass = item(Material.BLACK_STAINED_GLASS_PANE, Component.text(" "), Collections.emptyList());
        for (int i = 0; i < 9; i++) inventory.setItem(i, glass);
        for (int i = 45; i < 54; i++) inventory.setItem(i, glass);
        for (int row = 1; row <= 4; row++) {
            inventory.setItem(row * 9, glass);
            inventory.setItem(row * 9 + 8, glass);
        }
    }

    private void setItem(int slot, ItemStack item) {
        inventory.setItem(slot, item);
    }

    private ItemStack item(Material mat, Component name, List<Component>... lores) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        List<Component> combined = new ArrayList<>();
        for (List<Component> lore : lores) {
            combined.addAll(lore);
        }
        if (!combined.isEmpty()) meta.lore(combined);
        item.setItemMeta(meta);
        return item;
    }

    private Component name(String text, net.kyori.adventure.text.format.TextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private List<Component> lore(String text, net.kyori.adventure.text.format.TextColor color) {
        return List.of(Component.text(text, color).decoration(TextDecoration.ITALIC, false));
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
