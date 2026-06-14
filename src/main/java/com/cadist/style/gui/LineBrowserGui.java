package com.cadist.style.gui;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
import com.cadist.style.config.LinePattern;
import com.cadist.style.config.LinePatternRegistry;
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

public class LineBrowserGui implements InventoryHolder {

    private final CatppuccinStyler plugin;
    private final Player player;
    private final LinePatternRegistry registry;
    private final String title;
    private final Runnable backAction;

    private Inventory inventory;
    private int page = 0;
    private String editingFingerprint = null;

    public LineBrowserGui(CatppuccinStyler plugin, Player player, LinePatternRegistry registry, String title, Runnable backAction) {
        this.plugin = plugin;
        this.player = player;
        this.registry = registry;
        this.title = title;
        this.backAction = backAction;
    }

    public void open() {
        buildBrowse();
        player.openInventory(inventory);
    }

    private List<LinePattern> getPatterns() {
        List<LinePattern> all = new ArrayList<>(registry.allPatterns());
        all.sort(Comparator.comparing(LinePattern::getFingerprint));
        return all;
    }

    private void buildBrowse() {
        inventory = Bukkit.createInventory(this, 54, Component.text(title, CatppuccinTheme.MAUVE));
        fillBorder();

        List<LinePattern> patterns = getPatterns();
        int itemsPerPage = 28;
        int start = page * itemsPerPage;
        int end = Math.min(start + itemsPerPage, patterns.size());

        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        int slotIdx = 0;
        for (int i = start; i < end; i++) {
            LinePattern pat = patterns.get(i);
            Material mat = gradientMaterial(pat.getGradient());
            String sample = pat.getSample();
            if (sample.length() > 32) sample = sample.substring(0, 32) + "...";
            setItem(slots[slotIdx++], item(mat, name(sample, CatppuccinTheme.TEXT),
                    lore("Gradient: " + pat.getGradient(), CatppuccinTheme.SURFACE2),
                    lore("Click to change", CatppuccinTheme.SUBTEXT0)));
        }

        if (page > 0) {
            setItem(45, item(Material.ARROW, name("\u2190 Prev", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        }
        if (end < patterns.size()) {
            setItem(53, item(Material.ARROW, name("Next \u2192", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        }
        setItem(49, item(Material.BARRIER, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
    }

    private void buildGradientSelect(String fingerprint) {
        editingFingerprint = fingerprint;
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Select Gradient \u25C6", CatppuccinTheme.YELLOW));
        fillBorder();

        List<String> gradients = new ArrayList<>(GradientRegistry.names());
        int slot = 10;
        for (String grad : gradients) {
            if (slot >= 44) break;
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                if (slot % 9 == 8) slot++;
            }
            Material mat = gradientMaterial(grad);
            setItem(slot++, item(mat, name(grad, CatppuccinTheme.TEXT),
                    lore("Click to assign", CatppuccinTheme.SUBTEXT0)));
        }

        setItem(49, item(Material.ARROW, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        if (editingFingerprint != null) {
            if (slot == 49) {
                editingFingerprint = null;
                buildBrowse();
                player.openInventory(inventory);
                return;
            }
            List<String> gradients = new ArrayList<>(GradientRegistry.names());
            int s = 10;
            for (String grad : gradients) {
                if (s >= 44) break;
                if (s % 9 == 0 || s % 9 == 8) {
                    s++;
                    if (s % 9 == 8) s++;
                }
                if (s == slot) {
                    LinePattern pattern = registry.getPattern(editingFingerprint);
                    if (pattern != null) {
                        registry.setGradient(pattern.getSample(), grad);
                        player.sendMessage(Component.text("Line gradient set to " + grad, CatppuccinTheme.GREEN));
                    }
                    editingFingerprint = null;
                    buildBrowse();
                    player.openInventory(inventory);
                    return;
                }
                s++;
            }
            return;
        }

        if (slot == 49) {
            page = 0;
            backAction.run();
            return;
        }
        if (slot == 45 && page > 0) {
            page--;
            buildBrowse();
            player.openInventory(inventory);
            return;
        }
        if (slot == 53) {
            page++;
            buildBrowse();
            player.openInventory(inventory);
            return;
        }

        List<LinePattern> patterns = getPatterns();
        int itemsPerPage = 28;
        int start = page * itemsPerPage;
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
        for (int i = 0; i < slots.length && (start + i) < patterns.size(); i++) {
            if (slots[i] == slot) {
                buildGradientSelect(patterns.get(start + i).getFingerprint());
                return;
            }
        }
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
