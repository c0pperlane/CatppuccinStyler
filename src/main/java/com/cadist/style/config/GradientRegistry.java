package com.cadist.style.config;

import net.kyori.adventure.text.format.TextColor;

import java.util.*;

public final class GradientRegistry {
    private static final Map<String, Gradient> GRADIENTS = new LinkedHashMap<>();

    static {
        GRADIENTS.put("rose", new Gradient("#F5E0DC", "#F38BA8"));
        GRADIENTS.put("sunset", new Gradient("#F38BA8", "#FAB387"));
        GRADIENTS.put("flame", new Gradient("#FAB387", "#F9E2AF"));
        GRADIENTS.put("citrus", new Gradient("#F9E2AF", "#A6E3A1"));
        GRADIENTS.put("meadow", new Gradient("#A6E3A1", "#94E2D5"));
        GRADIENTS.put("ocean", new Gradient("#94E2D5", "#89B4FA"));
        GRADIENTS.put("twilight", new Gradient("#89B4FA", "#CBA6F7"));
        GRADIENTS.put("berry", new Gradient("#CBA6F7", "#F5C2E7"));
        GRADIENTS.put("frost", new Gradient("#F5C2E7", "#B4BEFE"));
        GRADIENTS.put("aurora", new Gradient("#B4BEFE", "#89DCEB"));
        GRADIENTS.put("royal", new Gradient("#89DCEB", "#74C7EC"));
        GRADIENTS.put("peachy", new Gradient("#74C7EC", "#F5E0DC"));
        GRADIENTS.put("coral", new Gradient("#F5E0DC", "#EBA0AC"));
        GRADIENTS.put("mint", new Gradient("#EBA0AC", "#A6E3A1"));
        GRADIENTS.put("galaxy", new Gradient("#A6E3A1", "#CBA6F7"));
        GRADIENTS.put("lavender_preset", new Gradient("#CBA6F7", "#B4BEFE"));
        GRADIENTS.put("forest", new Gradient("#A6E3A1", "#94E2D5"));
        GRADIENTS.put("sunset2", new Gradient("#F38BA8", "#F9E2AF"));
        GRADIENTS.put("cotton", new Gradient("#F5C2E7", "#F5E0DC"));
        GRADIENTS.put("ember", new Gradient("#FAB387", "#F38BA8"));
        GRADIENTS.put("sea", new Gradient("#89DCEB", "#89B4FA"));
        GRADIENTS.put("violet", new Gradient("#CBA6F7", "#F5C2E7"));
        GRADIENTS.put("spring", new Gradient("#94E2D5", "#A6E3A1"));
        GRADIENTS.put("neon", new Gradient("#F9E2AF", "#89DCEB"));
        GRADIENTS.put("ice", new Gradient("#89DCEB", "#B4BEFE"));
        GRADIENTS.put("cherry", new Gradient("#F38BA8", "#F5C2E7"));
        GRADIENTS.put("lime", new Gradient("#A6E3A1", "#F9E2AF"));
        GRADIENTS.put("bubblegum", new Gradient("#F5C2E7", "#EBA0AC"));
        GRADIENTS.put("dusk", new Gradient("#CBA6F7", "#89B4FA"));
        GRADIENTS.put("rainbow", new Gradient("#F38BA8", "#F9E2AF", "#A6E3A1", "#89B4FA", "#CBA6F7"));
    }

    public static Set<String> names() {
        return GRADIENTS.keySet();
    }

    public static Gradient get(String name) {
        return GRADIENTS.get(name);
    }

    public static String getString(String name) {
        Gradient g = GRADIENTS.get(name);
        return g == null ? null : g.toString();
    }

    public static Map<String, Gradient> all() {
        return Collections.unmodifiableMap(GRADIENTS);
    }

    public static TextColor colorAt(float t, Gradient g) {
        if (g.colors.length == 0) return TextColor.color(0xFFFFFF);
        if (g.colors.length == 1) return g.colors[0];

        t = Math.max(0f, Math.min(1f, t));
        float scaled = t * (g.colors.length - 1);
        int index = (int) scaled;
        float localT = scaled - index;

        if (index >= g.colors.length - 1) return g.colors[g.colors.length - 1];

        TextColor c1 = g.colors[index];
        TextColor c2 = g.colors[index + 1];

        int r = (int) (c1.red() + (c2.red() - c1.red()) * localT);
        int gr = (int) (c1.green() + (c2.green() - c1.green()) * localT);
        int b = (int) (c1.blue() + (c2.blue() - c1.blue()) * localT);

        return TextColor.color(r, gr, b);
    }

    public static class Gradient {
        public final TextColor[] colors;

        public Gradient(String... hexColors) {
            colors = new TextColor[hexColors.length];
            for (int i = 0; i < hexColors.length; i++) {
                colors[i] = parseColor(hexColors[i]);
            }
        }

        public Gradient(TextColor... colors) {
            this.colors = colors;
        }

        private static TextColor parseColor(String hex) {
            if (hex.startsWith("#")) hex = hex.substring(1);
            return TextColor.color(Integer.parseInt(hex, 16));
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) sb.append(":");
                sb.append(String.format("#%06X", colors[i].value()));
            }
            return sb.toString();
        }
    }
}
