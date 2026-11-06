package me.kall.modernemb.config;

import me.kall.narutoui.mui.color.MuiShimmerRamps;
import org.jetbrains.annotations.NotNull;

/**
 * Color ramps for the NarutoUI shimmer wave.
 * <p>
 * Every ramp has nine stops because the engine samples the wave into a nine-stop gradient. Mirrored
 * ramps avoid a visible seam when the wave wraps around. {@link #THEME} and {@link #CUSTOM} build a
 * light/core/deep ramp from two colors so they keep the same tonal depth as the hand-tuned presets.
 */
public enum GlowPalette {
    /** White, amber, orange and red: the original chakra wave. */
    CHAKRA(new int[]{
            0xFFFFFFFF, 0xFFFFD24A, 0xFFFFA726, 0xFFFF7043, 0xFFFF5252,
            0xFFFF7043, 0xFFFFA726, 0xFFFFD24A, 0xFFFFFFFF}),
    /** Cyan, teal and indigo. */
    AURORA(new int[]{
            0xFFFFFFFF, 0xFF9BF6FF, 0xFF5EEAD4, 0xFF34D399, 0xFF818CF8,
            0xFF34D399, 0xFF5EEAD4, 0xFF9BF6FF, 0xFFFFFFFF}),
    /** Cream and amber. */
    GOLD(new int[]{
            0xFFFFFFFF, 0xFFFFF3C4, 0xFFFFE08A, 0xFFFFC857, 0xFFFFA726,
            0xFFFFC857, 0xFFFFE08A, 0xFFFFF3C4, 0xFFFFFFFF}),
    /** Pale cyan and blue. */
    OCEAN(new int[]{
            0xFFFFFFFF, 0xFFB3E5FC, 0xFF81D4FA, 0xFF4FC3F7, 0xFF29B6F6,
            0xFF4FC3F7, 0xFF81D4FA, 0xFFB3E5FC, 0xFFFFFFFF}),
    /** Rose and blossom pink. */
    SAKURA(new int[]{
            0xFFFFFFFF, 0xFFFFE4EC, 0xFFFFC2D6, 0xFFFF9EBB, 0xFFFF6F91,
            0xFFFF9EBB, 0xFFFFC2D6, 0xFFFFE4EC, 0xFFFFFFFF}),
    /** Light/core/deep ramp derived from the active color scheme. */
    THEME,
    /** Light/core/deep ramp derived from the configured core and deep colors. */
    CUSTOM;

    private final int[] ramp;

    GlowPalette() {
        this(null);
    }

    GlowPalette(int[] ramp) {
        this.ramp = ramp;
    }

    /**
     * Returns the nine-stop ramp for this palette.
     *
     * @param first  core color for {@link #THEME} and {@link #CUSTOM}
     * @param second deep auxiliary color for {@link #THEME} and {@link #CUSTOM}
     * @return nine ARGB stops
     */
    public int @NotNull [] ramp(int first, int second) {
        if (this.ramp == null) {
            return MuiShimmerRamps.rich(first, second);
        }
        return this.ramp.clone();
    }
}