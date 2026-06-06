package com.cadist.style.util;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.Set;

public final class ConfigUpdater {

    private ConfigUpdater() {}

    public static void update(JavaPlugin plugin) {
        FileConfiguration disk = plugin.getConfig();
        FileConfiguration defaults = loadDefaults(plugin);
        if (defaults == null) return;

        boolean changed = mergeSection(disk, defaults, "");
        if (changed) {
            plugin.saveConfig();
            plugin.getLogger().info("Config updated with new default values.");
        }
    }

    private static FileConfiguration loadDefaults(JavaPlugin plugin) {
        try (InputStream in = plugin.getResource("config.yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load default config for update: " + e.getMessage());
            return null;
        }
    }

    private static boolean mergeSection(ConfigurationSection target, ConfigurationSection source, String path) {
        boolean changed = false;
        Set<String> keys = source.getKeys(false);

        for (String key : keys) {
            if (source.isConfigurationSection(key)) {
                ConfigurationSection sourceSection = source.getConfigurationSection(key);
                if (sourceSection == null) continue;

                if (!target.isConfigurationSection(key)) {
                    target.createSection(key);
                    changed = true;
                }
                ConfigurationSection targetSection = target.getConfigurationSection(key);
                if (targetSection != null) {
                    changed |= mergeSection(targetSection, sourceSection, path.isEmpty() ? key : path + "." + key);
                }
            } else {
                if (!target.contains(key, true)) {
                    target.set(key, source.get(key));
                    changed = true;
                }
            }
        }
        return changed;
    }
}
