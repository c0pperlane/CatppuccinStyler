package com.cadist.style.tab;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
import com.cadist.style.config.LinePatternRegistry;
import com.cadist.style.util.TextFingerprint;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.scoreboard.Line;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class NezamyTabIntegration implements TabIntegration {

    private final CatppuccinStyler plugin;
    private final LinePatternRegistry tabLineRegistry;
    private final LinePatternRegistry scoreboardLineRegistry;
    private final File tabConfigFile;

    public NezamyTabIntegration(CatppuccinStyler plugin) {
        this.plugin = plugin;
        this.tabLineRegistry = new LinePatternRegistry(plugin, "tab-line-patterns");
        this.scoreboardLineRegistry = new LinePatternRegistry(plugin, "scoreboard-line-patterns");
        this.tabConfigFile = new File(plugin.getDataFolder().getParentFile(), "TAB" + File.separator + "config.yml");

        TabAPI.getInstance().getEventBus().register(TabLoadEvent.class, event ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    styleRegisteredScoreboards();
                    refreshKnownLines();
                }));

        Bukkit.getScheduler().runTask(plugin, () -> {
            styleRegisteredScoreboards();
            refreshKnownLines();
        });

        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshKnownLines, 600L, 600L);
    }

    private long headerFooterPruneTtl() {
        return TimeUnit.MINUTES.toMillis(10);
    }

    @Override
    public boolean isEnabled() {
        return plugin.getStyleConfig().isCatppuccinTabEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        plugin.getStyleConfig().setCatppuccinTabEnabled(enabled);
    }

    @Override
    public boolean isTabListEnabled() {
        return plugin.getStyleConfig().isCatppuccinTabListEnabled();
    }

    @Override
    public void setTabListEnabled(boolean enabled) {
        plugin.getStyleConfig().setCatppuccinTabListEnabled(enabled);
    }

    @Override
    public boolean isScoreboardEnabled() {
        return plugin.getStyleConfig().isCatppuccinScoreboardEnabled();
    }

    @Override
    public void setScoreboardEnabled(boolean enabled) {
        plugin.getStyleConfig().setCatppuccinScoreboardEnabled(enabled);
    }

    @Override
    public LinePatternRegistry getTabLineRegistry() {
        return tabLineRegistry;
    }

    @Override
    public LinePatternRegistry getScoreboardLineRegistry() {
        return scoreboardLineRegistry;
    }

    @Override
    public void refreshKnownLines() {
        if (!tabConfigFile.exists()) return;

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(tabConfigFile);

            // Header/footer lines are styled from outgoing packets, where placeholders are
            // already resolved. We prune by last-seen TTL rather than config so placeholder
            // values (numbers, times) do not cause lines to be forgotten on every TAB reload.
            tabLineRegistry.pruneOld(headerFooterPruneTtl());

            Set<String> scoreboardKnown = ConcurrentHashMap.newKeySet();
            ConfigurationSection scoreboardSection = config.getConfigurationSection("scoreboard.scoreboards");
            if (scoreboardSection != null) {
                collectScoreboardLines(scoreboardSection, scoreboardKnown);
            }
            scoreboardLineRegistry.prune(scoreboardKnown);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not read TAB config for line pruning: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void styleRegisteredScoreboards() {
        ScoreboardManager manager = TabAPI.getInstance().getScoreboardManager();
        if (manager == null) return;

        for (Map.Entry<?, ?> entry : manager.getRegisteredScoreboards().entrySet()) {
            try {
                Scoreboard scoreboard = (Scoreboard) entry.getValue();
                styleScoreboard(scoreboard);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not style scoreboard " + entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    private void styleScoreboard(Scoreboard scoreboard) {
        String title = scoreboard.getTitle();
        String styledTitle = styleScoreboardText(title, false);
        if (styledTitle != null && !styledTitle.equals(title)) {
            scoreboard.setTitle(styledTitle);
        }

        for (Line line : scoreboard.getLines()) {
            String raw = line.getText();
            String styled = styleScoreboardText(raw, true);
            if (styled != null && !styled.equals(raw)) {
                line.setText(styled);
            }
        }
    }

    private String styleScoreboardText(String raw, boolean isLine) {
        if (raw == null || raw.isEmpty()) return raw;

        String[] parts = raw.split("\\|\\|", -1);
        String textPart = parts[0];
        if (textPart.isEmpty()) return raw;

        String stripped = textPart.replaceAll("(?i)[&§][0-9a-fk-or]", "").trim();
        if (stripped.isEmpty()) return raw;
        if (stripped.toLowerCase().contains("<gradient")) return raw; // already styled

        String gradient = scoreboardLineRegistry.getGradient(stripped);
        if (gradient == null) return raw;

        GradientRegistry.Gradient g = GradientRegistry.get(gradient);
        if (g == null) return raw;

        String gradientSpec = g.toString().toLowerCase();
        parts[0] = "<gradient:" + gradientSpec + ">" + stripped + "</gradient>";
        return String.join("||", parts);
    }

    private void collectHeaderFooterLines(ConfigurationSection section, Set<String> out) {
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                collectHeaderFooterLines(section.getConfigurationSection(key), out);
            } else if ((key.equalsIgnoreCase("header") || key.equalsIgnoreCase("footer")) && section.isList(key)) {
                for (Object obj : section.getList(key)) {
                    if (obj instanceof String line) {
                        out.add(fingerprintOf(line));
                    }
                }
            }
        }
    }

    private void collectScoreboardLines(ConfigurationSection section, Set<String> out) {
        for (String key : section.getKeys(false)) {
            if (!section.isConfigurationSection(key)) continue;
            ConfigurationSection scoreboard = section.getConfigurationSection(key);
            if (scoreboard == null) continue;

            String title = scoreboard.getString("title", "");
            out.add(fingerprintOf(title));

            if (scoreboard.isList("lines")) {
                for (Object obj : scoreboard.getList("lines")) {
                    if (obj instanceof String line) {
                        out.add(fingerprintOfScoreboardLine(line));
                    }
                }
            }
        }
    }

    private String fingerprintOfScoreboardLine(String rawLine) {
        if (rawLine == null) return "";
        String textPart = rawLine.split("\\|\\|", -1)[0];
        return fingerprintOf(textPart);
    }

    private String fingerprintOf(String rawLine) {
        if (rawLine == null) return "";
        String stripped = rawLine.replaceAll("(?i)[&§][0-9a-fk-or]", "").trim();
        return TextFingerprint.create(stripped);
    }
}
