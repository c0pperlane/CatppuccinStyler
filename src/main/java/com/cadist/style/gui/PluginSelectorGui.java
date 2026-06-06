package com.cadist.style.gui;

import com.cadist.style.CatppuccinStyler;
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

public class PluginSelectorGui implements InventoryHolder {

    private final CatppuccinStyler plugin;
    private final Player player;
    private Inventory inventory;
    private int page = 0;

    public PluginSelectorGui(CatppuccinStyler plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
    }

    public void open() {
        build();
        player.openInventory(inventory);
    }

    private void build() {
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Select Plugin to Exclude \u25C6", CatppuccinTheme.RED));
        fillBorder();

        List<org.bukkit.plugin.Plugin> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(p -> !plugin.getStyleConfig().isExcludedPlugin(p.getName()))
                .sorted(Comparator.comparing(org.bukkit.plugin.Plugin::getName))
                .toList();

        int itemsPerPage = 28; // slots 10-16, 19-25, 28-34, 37-43
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, plugins.size());

        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        int slotIdx = 0;
        for (int i = start; i < end; i++) {
            org.bukkit.plugin.Plugin p = plugins.get(i);
            Material mat = p.isEnabled() ? Material.LIME_WOOL : Material.RED_WOOL;
            setItem(slots[slotIdx++], item(mat, name(p.getName(), CatppuccinTheme.TEXT),
                    lore("Click to exclude", CatppuccinTheme.SUBTEXT0),
                    lore(p.getDescription().getDescription() != null ? p.getDescription().getDescription() : "No description", CatppuccinTheme.SURFACE2)));
        }

        if (page > 0) {
            setItem(45, item(Material.ARROW, name("\u2190 Prev", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        }
        if (end < plugins.size()) {
            setItem(53, item(Material.ARROW, name("Next \u2192", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        }
        setItem(49, item(Material.BARRIER, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == 49) {
            new CatppuccinGui(plugin, player).openExclusions();
            return;
        }
        if (slot == 45 && page > 0) {
            page--;
            build();
            player.openInventory(inventory);
            return;
        }
        if (slot == 53) {
            page++;
            build();
            player.openInventory(inventory);
            return;
        }

        List<org.bukkit.plugin.Plugin> plugins = Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .filter(p -> !plugin.getStyleConfig().isExcludedPlugin(p.getName()))
                .sorted(Comparator.comparing(org.bukkit.plugin.Plugin::getName))
                .toList();

        int itemsPerPage = 28;
        int start = page * itemsPerPage;
        int[] slots = {10,11,12,13,14,15,16,19,20,21,22,23,24,25,28,29,30,31,32,33,34,37,38,39,40,41,42,43};
        for (int i = 0; i < slots.length && (start + i) < plugins.size(); i++) {
            if (slots[i] == slot) {
                org.bukkit.plugin.Plugin p = plugins.get(start + i);
                plugin.getStyleConfig().addExcludedPlugin(p.getName());
                player.sendMessage(Component.text("Excluded plugin: " + p.getName(), CatppuccinTheme.GREEN));
                build();
                player.openInventory(inventory);
                return;
            }
        }
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
