package com.cadist.style.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.InventoryHolder;

public class GuiListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder instanceof CatppuccinGui gui) {
            gui.handleClick(event);
        } else if (holder instanceof PluginSelectorGui gui) {
            gui.handleClick(event);
        } else if (holder instanceof PatternBrowserGui gui) {
            gui.handleClick(event);
        } else if (holder instanceof CatppuccinTabGui gui) {
            gui.handleClick(event);
        } else if (holder instanceof LineBrowserGui gui) {
            gui.handleClick(event);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        if (PatternBrowserGui.hasPendingSearch(event.getPlayer())) {
            String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
            event.setCancelled(true);
            PatternBrowserGui.handleSearchPrompt(event.getPlayer(), input);
        }
    }

    /** Clean up pending search callbacks when a player disconnects. */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PatternBrowserGui.clearPendingSearch(event.getPlayer());
    }
}
