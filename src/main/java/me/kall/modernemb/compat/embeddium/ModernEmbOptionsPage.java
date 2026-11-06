package me.kall.modernemb.compat.embeddium;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.kall.modernemb.ModernEmb;
import me.kall.modernemb.config.GlowPalette;
import me.kall.modernemb.config.ModernEmbConfig;
import me.kall.modernemb.config.ModernEmbSettings;
import me.kall.modernemb.config.NavigationStyle;
import me.kall.narutoui.mui.color.MuiColorBand;
import me.kall.narutoui.mui.color.MuiColorSchemes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * ModernEmb's own appearance page, built with Embeddium's public option model.
 * <p>
 * Using {@code OptionPage}, {@code OptionGroup} and {@code OptionImpl} (instead of a private
 * settings screen) means the rows take part in Embeddium's normal edit lifecycle: staged values,
 * Apply, Revert, option flags and storage persistence all work exactly like an Embeddium option.
 * The Modern UI bridge then renders the same options with NarutoUI widgets.
 * <p>
 * Availability predicates read the staged option values rather than the applied config, so
 * selecting "Custom" immediately enables the color rows without rebuilding the screen.
 */
public final class ModernEmbOptionsPage {
    /** Page path under the {@code modernemb} namespace. */
    public static final String PAGE_PATH = "appearance";
    /** Group path for color-scheme options. */
    public static final String GROUP_THEME = "theme";
    /** Group path for navigation options. */
    public static final String GROUP_NAVIGATION = "navigation";
    /** Group path for shimmer options. */
    public static final String GROUP_GLOW = "glow";

    private static final ResourceLocation PAGE_ID = ResourceLocation.fromNamespaceAndPath(ModernEmb.MOD_ID, PAGE_PATH);
    private static final String PREFIX = "modernemb.options.";

    private ModernEmbOptionsPage() {}

    /**
     * Builds a fresh appearance page. Embeddium options are stateful, so a new page is created for
     * every options GUI construction instead of sharing one page between screens.
     *
     * @return the page
     */
    public static @NotNull OptionPage create() {
        final OptionImpl<ModernEmbConfig, String> preset = option("theme_preset", String.class,
                ModernEmbConfig::setThemeSchemeId, ModernEmbConfig::themeSchemeId,
                ModernEmbSchemeControl::new,
                () -> true);

        final OptionImpl<ModernEmbConfig, Integer> accent = option("theme_accent", Integer.class,
                ModernEmbConfig::setAccentColor, ModernEmbConfig::accentColor,
                ModernEmbColorControl::new,
                () -> "CUSTOM".equalsIgnoreCase(preset.getValue()));

        final OptionImpl<ModernEmbConfig, Integer> surface = option("theme_surface", Integer.class,
                ModernEmbConfig::setSurfaceColor, ModernEmbConfig::surfaceColor,
                ModernEmbColorControl::new,
                () -> "CUSTOM".equalsIgnoreCase(preset.getValue()));

        final OptionImpl<ModernEmbConfig, Integer> radius = option("theme_corner_radius", Integer.class,
                ModernEmbConfig::setCornerRadius, ModernEmbConfig::cornerRadius,
                control -> new SliderControl(control, 0, 28, 2, value -> Component.literal(value + " dp")),
                () -> !isAutoScheme(preset.getValue()));

        final OptionImpl<ModernEmbConfig, Integer> spacing = option("theme_spacing", Integer.class,
                ModernEmbConfig::setContentSpacing, ModernEmbConfig::contentSpacing,
                control -> new SliderControl(control, 8, 40, 4, value -> Component.literal(value + " dp")),
                () -> !isAutoScheme(preset.getValue()));

        final OptionImpl<ModernEmbConfig, NavigationStyle> navigation = option("navigation_style", NavigationStyle.class,
                ModernEmbConfig::setNavigationStyle, ModernEmbConfig::navigationStyle,
                control -> new CyclingControl<>(control, NavigationStyle.class,
                        names(NavigationStyle.class, "modernemb.navigation")),
                () -> true);

        final OptionImpl<ModernEmbConfig, Boolean> glow = option("glow_enabled", Boolean.class,
                ModernEmbConfig::setGlow, ModernEmbConfig::glow,
                TickBoxControl::new,
                () -> true);

        final OptionImpl<ModernEmbConfig, GlowPalette> palette = option("glow_palette", GlowPalette.class,
                ModernEmbConfig::setGlowPalette, ModernEmbConfig::glowPalette,
                control -> new CyclingControl<>(control, GlowPalette.class,
                        names(GlowPalette.class, "modernemb.glow")),
                glow::getValue);

        final OptionImpl<ModernEmbConfig, MuiColorBand> band = option("glow_band", MuiColorBand.class,
                ModernEmbConfig::setGlowBand, ModernEmbConfig::glowBand,
                ModernEmbColorBandControl::new,
                () -> glow.getValue() && palette.getValue() == GlowPalette.CUSTOM);

        final OptionImpl<ModernEmbConfig, Integer> speed = option("glow_speed", Integer.class,
                ModernEmbConfig::setGlowSpeed, ModernEmbConfig::glowSpeed,
                control -> new SliderControl(control, 50, 250, 10, value -> Component.literal(value + "%")),
                glow::getValue);

        final OptionImpl<ModernEmbConfig, Integer> strength = option("glow_strength", Integer.class,
                ModernEmbConfig::setGlowStrength, ModernEmbConfig::glowStrength,
                control -> new SliderControl(control, 10, 100, 5, value -> Component.literal(value + "%")),
                glow::getValue);

        final OptionImpl<ModernEmbConfig, Integer> scale = option("glow_scale", Integer.class,
                ModernEmbConfig::setGlowScale, ModernEmbConfig::glowScale,
                control -> new SliderControl(control, 50, 200, 10, value -> Component.literal(value + "%")),
                glow::getValue);

        final OptionImpl<ModernEmbConfig, Boolean> glowNavigation = option("glow_navigation", Boolean.class,
                ModernEmbConfig::setGlowNavigation, ModernEmbConfig::glowNavigation,
                TickBoxControl::new,
                glow::getValue);

        final OptionImpl<ModernEmbConfig, Boolean> glowTitles = option("glow_titles", Boolean.class,
                ModernEmbConfig::setGlowTitles, ModernEmbConfig::glowTitles,
                TickBoxControl::new,
                glow::getValue);

        final OptionImpl<ModernEmbConfig, Boolean> glowRows = option("glow_rows", Boolean.class,
                ModernEmbConfig::setGlowRows, ModernEmbConfig::glowRows,
                TickBoxControl::new,
                glow::getValue);

        OptionGroup themeGroup = OptionGroup.createBuilder()
                .setId(ResourceLocation.fromNamespaceAndPath(ModernEmb.MOD_ID, GROUP_THEME))
                .add(preset)
                .add(accent)
                .add(surface)
                .add(radius)
                .add(spacing)
                .build();

        OptionGroup navigationGroup = OptionGroup.createBuilder()
                .setId(ResourceLocation.fromNamespaceAndPath(ModernEmb.MOD_ID, GROUP_NAVIGATION))
                .add(navigation)
                .build();

        OptionGroup glowGroup = OptionGroup.createBuilder()
                .setId(ResourceLocation.fromNamespaceAndPath(ModernEmb.MOD_ID, GROUP_GLOW))
                .add(glow)
                .add(palette)
                .add(band)
                .add(speed)
                .add(strength)
                .add(scale)
                .add(glowNavigation)
                .add(glowTitles)
                .add(glowRows)
                .build();

        return new OptionPage(OptionIdentifier.create(PAGE_ID),
                Component.translatable("modernemb.options.page"),
                ImmutableList.of(themeGroup, navigationGroup, glowGroup));
    }

    /**
     * @param page option page
     * @return whether this page is ModernEmb's appearance page
     */
    public static boolean isAppearancePage(@Nullable OptionPage page) {
        return page != null && page.getId() != null && page.getId().matches(PAGE_ID);
    }

    /**
     * Returns the localized section title for one of ModernEmb's groups.
     *
     * @param id group id
     * @return the section title, or {@code null} when the group is not one of ours
     */
    public static @Nullable String groupTitle(@NotNull OptionIdentifier<?> id) {
        if (!OptionIdentifier.isPresent(id) || !ModernEmb.MOD_ID.equals(id.getModId())) {
            return null;
        }
        if (!GROUP_THEME.equals(id.getPath())
                && !GROUP_NAVIGATION.equals(id.getPath())
                && !GROUP_GLOW.equals(id.getPath())) {
            return null;
        }

        String key = PREFIX + "group." + id.getPath();
        String translated = Component.translatable(key).getString();
        return translated.equals(key) ? null : translated;
    }

    private static boolean isAutoScheme(@Nullable String id) {
        return id == null || id.isBlank() || "AUTO".equalsIgnoreCase(id)
                || (!"CUSTOM".equalsIgnoreCase(id) && MuiColorSchemes.byId(id) == null);
    }

    private static <T> OptionImpl<ModernEmbConfig, T> option(String path,
                                                             Class<T> type,
                                                             BiConsumer<ModernEmbConfig, T> setter,
                                                             Function<ModernEmbConfig, T> getter,
                                                             Function<OptionImpl<ModernEmbConfig, T>, Control<T>> control,
                                                             BooleanSupplier enabled) {
        return OptionImpl.createBuilder(type, ModernEmbSettings.storage())
                .setId(ResourceLocation.fromNamespaceAndPath(ModernEmb.MOD_ID, path))
                .setName(Component.translatable(PREFIX + path + ".name"))
                .setTooltip(Component.translatable(PREFIX + path + ".tooltip"))
                .setBinding(setter, getter)
                .setControl(control)
                .setEnabledPredicate(enabled)
                .build();
    }

    private static <E extends Enum<E>> Component[] names(Class<E> type, String prefix) {
        E[] values = type.getEnumConstants();
        Component[] names = new Component[values.length];
        for (int i = 0; i < values.length; i++) {
            names[i] = Component.translatable(prefix + "." + values[i].name().toLowerCase(Locale.ROOT));
        }
        return names;
    }
}