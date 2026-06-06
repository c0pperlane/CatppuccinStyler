package com.cadist.style.config;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.util.TextFingerprint;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessagePatternRegistry {

    private final CatppuccinStyler plugin;
    private final Map<String, MessagePattern> patterns = new ConcurrentHashMap<>();

    public MessagePatternRegistry(CatppuccinStyler plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        patterns.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("message-patterns");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection patSection = section.getConfigurationSection(key);
            if (patSection == null) continue;

            String fingerprint = patSection.getString("fingerprint", "");
            if (fingerprint.isEmpty()) continue;

            String gradient = patSection.getString("gradient", "twilight");
            String pluginName = patSection.getString("plugin", "");
            String sample = patSection.getString("sample", "");

            patterns.put(fingerprint, new MessagePattern(fingerprint, gradient, pluginName, sample));
        }
    }

    public void save() {
        plugin.getConfig().set("message-patterns", null);
        for (MessagePattern pattern : patterns.values()) {
            String safeKey = hashKey(pattern.getFingerprint());
            String path = "message-patterns." + safeKey;
            plugin.getConfig().set(path + ".fingerprint", pattern.getFingerprint());
            plugin.getConfig().set(path + ".gradient", pattern.getGradient());
            plugin.getConfig().set(path + ".plugin", pattern.getPluginName());
            plugin.getConfig().set(path + ".sample", pattern.getOriginalSample());
        }
        plugin.saveConfig();
    }

    public MessagePattern getPattern(String fingerprint) {
        return patterns.get(fingerprint);
    }

    public MessagePattern findSimilar(String fingerprint) {
        MessagePattern exact = patterns.get(fingerprint);
        if (exact != null) return exact;

        for (MessagePattern candidate : patterns.values()) {
            if (TextFingerprint.isSimilar(fingerprint, candidate.getFingerprint())) {
                return candidate;
            }
        }
        return null;
    }

    public MessagePattern register(String fingerprint, String gradient, String pluginName, String originalSample) {
        MessagePattern pattern = new MessagePattern(fingerprint, gradient, pluginName, originalSample);
        patterns.put(fingerprint, pattern);
        save();
        return pattern;
    }

    public void setGradient(String fingerprint, String gradient) {
        MessagePattern pattern = patterns.get(fingerprint);
        if (pattern != null) {
            pattern.setGradient(gradient);
            save();
        }
    }

    public Collection<MessagePattern> allPatterns() {
        return Collections.unmodifiableCollection(patterns.values());
    }

    private String hashKey(String fingerprint) {
        return Integer.toHexString(fingerprint.hashCode());
    }
}
