package me.kall.modernemb.config;

import me.kall.narutoui.mui.color.MuiColorBand;
import me.kall.narutoui.mui.color.MuiColorScheme;
import me.kall.narutoui.mui.color.MuiColorSchemes;
import me.kall.narutoui.mui.color.MuiShimmerRamps;
import me.kall.narutoui.mui.color.MuiThemePalette;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * ModernEmb's own appearance settings, backed by Forge's client config.
 * <p>
 * The values are standard {@link ForgeConfigSpec} entries, so the file lives at
 * {@code config/modernemb-client.toml}, gets comments and range validation from Forge, and can be
 * edited without ModernEmb maintaining a second serialization format. The singleton instance is
 * used as the {@code OptionStorage} data object: option getters read the spec values and
 * {@code applyChanges()} writes them, exactly like any other Embeddium option.
 * <p>
 * Colors are stored as {@code RRGGBB} / {@code AARRGGBB} strings because Forge configs do not have
 * a color type; string tokens keep the TOML readable and are validated by {@link #SPEC}.
 */
public final class ModernEmbConfig {
    /** Default accent used by the custom palette. */
    public static final int DEFAULT_ACCENT = 0xFFE0A64A;
    /** Default surface used by the custom palette. */
    public static final int DEFAULT_SURFACE = 0xFF101116;
    /** Default first shimmer color. */
    public static final int DEFAULT_GLOW_FIRST = 0xFFFFD24A;
    /** Default second shimmer color. */
    public static final int DEFAULT_GLOW_SECOND = 0xFFFF5252;

    private static final Predicate<Object> COLOR_VALIDATOR = value ->
            value instanceof String text && ModernEmbColors.isColorToken(text);

    /** The registered client spec. */
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.ConfigValue<String> THEME_SCHEME;
    private static final ForgeConfigSpec.ConfigValue<String> THEME_ACCENT;
    private static final ForgeConfigSpec.ConfigValue<String> THEME_SURFACE;
    private static final ForgeConfigSpec.IntValue THEME_RADIUS;
    private static final ForgeConfigSpec.IntValue THEME_SPACING;

    private static final ForgeConfigSpec.EnumValue<NavigationStyle> NAVIGATION_STYLE;

    private static final ForgeConfigSpec.BooleanValue GLOW;
    private static final ForgeConfigSpec.EnumValue<GlowPalette> GLOW_PALETTE;
    private static final ForgeConfigSpec.ConfigValue<String> GLOW_FIRST;
    private static final ForgeConfigSpec.ConfigValue<String> GLOW_SECOND;
    private static final ForgeConfigSpec.IntValue GLOW_SPEED;
    private static final ForgeConfigSpec.IntValue GLOW_STRENGTH;
    private static final ForgeConfigSpec.IntValue GLOW_SCALE;
    private static final ForgeConfigSpec.BooleanValue GLOW_NAVIGATION;
    private static final ForgeConfigSpec.BooleanValue GLOW_TITLES;
    private static final ForgeConfigSpec.BooleanValue GLOW_ROWS;

    private static final ModernEmbConfig INSTANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("ModernEmb appearance settings. Colors use RRGGBB or AARRGGBB hex.").push("theme");
        THEME_SCHEME = builder
                .comment("AUTO, CUSTOM, or a scheme id registered in NarutoUI.",
                        "NarutoUI currently registers NARUTO, CLASSIC, JADE, AZURE, SAKURA and GRAPHITE; new schemes appear automatically.")
                .define("palette", "AUTO", value -> value instanceof String text && !text.isBlank());
        THEME_ACCENT = builder
                .comment("Accent color used by the CUSTOM scheme, for example FFE0A64A.")
                .define("accentColor", ModernEmbColors.toColorToken(DEFAULT_ACCENT), COLOR_VALIDATOR);
        THEME_SURFACE = builder
                .comment("Surface color used by the CUSTOM scheme, for example 101116.")
                .define("surfaceColor", ModernEmbColors.toColorToken(DEFAULT_SURFACE), COLOR_VALIDATOR);
        THEME_RADIUS = builder
                .comment("Corner radius of cards and controls, in DP.")
                .defineInRange("cornerRadius", 12, 0, 28);
        THEME_SPACING = builder
                .comment("Content spacing between sections, in DP.")
                .defineInRange("spacing", 20, 8, 40);
        builder.pop();

        builder.comment("Navigation bar layout.").push("navigation");
        NAVIGATION_STYLE = builder
                .comment("SIDEBAR, CLASSIC or TOP.")
                .defineEnum("style", NavigationStyle.SIDEBAR);
        builder.pop();

        builder.comment("NarutoUI shimmer effects.").push("glow");
        GLOW = builder
                .comment("Master switch for every shimmer effect on the ModernEmb screen.")
                .define("enabled", false);
        GLOW_PALETTE = builder
                .comment("CHAKRA, AURORA, GOLD, OCEAN, SAKURA, THEME or CUSTOM.",
                        "THEME builds a light/core/deep ramp from the active color scheme.",
                        "CUSTOM builds the same kind of ramp from the two configured band colors.")
                .defineEnum("palette", GlowPalette.THEME);
        GLOW_FIRST = builder
                .comment("Core color of the custom light/core/deep band, for example FFD24A.")
                .define("bandFirst", ModernEmbColors.toColorToken(DEFAULT_GLOW_FIRST), COLOR_VALIDATOR);
        GLOW_SECOND = builder
                .comment("Deep auxiliary color of the custom light/core/deep band, for example FF5252.")
                .define("bandSecond", ModernEmbColors.toColorToken(DEFAULT_GLOW_SECOND), COLOR_VALIDATOR);
        GLOW_SPEED = builder
                .comment("Shimmer wave speed in percent.")
                .defineInRange("speed", 100, 50, 250);
        GLOW_STRENGTH = builder
                .comment("Shimmer blend strength in percent.")
                .defineInRange("strength", 100, 10, 100);
        GLOW_SCALE = builder
                .comment("Spatial density of the shimmer wave in percent.")
                .defineInRange("density", 100, 50, 200);
        GLOW_NAVIGATION = builder
                .comment("Apply the wave to navigation labels.")
                .define("navigation", true);
        GLOW_TITLES = builder
                .comment("Apply the wave to rail or top-bar titles.")
                .define("titles", true);
        GLOW_ROWS = builder
                .comment("Apply the wave to option rows. Enabled by default.")
                .define("rows", true);
        builder.pop();

        SPEC = builder.build();
        INSTANCE = new ModernEmbConfig();
    }

    private ModernEmbConfig() {}

    /**
     * Returns the singleton data object used by Embeddium option bindings.
     *
     * @return the config facade
     */
    public static @NotNull ModernEmbConfig get() {
        return INSTANCE;
    }

    /**
     * Persists the current values. Forge owns the file, comments and range correction.
     */
    public static void save() {
        if (SPEC.isLoaded()) {
            SPEC.save();
        }
    }

    /**
     * @return whether any part of the shimmer effects is switched on
     */
    public boolean glowActive() {
        return this.glow() && (this.glowNavigation() || this.glowTitles() || this.glowRows());
    }

    /**
     * @return whether shimmering option rows need the shimmering widget factory
     */
    public boolean glowActiveOnRows() {
        return this.glow() && this.glowRows();
    }

    /**
     * Resolves the UI palette of the active color scheme.
     * <p>
     * {@code AUTO} follows Modern UI, whose theme tokens are not available here, so it falls back to
     * the shared Naruto palette. {@code CUSTOM} derives its palette from the configured accent and
     * surface. Unknown ids also fall back to {@code AUTO}.
     *
     * @return the active palette
     */
    public @NotNull MuiThemePalette activeThemePalette() {
        if (this.themeCustom()) {
            return ModernEmbColors.customPalette(this.accentColor(), this.surfaceColor());
        }
        MuiColorScheme scheme = this.themeScheme();
        return scheme != null ? scheme.theme() : MuiColorSchemes.NARUTO.theme();
    }

    /**
     * Returns the ramp for a glow palette.
     *
     * @param palette glow palette
     * @return nine ARGB stops
     */
    public int @NotNull [] rampFor(@NotNull GlowPalette palette) {
        if (palette == GlowPalette.THEME) {
            String id = this.themeSchemeId();
            // Naruto/Auto keep the signature chakra ramp; the other schemes get a rich ramp in
            // their own color family.
            if ("NARUTO".equalsIgnoreCase(id) || this.themeAuto()) {
                return GlowPalette.CHAKRA.ramp(0, 0);
            }
            MuiThemePalette theme = this.activeThemePalette();
            if (this.themeCustom()) {
                // The custom palette's outline is a neutral contrast token, not a deep accent.
                return MuiShimmerRamps.rich(theme.primary());
            }
            return MuiShimmerRamps.rich(theme.primary(), theme.outline());
        }
        return palette.ramp(this.glowColorFirst(), this.glowColorSecond());
    }

    /**
     * @return the ramp for the configured glow palette
     */
    public int @NotNull [] glowRamp() {
        return this.rampFor(this.glowPalette());
    }

    /**
     * @return the configured scheme id, never blank
     */
    public @NotNull String themeSchemeId() {
        String value = THEME_SCHEME.get();
        return value == null || value.isBlank() ? "AUTO" : value;
    }

    /**
     * Replaces the scheme id.
     *
     * @param id scheme id, {@code AUTO} or {@code CUSTOM}
     */
    public void setThemeSchemeId(@Nullable String id) {
        THEME_SCHEME.set(id == null || id.isBlank() ? "AUTO" : id);
    }

    /**
     * @return whether the screen follows Modern UI's own theme
     */
    public boolean themeAuto() {
        String id = this.themeSchemeId();
        return "AUTO".equalsIgnoreCase(id)
                || (!"CUSTOM".equalsIgnoreCase(id) && MuiColorSchemes.byId(id) == null);
    }

    /**
     * @return whether the custom accent/surface palette is active
     */
    public boolean themeCustom() {
        return "CUSTOM".equalsIgnoreCase(this.themeSchemeId());
    }

    /**
     * @return the selected shared scheme, or {@code null} for Auto/Custom/unknown ids
     */
    public @Nullable MuiColorScheme themeScheme() {
        return MuiColorSchemes.byId(this.themeSchemeId());
    }

    public int accentColor() {
        return ModernEmbColors.parseColorToken(THEME_ACCENT.get(), DEFAULT_ACCENT);
    }

    public void setAccentColor(int accentColor) {
        THEME_ACCENT.set(ModernEmbColors.toColorToken(accentColor));
    }

    public int surfaceColor() {
        return ModernEmbColors.parseColorToken(THEME_SURFACE.get(), DEFAULT_SURFACE);
    }

    public void setSurfaceColor(int surfaceColor) {
        THEME_SURFACE.set(ModernEmbColors.toColorToken(surfaceColor));
    }

    public int cornerRadius() {
        return THEME_RADIUS.get();
    }

    public void setCornerRadius(int cornerRadius) {
        THEME_RADIUS.set(clamp(cornerRadius, 0, 28));
    }

    public int contentSpacing() {
        return THEME_SPACING.get();
    }

    public void setContentSpacing(int contentSpacing) {
        THEME_SPACING.set(clamp(contentSpacing, 8, 40));
    }

    public NavigationStyle navigationStyle() {
        NavigationStyle value = NAVIGATION_STYLE.get();
        return value == null ? NavigationStyle.SIDEBAR : value;
    }

    public void setNavigationStyle(NavigationStyle navigationStyle) {
        NAVIGATION_STYLE.set(navigationStyle == null ? NavigationStyle.SIDEBAR : navigationStyle);
    }

    public boolean glow() {
        return GLOW.get();
    }

    public void setGlow(boolean glow) {
        GLOW.set(glow);
    }

    public GlowPalette glowPalette() {
        GlowPalette value = GLOW_PALETTE.get();
        return value == null ? GlowPalette.CHAKRA : value;
    }

    public void setGlowPalette(GlowPalette glowPalette) {
        GLOW_PALETTE.set(glowPalette == null ? GlowPalette.CHAKRA : glowPalette);
    }

    public int glowColorFirst() {
        return ModernEmbColors.parseColorToken(GLOW_FIRST.get(), DEFAULT_GLOW_FIRST);
    }

    public void setGlowColorFirst(int glowColorFirst) {
        GLOW_FIRST.set(ModernEmbColors.toColorToken(glowColorFirst));
    }

    public int glowColorSecond() {
        return ModernEmbColors.parseColorToken(GLOW_SECOND.get(), DEFAULT_GLOW_SECOND);
    }

    public void setGlowColorSecond(int glowColorSecond) {
        GLOW_SECOND.set(ModernEmbColors.toColorToken(glowColorSecond));
    }

    /**
     * @return the two endpoints of the custom shimmer band
     */
    public @NotNull MuiColorBand glowBand() {
        return new MuiColorBand(this.glowColorFirst(), this.glowColorSecond());
    }

    /**
     * Replaces both custom shimmer endpoints at once.
     *
     * @param band the new band
     */
    public void setGlowBand(MuiColorBand band) {
        if (band == null) {
            return;
        }
        this.setGlowColorFirst(band.first());
        this.setGlowColorSecond(band.second());
    }

    public int glowSpeed() {
        return GLOW_SPEED.get();
    }

    public void setGlowSpeed(int glowSpeed) {
        GLOW_SPEED.set(clamp(glowSpeed, 50, 250));
    }

    public int glowStrength() {
        return GLOW_STRENGTH.get();
    }

    public void setGlowStrength(int glowStrength) {
        GLOW_STRENGTH.set(clamp(glowStrength, 10, 100));
    }

    public int glowScale() {
        return GLOW_SCALE.get();
    }

    public void setGlowScale(int glowScale) {
        GLOW_SCALE.set(clamp(glowScale, 50, 200));
    }

    public boolean glowNavigation() {
        return GLOW_NAVIGATION.get();
    }

    public void setGlowNavigation(boolean glowNavigation) {
        GLOW_NAVIGATION.set(glowNavigation);
    }

    public boolean glowTitles() {
        return GLOW_TITLES.get();
    }

    public void setGlowTitles(boolean glowTitles) {
        GLOW_TITLES.set(glowTitles);
    }

    public boolean glowRows() {
        return GLOW_ROWS.get();
    }

    public void setGlowRows(boolean glowRows) {
        GLOW_ROWS.set(glowRows);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}