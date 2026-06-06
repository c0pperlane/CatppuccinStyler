package com.cadist.style.listener;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.config.GradientRegistry;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class StyleListener implements Listener {

    private final CatppuccinStyler plugin;

    public StyleListener(CatppuccinStyler plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        String gid = plugin.getStyleConfig().getEventGradient("join");
        event.joinMessage(styleComponent(extractText(event.joinMessage()), gid, "\u25B6 "));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        String gid = plugin.getStyleConfig().getEventGradient("quit");
        event.quitMessage(styleComponent(extractText(event.quitMessage()), gid, "\u25C0 "));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        String gid = plugin.getStyleConfig().getEventGradient("death");
        event.deathMessage(styleComponent(extractText(event.deathMessage()), gid, "\u2620 "));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String gid = plugin.getStyleConfig().getEventGradient("advancement");
        Component msg = event.message();
        if (msg != null) {
            event.message(styleComponent(extractText(msg), gid, "\u2726 "));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        String gid = plugin.getStyleConfig().getEventGradient("chat");
        if (gid == null || gid.isEmpty()) {
            return;
        }
        String text = extractText(event.message());
        event.message(styleComponent(text, gid, ""));
    }

    private String extractText(Component component) {
        if (component == null) return "";
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    private Component styleComponent(String text, String gradientId, String prefix) {
        if (text == null || text.isEmpty()) return Component.empty();
        GradientRegistry.Gradient g = GradientRegistry.get(gradientId);
        if (g == null) return Component.text(text);

        Component result = Component.empty();
        if (plugin.getStyleConfig().isUnicodeDecoration() && !prefix.isEmpty()) {
            result = result.append(Component.text(prefix, GradientRegistry.colorAt(0.5f, g)));
        }

        for (int i = 0; i < text.length(); i++) {
            float t = (float) i / Math.max(1, text.length() - 1);
            TextColor color = GradientRegistry.colorAt(t, g);
            result = result.append(Component.text(String.valueOf(text.charAt(i)), color));
        }
        return result;
    }
}
