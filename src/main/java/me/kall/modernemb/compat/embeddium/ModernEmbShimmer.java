package me.kall.modernemb.compat.embeddium;

import me.kall.modernemb.config.ModernEmbConfig;
import me.kall.narutoui.mui.shimmer.MuiShimmerConfig;
import org.jetbrains.annotations.NotNull;

/**
 * Per-screen shimmer profile backed by ModernEmb's appearance config.
 * <p>
 * The profile is created for one screen build, so applying a new style rebuilds the shell and
 * installs a profile with the new palette, speed, density and strength. It never mutates
 * NarutoUI's shared default profile, which would leak ModernEmb settings into other mods' screens.
 */
final class ModernEmbShimmer implements MuiShimmerConfig {
    private final ModernEmbConfig config;
    private final int[] ramp;

    ModernEmbShimmer(@NotNull ModernEmbConfig config) {
        this.config = config;
        // The engine asks for the ramp on every animation tick and for every target. Generating
        // the OKLab band once per screen avoids hundreds of color-space conversions per frame.
        this.ramp = config.glowRamp();
    }

    @Override
    public boolean enabled() {
        return this.config.glow();
    }

    @Override
    public int fps() {
        return 30;
    }

    @Override
    public double speed() {
        return this.config.glowSpeed() / 100.0D;
    }

    @Override
    public double scale() {
        return this.config.glowScale() / 100.0D;
    }

    @Override
    public double strength() {
        return this.config.glowStrength() / 100.0D;
    }

    @Override
    public int[] ramp() {
        return this.ramp;
    }
}