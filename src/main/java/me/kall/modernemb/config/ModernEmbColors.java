package me.kall.modernemb.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Color math shared by the theme presets and the custom palette.
 * <p>
 * Everything here is plain ARGB integer math so the result is identical on every platform and can
 * be reused by the configuration page, the screen builder and the shimmer profile. Theme colors are
 * forced opaque: a half-transparent surface would make the derived card and text colors read
 * against the wrong backdrop.
 */
public final class ModernEmbColors {
    /** Text color used on light backgrounds. */
    public static final int DARK_TEXT = 0xFF14161A;
    /** Text color used on dark backgrounds. */
    public static final int LIGHT_TEXT = 0xFFF5F6F8;
    /** Error color used on dark backgrounds. */
    public static final int DARK_ERROR = 0xFFFF6B6B;
    /** Error color used on light backgrounds. */
    public static final int LIGHT_ERROR = 0xFFB3261E;

    private static final Pattern COLOR_TOKEN = Pattern.compile("#?(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{8})");

    private ModernEmbColors() {}

    /**
     * Forces a color opaque.
     *
     * @param color ARGB color
     * @return the color with an alpha channel of {@code 0xFF}
     */
    public static int opaque(int color) {
        return color | 0xFF000000;
    }

    /**
     * Linearly blends two colors.
     *
     * @param from   start color
     * @param to     end color
     * @param amount blend amount, clamped to {@code 0..1}
     * @return the blended color
     */
    public static int mix(int from, int to, double amount) {
        double a = Math.max(0.0D, Math.min(1.0D, amount));
        int alpha = (int) Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * a);
        int red = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * a);
        int green = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * a);
        int blue = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * a);
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Returns the relative luminance of a color, ignoring its alpha channel.
     *
     * @param color ARGB color
     * @return luminance in {@code 0..1}
     */
    public static double luminance(int color) {
        double red = linear((color >> 16) & 0xFF);
        double green = linear((color >> 8) & 0xFF);
        double blue = linear(color & 0xFF);
        return 0.2126D * red + 0.7152D * green + 0.0722D * blue;
    }

    /**
     * Picks dark or light content for a filled control.
     * <p>
     * The crossover is where the dark and light text contrast ratios meet, around a background
     * luminance of {@code 0.18}; below that, light text is more readable, above it dark text is.
     *
     * @param background the fill color
     * @return a readable content color
     */
    public static int readableOn(int background) {
        return luminance(background) > 0.18D ? DARK_TEXT : LIGHT_TEXT;
    }

    /**
     * Builds a complete palette from a user-chosen accent and surface.
     * <p>
     * The remaining tokens are derived so a custom color scheme cannot end up with, for example,
     * light text on a light surface or a divider that is invisible against the card.
     *
     * @param accent  accent color
     * @param surface base surface color
     * @return the derived palette
     */
    public static @NotNull ThemePalette customPalette(int accent, int surface) {
        int primary = opaque(accent);
        int base = opaque(surface);
        int onSurface = readableOn(base);

        return new ThemePalette(
                primary,
                readableOn(primary),
                base,
                mix(base, onSurface, 0.06D),
                mix(base, onSurface, 0.12D),
                onSurface,
                mix(onSurface, base, 0.38D),
                mix(base, onSurface, 0.18D),
                luminance(base) > 0.45D ? LIGHT_ERROR : DARK_ERROR
        );
    }

    /**
     * Formats a color for the configuration rows.
     *
     * @param color ARGB color
     * @return {@code #RRGGBB} when opaque, otherwise {@code #AARRGGBB}
     */
    public static @NotNull String format(int color) {
        int alpha = (color >>> 24) & 0xFF;
        return alpha == 0xFF
                ? String.format("#%06X", color & 0xFFFFFF)
                : String.format("#%08X", color);
    }

    /**
     * Checks whether a config string is a color token.
     * <p>
     * Forge configs only provide primitive, enum, list and string values; colors are stored as
     * {@code RRGGBB} or {@code AARRGGBB} strings so the TOML stays readable. A leading {@code #} is
     * accepted for hand-edited files.
     *
     * @param value candidate token
     * @return {@code true} when the token is {@code #?RRGGBB} or {@code #?AARRGGBB}
     */
    public static boolean isColorToken(@Nullable CharSequence value) {
        return value != null && COLOR_TOKEN.matcher(value).matches();
    }

    /**
     * Parses a config color token.
     *
     * @param value    token such as {@code FFE0A64A} or {@code #E0A64A}
     * @param fallback color returned when the token is missing or invalid
     * @return the opaque ARGB color
     */
    public static int parseColorToken(@Nullable String value, int fallback) {
        if (value == null) {
            return fallback;
        }

        String token = value.startsWith("#") ? value.substring(1) : value;
        if (token.length() != 6 && token.length() != 8) {
            return fallback;
        }

        try {
            int color = (int) Long.parseLong(token, 16);
            if (token.length() == 6) {
                color |= 0xFF000000;
            }
            return opaque(color);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /**
     * Formats a color as the eight-digit token stored in the Forge config.
     *
     * @param color ARGB color
     * @return {@code AARRGGBB}
     */
    public static @NotNull String toColorToken(int color) {
        return String.format("%08X", opaque(color));
    }

    private static double linear(int channel) {
        double value = channel / 255.0D;
        return value <= 0.04045D ? value / 12.92D : Math.pow((value + 0.055D) / 1.055D, 2.4D);
    }
}