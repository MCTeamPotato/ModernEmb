package me.kall.modernemb.config;

import me.kall.narutoui.mui.color.MuiHudPreset;
import me.kall.narutoui.mui.color.MuiRampStyle;
import me.kall.narutoui.mui.color.MuiSchemeGenerator;
import me.kall.narutoui.mui.color.MuiSurfaceTint;
import me.kall.narutoui.mui.color.MuiThemePalette;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

/**
 * Color token handling for the theme presets and the custom palette.
 * <p>
 * The palette itself is not derived here: {@link MuiSchemeGenerator} owns that, so a custom scheme
 * gets the same contrast floors, the same OKLab interpolation and the same light/dark handling as
 * the built-in ones. This class only parses and formats the config tokens.
 */
public final class ModernEmbColors {
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
     * Builds a complete palette from a player-chosen accent and surface.
     * <p>
     * The surface is taken as given and the remaining tokens are derived from it, so a custom
     * scheme cannot end up with, for example, light text on a light surface or a divider that is
     * invisible against the card.
     *
     * @param accent  accent color
     * @param surface base surface color
     * @return the derived palette
     */
    public static @NotNull MuiThemePalette customPalette(int accent, int surface) {
        return MuiSchemeGenerator.tokens(accent, MuiSurfaceTint.BRAND, opaque(surface));
    }

    /**
     * Builds the shimmer ramp of the custom scheme.
     * <p>
     * The ramp follows the same rule as the built-in schemes: a mirrored light-to-deep band in the
     * accent's own family, interpolated in OKLab.
     *
     * @param accent  accent color
     * @param surface base surface color
     * @return nine ARGB stops
     */
    public static int @NotNull [] customRamp(int accent, int surface) {
        MuiThemePalette palette = customPalette(accent, surface);
        return MuiSchemeGenerator.ramp(palette,
                MuiSchemeGenerator.hud(palette, MuiHudPreset.DERIVED), MuiRampStyle.HUE_SWEEP);
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
}
