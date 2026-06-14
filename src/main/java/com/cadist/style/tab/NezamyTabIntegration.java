package com.cadist.style.tab;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.LinePatternRegistry;
import me.neznamy.tab.api.TabAPI;
import me.neznamy.tab.api.TabPlayer;
import me.neznamy.tab.api.event.plugin.TabLoadEvent;
import me.neznamy.tab.api.scoreboard.Scoreboard;
import me.neznamy.tab.api.scoreboard.ScoreboardManager;
import org.bukkit.Bukkit;

import java.io.File;
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

        TabAPI.getInstance().getEventBus().register(TabLoadEvent.class, event -> {
            // Fires on /tab reload — force every player to receive a fresh, fully-styled scoreboard
            // so the "half-styled after reload" issue is resolved automatically.
            Bukkit.getScheduler().runTask(plugin, () -> {
                refreshKnownLines();
                resendScoreboards();
            });
        });

        Bukkit.getScheduler().runTask(plugin, this::refreshKnownLines);
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshKnownLines, 600L, 600L);
    }

    private long pruneTtl() {
        return TimeUnit.MINUTES.toMillis(30);
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
        try {
            // Header/footer lines are styled from outgoing packets, where placeholders are
            // already resolved. We prune by last-seen TTL rather than config so placeholder
            // values (numbers, times) do not cause lines to be forgotten on every TAB reload.
            tabLineRegistry.pruneOld(pruneTtl());
            scoreboardLineRegistry.pruneOld(pruneTtl());
        } catch (Exception e) {
            plugin.getLogger().warning("Could not prune line registries: " + e.getMessage());
        }
    }

    /**
     * Resets and re-shows every player's active scoreboard so TAB resends all of its
     * scoreboard packets. Our PacketEvents listener then re-styles the fresh packets,
     * which fixes the "half the scoreboard is un-styled after /tab reload" problem
     * without requiring players to rejoin.
     */
    private void resendScoreboards() {
        try {
            ScoreboardManager sm = TabAPI.getInstance().getScoreboardManager();
            if (sm == null) return;

            for (TabPlayer tabPlayer : TabAPI.getInstance().getOnlinePlayers()) {
                try {
                    if (!sm.hasScoreboardVisible(tabPlayer)) continue;
                    Scoreboard active = sm.getActiveScoreboard(tabPlayer);
                    if (active == null) continue;

                    // Reset → re-show: this is a clean rebuild, all packets come through
                    // PacketEvents again and get re-styled.
                    sm.resetScoreboard(tabPlayer);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        try {
                            sm.showScoreboard(tabPlayer, active);
                        } catch (Exception inner) {
                            plugin.getLogger().fine("Failed to re-show scoreboard: " + inner.getMessage());
                        }
                    }, 2L);
                } catch (Exception perPlayer) {
                    plugin.getLogger().fine("Scoreboard resend failed for a player: " + perPlayer.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not resend scoreboards on TAB reload: " + e.getMessage());
        }
    }
}
