package com.cadist.style.gui;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
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

public class CatppuccinGui implements InventoryHolder {

    private final CatppuccinStyler plugin;
    private final Player player;
    private Inventory inventory;
    private GuiMode mode;
    private String editingTarget = null;
    private EditType editingType = EditType.NONE;

    // NOTE: PENDING_PROMPTS was removed — no GUI action ever populated it (dead code).

    private enum GuiMode { MAIN, EVENTS, PLUGINS, EXCLUSIONS, GRADIENT_SELECT }
    private enum EditType { NONE, EVENT, PLUGIN }

    public CatppuccinGui(CatppuccinStyler plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.mode = GuiMode.MAIN;
    }

    public void open() {
        openMain();
    }

    public void openMain() {
        mode = GuiMode.MAIN;
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Catppuccin Styler \u25C6", CatppuccinTheme.LAVENDER));
        fillBorder();

        setItem(10, item(Material.EMERALD, name("Events", CatppuccinTheme.GREEN),
                lore("Assign gradients to server events", CatppuccinTheme.SUBTEXT0)));
        setItem(12, item(Material.DIAMOND, name("Plugins", CatppuccinTheme.SKY),
                lore("Assign gradients to plugins", CatppuccinTheme.SUBTEXT0)));
        setItem(14, item(Material.BARRIER, name("Exclusions", CatppuccinTheme.RED),
                lore("Exclude plugins from styling", CatppuccinTheme.SUBTEXT0)));
        setItem(16, item(Material.CLOCK, name("Reload Config", CatppuccinTheme.YELLOW),
                lore("Reload configuration from disk", CatppuccinTheme.SUBTEXT0)));
        setItem(30, item(Material.MAP, name("Patterns", CatppuccinTheme.PEACH),
                lore("Browse and edit message patterns", CatppuccinTheme.SUBTEXT0)));
        setItem(31, item(Material.BOOK, name("Info", CatppuccinTheme.LAVENDER),
                lore("Catppuccin Styler v1.0.0", CatppuccinTheme.SUBTEXT0),
                lore("30 gradients available", CatppuccinTheme.SURFACE2)));

        player.openInventory(inventory);
    }

    private void openEvents() {
        mode = GuiMode.EVENTS;
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Event Gradients \u25C6", CatppuccinTheme.MAUVE));
        fillBorder();

        Map<String, String> events = plugin.getStyleConfig().getEventDefaults();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (Map.Entry<String, String> entry : events.entrySet()) {
            if (idx >= slots.length) break;
            String event = entry.getKey();
            String grad = entry.getValue();
            Material mat = gradientMaterial(grad);
            setItem(slots[idx++], item(mat, name(capitalize(event) + ": " + grad, CatppuccinTheme.TEXT),
                    lore("Click to change gradient", CatppuccinTheme.SUBTEXT0)));
        }

        setItem(49, item(Material.ARROW, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        player.openInventory(inventory);
    }

    private void openPlugins() {
        mode = GuiMode.PLUGINS;
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Plugin Gradients \u25C6", CatppuccinTheme.MAUVE));
        fillBorder();

        Map<String, String> plugins = plugin.getStyleConfig().getPluginDefaults();
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
        int idx = 0;
        for (Map.Entry<String, String> entry : plugins.entrySet()) {
            if (idx >= slots.length) break;
            String pname = entry.getKey();
            String grad = entry.getValue();
            Material mat = gradientMaterial(grad);
            setItem(slots[idx++], item(mat, name(pname + ": " + grad, CatppuccinTheme.TEXT),
                    lore("Click to change gradient", CatppuccinTheme.SUBTEXT0)));
        }

        setItem(49, item(Material.ARROW, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        player.openInventory(inventory);
    }

    public void openExclusions() {
        mode = GuiMode.EXCLUSIONS;
        inventory = Bukkit.createInventory(this, 54, Component.text("\u25C6 Excluded Plugins \u25C6", CatppuccinTheme.RED));
        fillBorder();

        Set<String> excluded = plugin.getStyleConfig().getExcludedPlugins();
        int slot = 10;
        for (String pname : excluded) {
            if (slot >= 44) break;
            if (slot % 9 == 0 || slot % 9 == 8) {
                slot++;
                if (slot % 9 == 8) slot++;
            }
            setItem(slot++, item(Material.BARRIER, name(pname, CatppuccinTheme.RED),
                    lore("Click to remove exclusion", CatppuccinTheme.SUBTEXT0)));
        }

        setItem(43, item(Material.LIME_WOOL, name("+ Add Plugin", CatppuccinTheme.GREEN),
                lore("Click to exclude a new plugin", CatppuccinTheme.SUBTEXT0)));
        setItem(49, item(Material.ARROW, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        player.openInventory(inventory);
    }

    private void openGradientSelect(String target, EditType type) {
        mode = GuiMode.GRADIENT_SELECT;
        editingTarget = target;
        editingType = type;
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
                    lore("Click to select", CatppuccinTheme.SUBTEXT0)));
        }

        setItem(49, item(Material.ARROW, name("\u2190 Back", CatppuccinTheme.SURFACE2), Collections.emptyList()));
        player.openInventory(inventory);
    }

    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getSlot();

        switch (mode) {
            case MAIN -> {
                switch (slot) {
                    case 10 -> openEvents();
                    case 12 -> openPlugins();
                    case 14 -> openExclusions();
                    case 16 -> {
                        plugin.getStyleConfig().reload();
                        plugin.getPatternRegistry().load();
                        player.sendMessage(Component.text("Config reloaded!", CatppuccinTheme.GREEN));
                        player.closeInventory();
                    }
                    case 30 -> new PatternBrowserGui(plugin, player).open();
                }
            }
            case EVENTS -> {
                if (slot == 49) { openMain(); return; }
                Map<String, String> events = new LinkedHashMap<>(plugin.getStyleConfig().getEventDefaults());
                List<String> keys = new ArrayList<>(events.keySet());
                int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
                for (int i = 0; i < slots.length && i < keys.size(); i++) {
                    if (slots[i] == slot) {
                        openGradientSelect(keys.get(i), EditType.EVENT);
                        return;
                    }
                }
            }
            case PLUGINS -> {
                if (slot == 49) { openMain(); return; }
                Map<String, String> plugins = new LinkedHashMap<>(plugin.getStyleConfig().getPluginDefaults());
                List<String> keys = new ArrayList<>(plugins.keySet());
                int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25};
                for (int i = 0; i < slots.length && i < keys.size(); i++) {
                    if (slots[i] == slot) {
                        openGradientSelect(keys.get(i), EditType.PLUGIN);
                        return;
                    }
                }
            }
            case EXCLUSIONS -> {
                if (slot == 49) { openMain(); return; }
                if (slot == 43) {
                    player.closeInventory();
                    new PluginSelectorGui(plugin, player).open();
                    return;
                }
                Set<String> excluded = new LinkedHashSet<>(plugin.getStyleConfig().getExcludedPlugins());
                int s = 10;
                for (String pname : excluded) {
                    if (s >= 44) break;
                    if (s % 9 == 0 || s % 9 == 8) {
                        s++;
                        if (s % 9 == 8) s++;
                    }
                    if (s == slot) {
                        plugin.getStyleConfig().removeExcludedPlugin(pname);
                        player.sendMessage(Component.text("Removed exclusion: " + pname, CatppuccinTheme.GREEN));
                        openExclusions();
                        return;
                    }
                    s++;
                }
            }
            case GRADIENT_SELECT -> {
                if (slot == 49) {
                    if (editingType == EditType.PLUGIN) {
                        openPlugins();
                    } else {
                        openEvents();
                    }
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
                    if (s == slot && editingTarget != null) {
                        if (editingType == EditType.PLUGIN) {
                            plugin.getStyleConfig().setPluginGradient(editingTarget, grad);
                            player.sendMessage(Component.text("Set plugin " + editingTarget + " to " + grad, CatppuccinTheme.GREEN));
                            openPlugins();
                        } else {
                            plugin.getStyleConfig().setEventGradient(editingTarget, grad);
                            player.sendMessage(Component.text("Set " + editingTarget + " to " + grad, CatppuccinTheme.GREEN));
                            openEvents();
                        }
                        return;
                    }
                    s++;
                }
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

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
