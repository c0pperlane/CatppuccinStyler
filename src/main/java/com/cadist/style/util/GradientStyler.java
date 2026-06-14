package com.cadist.style.util;

import com.cadist.style.config.GradientRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.EnumSet;
import java.util.Set;

/**
 * Builds a Catppuccin-gradient component from a legacy-formatted string,
 * filtering out color codes while keeping formatting codes (bold, italic, etc.).
 */
public final class GradientStyler {

    private GradientStyler() {}

    public static Component styleLegacyLine(String text, String gradientId) {
        GradientRegistry.Gradient g = GradientRegistry.get(gradientId);
        if (g == null) return Component.text(text);
        if (text == null || text.isEmpty()) return Component.empty();

        int displayCount = countDisplayChars(text);
        if (displayCount == 0) return Component.empty();

        Component result = Component.empty();
        Set<TextDecoration> decorations = EnumSet.noneOf(TextDecoration.class);
        int index = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'r') {
                    // Reset: clear all decorations
                    decorations.clear();
                    i += 2;
                    continue;
                }
                if (isColorOrReset(code)) {
                    i += 2;
                    continue;
                }
                if (code == '#') {
                    // Hex color code: &#RRGGBB or §#RRGGBB
                    if (i + 7 < text.length() && isHex(text.substring(i + 2, i + 8))) {
                        i += 8;
                    } else {
                        i += 2;
                    }
                    continue;
                }
                TextDecoration deco = decorationFor(code);
                if (deco != null) {
                    // Toggle handling: &l/&L = set, but we only support set for now
                    decorations.add(deco);
                    i += 2;
                    continue;
                }
                // Unknown code — emit the '&' literally and continue parsing
                // (avoids eating the 'l' in "&lVoxelPower" if the code is somehow not recognized)
                i++;
                continue;
            }

            float t = (float) index / Math.max(1, displayCount - 1);
            TextColor color = GradientRegistry.colorAt(t, g);
            Component charComp = Component.text(String.valueOf(c), color);
            if (!decorations.isEmpty()) {
                charComp = charComp.decorate(decorations.toArray(new TextDecoration[0]));
            }
            result = result.append(charComp);
            index++;
            i++;
        }

        return result;
    }

    private static int countDisplayChars(String text) {
        int count = 0;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char code = Character.toLowerCase(text.charAt(i + 1));
                if (code == 'r' || isColorOrReset(code)) {
                    i += 2;
                    continue;
                }
                if (code == '#') {
                    if (i + 7 < text.length() && isHex(text.substring(i + 2, i + 8))) {
                        i += 8;
                    } else {
                        i += 2;
                    }
                    continue;
                }
                if (decorationFor(code) != null) {
                    i += 2;
                    continue;
                }
                // Unknown code — keep advancing but only by 1 so the '&' becomes a display char
                i++;
                count++;
                continue;
            }
            count++;
            i++;
        }
        return count;
    }

    private static boolean isColorOrReset(char code) {
        return (code >= '0' && code <= '9') || (code >= 'a' && code <= 'f') || code == 'r';
    }

    private static boolean isHex(String s) {
        for (char c : s.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return s.length() == 6;
    }

    private static TextDecoration decorationFor(char code) {
        return switch (code) {
            case 'k' -> TextDecoration.OBFUSCATED;
            case 'l' -> TextDecoration.BOLD;
            case 'm' -> TextDecoration.STRIKETHROUGH;
            case 'n' -> TextDecoration.UNDERLINED;
            case 'o' -> TextDecoration.ITALIC;
            default -> null;
        };
    }
}
