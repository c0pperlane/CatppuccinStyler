package com.cadist.style;

import com.cadist.style.command.CatppuccinCommand;
import com.cadist.style.config.MessagePatternRegistry;
import com.cadist.style.config.StyleConfig;
import com.cadist.style.gui.GuiListener;
import com.cadist.style.listener.PacketEventsListener;
import com.cadist.style.listener.StyleListener;
import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class CatppuccinStyler extends JavaPlugin {

    private static CatppuccinStyler instance;
    private StyleConfig styleConfig;
    private MessagePatternRegistry patternRegistry;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        com.cadist.style.util.ConfigUpdater.update(this);
        this.styleConfig = new StyleConfig(this);
        this.patternRegistry = new MessagePatternRegistry(this);

        getCommand("adminstyle").setExecutor(new CatppuccinCommand(this));
        getServer().getPluginManager().registerEvents(new StyleListener(this), this);
        getServer().getPluginManager().registerEvents(new GuiListener(), this);

        if (getServer().getPluginManager().getPlugin("packetevents") != null) {
            try {
                PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(this));
                getLogger().info("PacketEvents integration enabled — intercepting system chat messages.");
            } catch (Exception e) {
                getLogger().warning("Failed to register PacketEvents listener: " + e.getMessage());
            }
        } else {
            getLogger().info("PacketEvents not found — system chat interception disabled.");
        }

        getLogger().info("Catppuccin Styler enabled — styling " + styleConfig.getEventDefaults().size() + " event types.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Catppuccin Styler disabled.");
    }

    public static CatppuccinStyler getInstance() {
        return instance;
    }

    public StyleConfig getStyleConfig() {
        return styleConfig;
    }

    public MessagePatternRegistry getPatternRegistry() {
        return patternRegistry;
    }
}
