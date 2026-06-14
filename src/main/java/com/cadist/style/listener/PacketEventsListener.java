package com.cadist.style.listener;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
import com.cadist.style.config.LinePatternRegistry;
import com.cadist.style.config.MessagePattern;
import com.cadist.style.config.MessagePatternRegistry;
import com.cadist.style.tab.TabIntegration;
import com.cadist.style.util.TextFingerprint;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerListHeaderAndFooter;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.cadist.style.util.GradientStyler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class PacketEventsListener extends PacketListenerAbstract {

    private final CatppuccinStyler plugin;

    /**
     * Snapshot of loaded plugin names, refreshed on the main thread.
     * Using CopyOnWriteArrayList so Netty threads can read without locks.
     */
    private final CopyOnWriteArrayList<String> cachedPluginNames = new CopyOnWriteArrayList<>();

    public PacketEventsListener(CatppuccinStyler plugin) {
        super(PacketListenerPriority.HIGHEST);
        this.plugin = plugin;
        // Populate the initial snapshot on the main thread
        refreshPluginNames();
    }

    /** Call this from the main thread to refresh the plugin name snapshot. */
    public void refreshPluginNames() {
        List<String> names = new ArrayList<>();
        for (org.bukkit.plugin.Plugin p : Bukkit.getPluginManager().getPlugins()) {
            names.add(p.getName());
        }
        cachedPluginNames.clear();
        cachedPluginNames.addAll(names);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            if (plugin.getStyleConfig().isCatppuccinChatEnabled()) {
                handleSystemChat(event);
            }
            return;
        }

        TabIntegration tab = plugin.getTabIntegration();
        if (tab == null || !tab.isEnabled()) return;

        if (tab.isTabListEnabled() && event.getPacketType() == PacketType.Play.Server.PLAYER_LIST_HEADER_AND_FOOTER) {
            handleHeaderFooter(event, tab);
            return;
        }

        if (tab.isScoreboardEnabled() && event.getPacketType() == PacketType.Play.Server.SCOREBOARD_OBJECTIVE) {
            handleScoreboardObjective(event, tab.getScoreboardLineRegistry());
            return;
        }

        if (tab.isScoreboardEnabled() && event.getPacketType() == PacketType.Play.Server.TEAMS) {
            handleScoreboardTeam(event, tab.getScoreboardLineRegistry());
        }
    }

    private void handleSystemChat(PacketSendEvent event) {
        WrapperPlayServerSystemChatMessage packet = new WrapperPlayServerSystemChatMessage(event);
        Component message = packet.getMessage();
        if (message == null) return;

        String text = PlainTextComponentSerializer.plainText().serialize(message);
        if (text.isEmpty()) return;

        // Skip already-styled messages (4+ distinct colors = existing gradient)
        if (isAlreadyStyled(message)) {
            return;
        }

        // If this is player chat and the player has a /chatcolor in CadistClaim, skip.
        // hasCadistChatColor uses Bukkit API, so dispatch the check and any state
        // mutations to the main thread; do the packet styling here using a fast path.
        UUID playerUuid = event.getUser().getUUID();
        String pluginName = detectPluginFromContent(text); // uses cached list — safe off-thread

        // Check exclusions (reads ConcurrentHashSet — safe off-thread)
        if (pluginName != null && plugin.getStyleConfig().isExcludedPlugin(pluginName)) {
            return;
        }

        String fingerprint = TextFingerprint.create(text);
        MessagePatternRegistry registry = plugin.getPatternRegistry();

        MessagePattern pattern = registry.getPattern(fingerprint);
        if (pattern == null) {
            pattern = registry.findSimilar(fingerprint);
        }

        final String gradient;
        if (pattern != null) {
            gradient = pattern.getGradient();
        } else {
            gradient = pickRandomGradient();
            // Register the new pattern on the main thread (saveConfig is synchronous I/O)
            final String finalPluginName = pluginName != null ? pluginName : "";
            final String finalText = text;
            final String fp = fingerprint;
            final String chosenGradient = gradient;
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Perform the chat-color check on the main thread before persisting
                if (!hasCadistChatColor(playerUuid, finalText)) {
                    registry.register(fp, chosenGradient, finalPluginName, finalText);
                }
            });
        }

        Component styled = styleComponent(message, gradient);
        packet.setMessage(styled);
    }

    private void handleHeaderFooter(PacketSendEvent event, TabIntegration tab) {
        WrapperPlayServerPlayerListHeaderAndFooter packet = new WrapperPlayServerPlayerListHeaderAndFooter(event);

        Component header = packet.getHeader();
        if (header != null) {
            Component styled = styleHeaderFooter(header, tab.getTabLineRegistry());
            if (styled != null) packet.setHeader(styled);
        }

        Component footer = packet.getFooter();
        if (footer != null) {
            Component styled = styleHeaderFooter(footer, tab.getTabLineRegistry());
            if (styled != null) packet.setFooter(styled);
        }
    }

    private Component styleHeaderFooter(Component original, LinePatternRegistry registry) {
        if (isAlreadyStyled(original)) return null;

        String legacy = LegacyComponentSerializer.legacyAmpersand().serialize(original);
        if (legacy.isEmpty()) return null;

        String[] lines = legacy.split("\n", -1);
        Component result = Component.empty();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (!line.isEmpty()) {
                String gradient = registry.getGradient(line);
                if (gradient != null) {
                    result = result.append(GradientStyler.styleLegacyLine(line, gradient));
                } else {
                    result = result.append(Component.text(line));
                }
            }
            if (i < lines.length - 1) {
                result = result.append(Component.text("\n"));
            }
        }
        return result;
    }

    private void handleScoreboardObjective(PacketSendEvent event, LinePatternRegistry registry) {
        WrapperPlayServerScoreboardObjective packet = new WrapperPlayServerScoreboardObjective(event);
        String name = packet.getName();
        if (name == null || !name.toUpperCase().startsWith("TAB")) return;

        Component displayName = packet.getDisplayName();
        if (displayName == null) return;

        Component styled = styleScoreboardComponent(displayName, registry);
        if (styled != null) {
            packet.setDisplayName(styled);
        }
    }

    private void handleScoreboardTeam(PacketSendEvent event, LinePatternRegistry registry) {
        WrapperPlayServerTeams packet = new WrapperPlayServerTeams(event);
        String teamName = packet.getTeamName();
        if (teamName == null || !teamName.startsWith("TAB-Sidebar")) return;

        Optional<WrapperPlayServerTeams.ScoreBoardTeamInfo> infoOpt = packet.getTeamInfo();
        if (infoOpt.isEmpty()) return;

        WrapperPlayServerTeams.ScoreBoardTeamInfo info = infoOpt.get();
        boolean changed = false;

        Component prefix = info.getPrefix();
        if (prefix != null) {
            Component styled = styleScoreboardComponent(prefix, registry);
            if (styled != null) {
                info.setPrefix(styled);
                changed = true;
            }
        }

        Component suffix = info.getSuffix();
        if (suffix != null) {
            Component styled = styleScoreboardComponent(suffix, registry);
            if (styled != null) {
                info.setSuffix(styled);
                changed = true;
            }
        }

        Component displayName = info.getDisplayName();
        if (displayName != null) {
            Component styled = styleScoreboardComponent(displayName, registry);
            if (styled != null) {
                info.setDisplayName(styled);
                changed = true;
            }
        }

        if (changed) {
            packet.setTeamInfo(info);
        }
    }

    private Component styleScoreboardComponent(Component original, LinePatternRegistry registry) {
        if (isAlreadyStyled(original)) return null;

        String plain = PlainTextComponentSerializer.plainText().serialize(original);
        if (plain.isEmpty()) return null;

        String gradient = registry.getGradient(plain);
        if (gradient == null) return null;

        return styleComponent(original, gradient);
    }

    private Component stylePlainText(String text, String gradientId) {
        GradientRegistry.Gradient g = GradientRegistry.get(gradientId);
        if (g == null) return Component.text(text);

        Component result = Component.empty();
        for (int i = 0; i < text.length(); i++) {
            float t = (float) i / Math.max(1, text.length() - 1);
            TextColor color = GradientRegistry.colorAt(t, g);
            result = result.append(Component.text(String.valueOf(text.charAt(i)), color));
        }
        return result;
    }

    private boolean isAlreadyStyled(Component message) {
        Set<TextColor> colors = new HashSet<>();
        collectColors(message, colors);
        // Only skip messages that already have 4+ distinct colors (gradients).
        // This lets CatppuccinStyler aggressively tint single-color prefixes,
        // plain text, and unstyled player chat — while preserving existing gradients.
        return colors.size() > 3;
    }

    private void collectColors(Component message, Set<TextColor> colors) {
        if (message.color() != null) colors.add(message.color());
        for (Component child : message.children()) {
            collectColors(child, colors);
        }
    }

    /**
     * Checks whether the player (identified by UUID) has a /chatcolor set via
     * CadistProfile (or CadistClaim as fallback).
     * MUST be called on the main thread — uses Bukkit API.
     * Uses soft-reflection so there is no hard compile-time dependency.
     */
    private boolean hasCadistChatColor(UUID playerUuid, String text) {
        // Only relevant for the player-chat format: "<PlayerName> message"
        if (!text.startsWith("<")) return false;

        org.bukkit.entity.Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) return false;

        // Check CadistProfile first
        org.bukkit.plugin.Plugin profile = Bukkit.getPluginManager().getPlugin("CadistProfile");
        if (profile != null) {
            try {
                Object chatColorManager = profile.getClass().getMethod("getChatColorManager").invoke(profile);
                Object color = chatColorManager.getClass().getMethod("getColor", UUID.class)
                        .invoke(chatColorManager, playerUuid);
                return color != null;
            } catch (Exception ignored) {}
        }

        // Fallback to CadistClaim
        org.bukkit.plugin.Plugin cadist = Bukkit.getPluginManager().getPlugin("CadistClaim");
        if (cadist == null) return false;

        try {
            Object chatColorManager = cadist.getClass().getMethod("getChatColorManager").invoke(cadist);
            Object color = chatColorManager.getClass().getMethod("getColor", UUID.class)
                    .invoke(chatColorManager, playerUuid);
            return color != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Detects which plugin likely sent this message by matching against the
     * cached plugin name list. Safe to call off the main thread.
     */
    private String detectPluginFromContent(String text) {
        String lower = text.toLowerCase();

        if (text.startsWith("[")) {
            int end = text.indexOf("]");
            if (end > 1 && end < 40) {
                String candidate = text.substring(1, end).toLowerCase();
                for (String name : cachedPluginNames) {
                    if (name.equalsIgnoreCase(candidate)) {
                        return name;
                    }
                }
            }
        }

        int colon = text.indexOf(":");
        if (colon > 0 && colon < 40) {
            String candidate = text.substring(0, colon).trim().toLowerCase();
            for (String name : cachedPluginNames) {
                if (name.equalsIgnoreCase(candidate)) {
                    return name;
                }
            }
        }

        for (String name : cachedPluginNames) {
            if (lower.startsWith(name.toLowerCase())) {
                return name;
            }
        }

        return null;
    }

    private String pickRandomGradient() {
        List<String> gradients = new ArrayList<>(GradientRegistry.names());
        if (gradients.isEmpty()) return "twilight";
        return gradients.get(ThreadLocalRandom.current().nextInt(gradients.size()));
    }

    /**
     * Applies a gradient to the text content of a component tree while preserving
     * click events, hover events, and all other styles.
     */
    private Component styleComponent(Component original, String gradientId) {
        GradientRegistry.Gradient g = GradientRegistry.get(gradientId);
        if (g == null) return original;

        String text = PlainTextComponentSerializer.plainText().serialize(original);
        if (text.isEmpty()) return original;

        int[] index = new int[]{0};
        int totalLen = text.length();
        return styleRecursive(original, g, index, totalLen, Style.empty());
    }

    private Component styleRecursive(Component comp, GradientRegistry.Gradient g, int[] index, int totalLen, Style inheritedStyle) {
        // Merge inherited decorations with this component's own decorations
        // so that, e.g., a parent empty component with BOLD applies BOLD to its child text.
        Style effectiveStyle = comp.style().merge(inheritedStyle);

        // Process children first so we can rebuild the node
        List<Component> newChildren = new ArrayList<>();
        for (Component child : comp.children()) {
            newChildren.add(styleRecursive(child, g, index, totalLen, effectiveStyle));
        }

        if (comp instanceof TextComponent tc) {
            String content = tc.content();
            if (!content.isEmpty()) {
                Component result = Component.empty().style(effectiveStyle.color(null));

                for (int i = 0; i < content.length(); i++) {
                    float t = (float) index[0] / Math.max(1, totalLen - 1);
                    TextColor color = GradientRegistry.colorAt(t, g);
                    // Preserve click/hover/decorations/font by copying the full style,
                    // only overriding the color for this character
                    result = result.append(Component.text(String.valueOf(content.charAt(i)))
                            .style(effectiveStyle.color(color)));
                    index[0]++;
                }

                for (Component child : newChildren) {
                    result = result.append(child);
                }

                return result;
            }
        }

        return comp.children(newChildren);
    }
}
