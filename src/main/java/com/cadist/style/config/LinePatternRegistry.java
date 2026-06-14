package com.cadist.style.config;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.util.TextFingerprint;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinePatternRegistry {

    private final CatppuccinStyler plugin;
    private final String sectionName;
    private final Map<String, LinePattern> patterns = new ConcurrentHashMap<>();
    private final AtomicBoolean savePending = new AtomicBoolean(false);

    public LinePatternRegistry(CatppuccinStyler plugin, String sectionName) {
        this.plugin = plugin;
        this.sectionName = sectionName;
        load();
    }

    /**
     * Returns the remembered gradient for a line, creating a random one if necessary.
     * Updates the sample and last-seen timestamp.
     */
    public String getGradient(String plainText) {
        if (plainText == null) return null;
        String fingerprint = TextFingerprint.create(plainText);
        if (fingerprint.isEmpty()) return null;

        LinePattern pattern = patterns.get(fingerprint);
        if (pattern == null) {
            pattern = new LinePattern(fingerprint, pickRandomGradient(), plainText);
            patterns.put(fingerprint, pattern);
            scheduleSave();
        }
        pattern.setSample(plainText);
        pattern.setLastSeen(System.currentTimeMillis());
        return pattern.getGradient();
    }

    public void setGradient(String plainText, String gradient) {
        if (plainText == null) return;
        String fingerprint = TextFingerprint.create(plainText);
        if (fingerprint.isEmpty()) return;

        LinePattern pattern = patterns.get(fingerprint);
        if (pattern == null) {
            pattern = new LinePattern(fingerprint, gradient, plainText);
            patterns.put(fingerprint, pattern);
        } else {
            pattern.setGradient(gradient);
            pattern.setSample(plainText);
        }
        scheduleSave();
    }

    public LinePattern getPattern(String fingerprint) {
        return patterns.get(fingerprint);
    }

    public Collection<LinePattern> allPatterns() {
        return Collections.unmodifiableCollection(new ArrayList<>(patterns.values()));
    }

    /**
     * Removes every stored pattern whose fingerprint is not in the provided keep-set.
     */
    public void prune(Set<String> keepFingerprints) {
        boolean changed = patterns.keySet().retainAll(keepFingerprints);
        if (changed) {
            scheduleSave();
        }
    }

    /**
     * Removes patterns that have not been seen for longer than maxAgeMs.
     */
    public void pruneOld(long maxAgeMs) {
        long cutoff = System.currentTimeMillis() - maxAgeMs;
        boolean changed = patterns.values().removeIf(p -> p.getLastSeen() < cutoff);
        if (changed) {
            scheduleSave();
        }
    }

    public void load() {
        patterns.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(sectionName);
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection patSection = section.getConfigurationSection(key);
            if (patSection == null) continue;

            String fingerprint = patSection.getString("fingerprint", "");
            if (fingerprint.isEmpty()) continue;

            String gradient = patSection.getString("gradient", "twilight");
            String sample = patSection.getString("sample", "");
            long lastSeen = patSection.getLong("last-seen", System.currentTimeMillis());

            LinePattern pattern = new LinePattern(fingerprint, gradient, sample);
            pattern.setLastSeen(lastSeen);
            patterns.put(fingerprint, pattern);
        }
    }

    private void saveSync() {
        plugin.getConfig().set(sectionName, null);
        for (LinePattern pattern : patterns.values()) {
            String safeKey = hashKey(pattern.getFingerprint());
            String path = sectionName + "." + safeKey;
            plugin.getConfig().set(path + ".fingerprint", pattern.getFingerprint());
            plugin.getConfig().set(path + ".gradient", pattern.getGradient());
            plugin.getConfig().set(path + ".sample", pattern.getSample());
            plugin.getConfig().set(path + ".last-seen", pattern.getLastSeen());
        }
        plugin.saveConfig();
    }

    private void scheduleSave() {
        if (savePending.compareAndSet(false, true)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                savePending.set(false);
                saveSync();
            });
        }
    }

    private String pickRandomGradient() {
        List<String> gradients = new ArrayList<>(GradientRegistry.names());
        if (gradients.isEmpty()) return "twilight";
        return gradients.get(ThreadLocalRandom.current().nextInt(gradients.size()));
    }

    private String hashKey(String fingerprint) {
        return Integer.toHexString(fingerprint.hashCode());
    }
}
