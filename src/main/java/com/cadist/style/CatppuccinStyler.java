package com.cadist.style;

import com.cadist.style.command.CatppuccinCommand;
import com.cadist.style.config.MessagePatternRegistry;
import com.cadist.style.config.StyleConfig;
import com.cadist.style.gui.GuiListener;
import com.cadist.style.listener.StyleListener;
import org.bukkit.plugin.java.JavaPlugin;

public class CatppuccinStyler extends JavaPlugin {

    private static CatppuccinStyler instance;
    private StyleConfig styleConfig;
    private MessagePatternRegistry patternRegistry;
    private com.cadist.style.tab.TabIntegration tabIntegration;

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
            loadIntegration("com.cadist.style.packet.PacketEventsIntegration", "PacketEvents");
        } else {
            getLogger().warning("PacketEvents not found — system chat and TAB header/footer styling are disabled.");
            getLogger().warning("Install PacketEvents to enable those features: https://github.com/retrooper/packetevents");
        }

        if (getServer().getPluginManager().getPlugin("TAB") != null) {
            loadTabIntegration();
        } else {
            getLogger().info("TAB not found — Catppuccin TAB styling disabled.");
        }

        getLogger().info("Catppuccin Styler enabled — styling " + styleConfig.getEventDefaults().size() + " event types.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Catppuccin Styler disabled.");
    }

    private void loadIntegration(String className, String label) {
        try {
            Class<?> clazz = Class.forName(className);
            clazz.getConstructor(CatppuccinStyler.class).newInstance(this);
            getLogger().info(label + " integration enabled.");
        } catch (Exception e) {
            getLogger().warning("Failed to load " + label + " integration: " + e.getMessage());
        }
    }

    private void loadTabIntegration() {
        try {
            Class<?> clazz = Class.forName("com.cadist.style.tab.NezamyTabIntegration");
            tabIntegration = (com.cadist.style.tab.TabIntegration) clazz.getConstructor(CatppuccinStyler.class).newInstance(this);
            getLogger().info("TAB integration enabled — Catppuccin TAB styling available.");
        } catch (Exception e) {
            getLogger().warning("Failed to load TAB integration: " + e.getMessage());
        }
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

    public com.cadist.style.tab.TabIntegration getTabIntegration() {
        return tabIntegration;
    }
}
