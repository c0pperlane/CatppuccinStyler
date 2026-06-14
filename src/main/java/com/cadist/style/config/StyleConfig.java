package com.cadist.style.config;

import com.cadist.style.CatppuccinStyler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StyleConfig {
    private final CatppuccinStyler plugin;
    private FileConfiguration config;
    private final Map<String, String> eventDefaults = new ConcurrentHashMap<>();
    private final Map<String, String> pluginDefaults = new ConcurrentHashMap<>();
    private final Set<String> excludedPlugins = ConcurrentHashMap.newKeySet();

    public StyleConfig(CatppuccinStyler plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        eventDefaults.clear();
        pluginDefaults.clear();
        excludedPlugins.clear();

        ConfigurationSection events = config.getConfigurationSection("event-defaults");
        if (events != null) {
            for (String key : events.getKeys(false)) {
                eventDefaults.put(key, events.getString(key, "twilight"));
            }
        }
        // Ensure defaults exist
        eventDefaults.putIfAbsent("join", "twilight");
        eventDefaults.putIfAbsent("quit", "twilight");
        eventDefaults.putIfAbsent("death", "rose");
        eventDefaults.putIfAbsent("advancement", "meadow");
        eventDefaults.putIfAbsent("broadcast", "flame");
        eventDefaults.putIfAbsent("chat", "twilight");

        ConfigurationSection plugins = config.getConfigurationSection("plugin-defaults");
        if (plugins != null) {
            for (String key : plugins.getKeys(false)) {
                pluginDefaults.put(key, plugins.getString(key, "twilight"));
            }
        }

        List<String> excl = config.getStringList("excluded-plugins");
        if (excl != null) {
            for (String s : excl) {
                excludedPlugins.add(s.toLowerCase());
            }
        }
    }

    public String getEventGradient(String event) {
        return eventDefaults.getOrDefault(event.toLowerCase(), "twilight");
    }

    public void setEventGradient(String event, String gradient) {
        eventDefaults.put(event.toLowerCase(), gradient);
        config.set("event-defaults." + event.toLowerCase(), gradient);
        plugin.saveConfig();
    }

    public Map<String, String> getEventDefaults() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(eventDefaults));
    }

    public Map<String, String> getPluginDefaults() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(pluginDefaults));
    }

    public Set<String> getExcludedPlugins() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(excludedPlugins));
    }

    public String getPluginGradient(String pluginName) {
        return pluginDefaults.getOrDefault(pluginName.toLowerCase(), "twilight");
    }

    public void setPluginGradient(String pluginName, String gradient) {
        pluginDefaults.put(pluginName.toLowerCase(), gradient);
        config.set("plugin-defaults." + pluginName.toLowerCase(), gradient);
        plugin.saveConfig();
    }

    public boolean isExcludedPlugin(String pluginName) {
        return excludedPlugins.contains(pluginName.toLowerCase());
    }

    public void addExcludedPlugin(String pluginName) {
        excludedPlugins.add(pluginName.toLowerCase());
        config.set("excluded-plugins", new ArrayList<>(excludedPlugins));
        plugin.saveConfig();
    }

    public void removeExcludedPlugin(String pluginName) {
        excludedPlugins.remove(pluginName.toLowerCase());
        config.set("excluded-plugins", new ArrayList<>(excludedPlugins));
        plugin.saveConfig();
    }

    public boolean isUnicodeDecoration() {
        return config.getBoolean("unicode-decoration", true);
    }

    public boolean isCatppuccinTabEnabled() {
        return config.getBoolean("catppuccin-tab.enabled", false);
    }

    public void setCatppuccinTabEnabled(boolean enabled) {
        config.set("catppuccin-tab.enabled", enabled);
        plugin.saveConfig();
    }

    public boolean isCatppuccinTabListEnabled() {
        return config.getBoolean("catppuccin-tab.tab-list", true);
    }

    public void setCatppuccinTabListEnabled(boolean enabled) {
        config.set("catppuccin-tab.tab-list", enabled);
        plugin.saveConfig();
    }

    public boolean isCatppuccinScoreboardEnabled() {
        return config.getBoolean("catppuccin-tab.scoreboard", true);
    }

    public void setCatppuccinScoreboardEnabled(boolean enabled) {
        config.set("catppuccin-tab.scoreboard", enabled);
        plugin.saveConfig();
    }
}
