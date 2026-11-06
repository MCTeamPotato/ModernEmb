package me.kall.modernemb.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Built-in color schemes offered by ModernEmb's appearance page.
 * <p>
 * {@link #AUTO} keeps Modern UI's resolved theme and therefore follows the player's resource pack
 * and light/dark mode. Every other preset has a fixed palette so its derived text and card colors
 * stay predictable. {@link #CUSTOM} derives its palette from the configured accent and surface.
 */
public enum ThemePreset {
    /** Follow the Modern UI theme without overriding it. */
    AUTO,
    /** Gold accent on the original near-black surface. */
    NARUTO(new ThemePalette(
            0xFFE0A64A, 0xFF1A1208, 0xFF101116, 0xFF1F2027, 0xFF2A2B34,
            0xFFEDEFF3, 0xFFA6AAB4, 0xFF3A3C48, 0xFFFF6B6B)),
    /** Jade accent on a deep green surface. */
    JADE(new ThemePalette(
            0xFF3DDC97, 0xFF07281A, 0xFF0B1512, 0xFF13231D, 0xFF1C322A,
            0xFFE7F6EF, 0xFF9DB8AC, 0xFF2B463C, 0xFFFF6B6B)),
    /** Azure accent on a deep blue surface. */
    AZURE(new ThemePalette(
            0xFF5AA9FF, 0xFF04162B, 0xFF0A111C, 0xFF121C2C, 0xFF1B2A3F,
            0xFFE8F1FF, 0xFFA3B6CF, 0xFF2B3D55, 0xFFFF6B6B)),
    /** Sakura accent on a warm charcoal surface. */
    SAKURA(new ThemePalette(
            0xFFFF8FB3, 0xFF2A0712, 0xFF150E13, 0xFF21161D, 0xFF2D1E27,
            0xFFFDEBF1, 0xFFC9A9B6, 0xFF4A2E3A, 0xFFFF6B6B)),
    /** Neutral graphite surface with a light accent. */
    GRAPHITE(new ThemePalette(
            0xFFD6D9E0, 0xFF111318, 0xFF0E0F12, 0xFF17191E, 0xFF22252C,
            0xFFF2F3F6, 0xFFA9AEB8, 0xFF3A3E47, 0xFFFF6B6B)),
    /** Palette derived from the configured accent and surface colors. */
    CUSTOM;

    private final @Nullable ThemePalette palette;

    ThemePreset() {
        this(null);
    }

    ThemePreset(@NotNull ThemePalette palette) {
        this.palette = palette;
    }

    /**
     * @return the fixed palette, or {@code null} for {@link #AUTO} and {@link #CUSTOM}
     */
    public @Nullable ThemePalette palette() {
        return this.palette;
    }

    /**
     * @return whether this preset follows Modern UI's own theme
     */
    public boolean auto() {
        return this == AUTO;
    }

    /**
     * @return whether this preset derives its palette from user colors
     */
    public boolean custom() {
        return this == CUSTOM;
    }
}