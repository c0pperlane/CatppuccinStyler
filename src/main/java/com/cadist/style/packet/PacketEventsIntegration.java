package com.cadist.style.packet;

import com.cadist.style.CatppuccinStyler;
import com.cadist.style.listener.PacketEventsListener;
import com.github.retrooper.packetevents.PacketEvents;

public class PacketEventsIntegration {

    public PacketEventsIntegration(CatppuccinStyler plugin) {
        try {
            PacketEvents.getAPI().getEventManager().registerListener(new PacketEventsListener(plugin));
            plugin.getLogger().info("PacketEvents integration enabled — intercepting system chat, TAB header/footer, and scoreboard packets.");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PacketEvents listener: " + e.getMessage());
        }
    }
}
