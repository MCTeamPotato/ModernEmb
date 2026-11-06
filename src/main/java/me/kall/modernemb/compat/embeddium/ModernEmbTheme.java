package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import me.kall.modernemb.config.ModernEmbColors;
import me.kall.modernemb.config.ModernEmbConfig;
import me.kall.modernemb.config.ModernEmbSettings;
import me.kall.modernemb.config.ThemePalette;
import me.kall.modernemb.config.ThemePreset;
import me.kall.narutoui.mui.background.MuiBackground;
import me.kall.narutoui.mui.theme.MuiTheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the NarutoUI theme and background from ModernEmb's appearance config.
 * <p>
 * {@link ThemePreset#AUTO} deliberately returns {@code null}, which tells NarutoUI to resolve the
 * host Modern UI theme. Every other preset produces a fixed theme; the custom preset derives its
 * remaining tokens from the chosen accent and surface so contrast stays readable.
 */
final class ModernEmbTheme {
    /** Alpha of the colored backdrop used by non-auto themes. */
    private static final int BACKDROP_ALPHA = 0xC0;

    private ModernEmbTheme() {}

    /**
     * Resolves the theme override for a config.
     *
     * @param config appearance config
     * @return the theme override, or {@code null} to follow Modern UI
     */
    static @Nullable MuiTheme resolve(@NotNull ModernEmbConfig config) {
        ThemePreset preset = config.themePreset();
        if (preset.auto()) {
            return null;
        }

        ThemePalette palette = palette(config);
        return new MuiTheme(
                palette.primary(),
                palette.onPrimary(),
                palette.surface(),
                palette.surfaceLow(),
                palette.surfaceHigh(),
                palette.onSurface(),
                palette.onSurfaceVariant(),
                palette.outline(),
                palette.error(),
                config.cornerRadius(),
                config.contentSpacing()
        );
    }

    /**
     * Resolves the theme for callbacks that only receive a context, such as the rail logo.
     *
     * @param context screen context
     * @return the resolved theme
     */
    static @NotNull MuiTheme resolve(@NotNull Context context) {
        MuiTheme override = resolve(ModernEmbSettings.config());
        return override != null ? override : MuiTheme.modern(context);
    }

    /**
     * Builds the screen backdrop for a config.
     * <p>
     * Automatic mode keeps the transparent background used by earlier ModernEmb versions. A fixed
     * or custom palette gets a translucent tint of its own surface, so the chosen scheme is visible
     * even before the player looks at the cards.
     *
     * @param config appearance config
     * @return the background provider
     */
    static @NotNull MuiBackground background(@NotNull ModernEmbConfig config) {
        if (config.themePreset().auto()) {
            return MuiBackground.transparent();
        }

        ThemePalette palette = palette(config);
        return MuiBackground.color((BACKDROP_ALPHA << 24) | (palette.surface() & 0xFFFFFF));
    }

    /**
     * Resolves the palette for a non-auto preset.
     *
     * @param config appearance config
     * @return a fixed palette, or one derived from the custom accent and surface
     */
    private static @NotNull ThemePalette palette(@NotNull ModernEmbConfig config) {
        ThemePreset preset = config.themePreset();
        if (preset.custom() || preset.palette() == null) {
            return ModernEmbColors.customPalette(config.accentColor(), config.surfaceColor());
        }
        return preset.palette();
    }
}