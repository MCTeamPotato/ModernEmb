package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import me.kall.modernemb.config.ModernEmbConfig;
import me.kall.modernemb.config.ModernEmbSettings;
import me.kall.narutoui.mui.background.MuiBackground;
import me.kall.narutoui.mui.color.MuiThemePalette;
import me.kall.narutoui.mui.theme.MuiTheme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Resolves the NarutoUI theme and background from ModernEmb's appearance config.
 * <p>
 * {@code AUTO} deliberately returns {@code null}, which tells NarutoUI to resolve the host Modern UI
 * theme. Every other scheme produces a fixed theme; the custom scheme derives its remaining tokens
 * from the chosen accent and surface so contrast stays readable.
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
        if (config.themeAuto()) {
            return null;
        }

        MuiThemePalette palette = config.activeThemePalette();
        return palette.toTheme(config.cornerRadius(), config.contentSpacing());
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
        if (config.themeAuto()) {
            return MuiBackground.transparent();
        }

        MuiThemePalette palette = config.activeThemePalette();
        return MuiBackground.color((BACKDROP_ALPHA << 24) | (palette.surface() & 0xFFFFFF));
    }
}