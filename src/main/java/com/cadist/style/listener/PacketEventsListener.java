package com.cadist.style.listener;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
import com.cadist.style.config.MessagePattern;
import com.cadist.style.config.MessagePatternRegistry;
import com.cadist.style.util.TextFingerprint;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
        if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            return;
        }

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
        return styleRecursive(original, g, index, totalLen);
    }

    private Component styleRecursive(Component comp, GradientRegistry.Gradient g, int[] index, int totalLen) {
        // Process children first so we can rebuild the node
        List<Component> newChildren = new ArrayList<>();
        for (Component child : comp.children()) {
            newChildren.add(styleRecursive(child, g, index, totalLen));
        }

        if (comp instanceof TextComponent tc) {
            String content = tc.content();
            if (!content.isEmpty()) {
                Component result = Component.empty().style(tc.style().color(null));

                for (int i = 0; i < content.length(); i++) {
                    float t = (float) index[0] / Math.max(1, totalLen - 1);
                    TextColor color = GradientRegistry.colorAt(t, g);
                    // Preserve click/hover/decorations/font by copying the full style,
                    // only overriding the color for this character
                    result = result.append(Component.text(String.valueOf(content.charAt(i)))
                            .style(tc.style().color(color)));
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
