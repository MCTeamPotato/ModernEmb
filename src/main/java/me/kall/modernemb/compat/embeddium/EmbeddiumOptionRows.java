package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import me.jellysquid.mods.sodium.client.compat.modernui.MuiGuiScaleHook;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.kall.modernemb.config.GlowPalette;
import me.kall.modernemb.config.ModernEmbColors;
import me.kall.modernemb.config.ModernEmbSettings;
import me.kall.modernemb.config.ThemePreset;
import me.kall.modernemb.mixin.SliderControlAccessor;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.color.MuiColorBand;
import me.kall.narutoui.mui.color.MuiShimmerRamps;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.overlay.MuiColorPickerOverlay;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import me.kall.narutoui.mui.theme.MuiTheme;
import me.kall.narutoui.mui.widget.MuiColorBandField;
import me.kall.narutoui.mui.widget.MuiColorBandPreview;
import me.kall.narutoui.mui.widget.MuiColorField;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Maps Embeddium controls to the declarative Modern UI rows.
 * <p>
 * A slider is rebuilt from its own discrete range: the Modern UI slider ranges over the step index
 * and the formatter maps that index back to Embeddium's displayed value, so options such as the
 * frame-rate limit (10..260 in steps of 10) keep their exact steps.
 */
final class EmbeddiumOptionRows {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Embeddium");

    private EmbeddiumOptionRows() {}

    /**
     * @param option Embeddium option
     * @return whether this bridge knows how to render the option's control
     */
    static boolean supports(@NotNull Option<?> option) {
        return option.getControl() instanceof TickBoxControl
                || option.getControl() instanceof SliderControl
                || option.getControl() instanceof CyclingControl<?>
                || option.getControl() instanceof ModernEmbColorControl
                || option.getControl() instanceof ModernEmbColorBandControl;
    }

    /**
     * Appends one option row and marks the row unavailable when the option itself is unavailable.
     *
     * @param section target section
     * @param state   editing session
     * @param option  Embeddium option
     */
    static void add(@NotNull MuiSettingsSection section,
                    @NotNull EmbeddiumState state,
                    @NotNull Option<?> option) {
        String title = title(option);
        String description = description(option);

        if (ModernEmbSettings.config().glowActiveOnRows()) {
            // The marker only applies to the row added next; it does not leak into later rows.
            section.shimmerNext();
        }

        if (option.getControl() instanceof ModernEmbColorControl) {
            addColor(section, state, option, title, description);
        } else if (option.getControl() instanceof ModernEmbColorBandControl) {
            addBand(section, state, option, title, description);
        } else if (option.getControl() instanceof TickBoxControl) {
            addToggle(section, state, option, title, description);
        } else if (option.getControl() instanceof SliderControl slider) {
            addSlider(section, state, option, slider, title, description);
        } else if (option.getControl() instanceof CyclingControl<?> cycling) {
            addCycling(section, state, option, cycling, title, description);
        } else {
            LOGGER.warn("Unsupported Embeddium control {} for option '{}'",
                    option.getControl().getClass().getName(), title);
            section.value(title, String.valueOf(option.getValue()));
        }

        section.enabledWhen(option::isAvailable);
    }

    @SuppressWarnings("unchecked")
    private static void addColor(@NotNull MuiSettingsSection section,
                                 @NotNull EmbeddiumState state,
                                 @NotNull Option<?> option,
                                 @NotNull String title,
                                 @NotNull String description) {
        if (!(option.getValue() instanceof Integer)) {
            section.value(title, String.valueOf(option.getValue()));
            return;
        }

        Option<Integer> colorOption = (Option<Integer>) option;
        Context context = section.page().context();
        MuiTheme theme = section.page().theme();

        MuiColorField field = MuiColorField.create(context, theme,
                fieldLabel(title, colorOption.getValue()), colorOption.getValue());
        field.buttonLabel(label("modernemb.options.pick_color"));
        field.onPick(() -> pickColor(state, context, theme, title, colorOption.getValue(), argb -> {
            colorOption.setValue(argb);
            field.color(argb);
            field.label(fieldLabel(title, argb));
            state.markEdited();
        }));

        if (!description.isBlank()) {
            MuiControls.applyTitleTooltip(field, description);
        }
        section.custom(field);
    }

    @SuppressWarnings("unchecked")
    private static void addBand(@NotNull MuiSettingsSection section,
                                @NotNull EmbeddiumState state,
                                @NotNull Option<?> option,
                                @NotNull String title,
                                @NotNull String description) {
        if (!(option.getValue() instanceof MuiColorBand band)) {
            section.value(title, String.valueOf(option.getValue()));
            return;
        }

        Option<MuiColorBand> bandOption = (Option<MuiColorBand>) option;
        Context context = section.page().context();
        MuiTheme theme = section.page().theme();
        String firstLabel = label("modernemb.options.glow_band.first");
        String secondLabel = label("modernemb.options.glow_band.second");

        MuiColorBandField field = MuiColorBandField.create(context, theme, title, band.first(), band.second())
                .labels(firstLabel, secondLabel, label("modernemb.options.pick_color"))
                .rampProvider(value -> MuiShimmerRamps.rich(value.first(), value.second()));
        if (!description.isBlank()) {
            field.description(description);
        }

        field.onPickFirst(() -> pickColor(state, context, theme, firstLabel, field.firstColor(), argb -> {
            MuiColorBand next = bandOption.getValue().withFirst(argb);
            bandOption.setValue(next);
            field.first(argb);
            state.markEdited();
        }));
        field.onPickSecond(() -> pickColor(state, context, theme, secondLabel, field.secondColor(), argb -> {
            MuiColorBand next = bandOption.getValue().withSecond(argb);
            bandOption.setValue(next);
            field.second(argb);
            state.markEdited();
        }));

        section.custom(field);
    }

    private static void pickColor(@NotNull EmbeddiumState state,
                                  @NotNull Context context,
                                  @NotNull MuiTheme theme,
                                  @NotNull String title,
                                  int initial,
                                  @NotNull IntConsumer onPicked) {
        MuiColorPickerOverlay picker = new MuiColorPickerOverlay(
                context,
                theme,
                initial,
                new MuiColorPickerOverlay.Labels(
                        title,
                        label("modernemb.options.color_picker.brightness"),
                        label("modernemb.options.color_picker.alpha"),
                        label("modernemb.options.color_picker.apply"),
                        label("modernemb.options.color_picker.cancel")),
                argb -> onPicked.accept(ModernEmbColors.opaque(argb)));
        state.showOverlay(picker);
    }

    private static @NotNull String fieldLabel(@NotNull String base, int color) {
        return base + "  " + ModernEmbColors.format(color);
    }

    @SuppressWarnings("unchecked")
    private static void addToggle(@NotNull MuiSettingsSection section,
                                  @NotNull EmbeddiumState state,
                                  @NotNull Option<?> option,
                                  @NotNull String title,
                                  @NotNull String description) {
        Option<Boolean> booleanOption = (Option<Boolean>) option;
        section.toggle(title, description,
                () -> Boolean.TRUE.equals(booleanOption.getValue()),
                value -> {
                    booleanOption.setValue(value);
                    state.markEdited();
                });
    }

    private static void addSlider(@NotNull MuiSettingsSection section,
                                  @NotNull EmbeddiumState state,
                                  @NotNull Option<?> option,
                                  @NotNull SliderControl control,
                                  @NotNull String title,
                                  @NotNull String description) {
        final int min;
        int max;
        final int step;
        final ControlValueFormatter formatter;

        try {
            SliderControlAccessor access = (SliderControlAccessor) (Object) control;
            min = access.modernemb$min();
            max = access.modernemb$max();
            step = access.modernemb$interval();
            formatter = access.modernemb$formatter();
        } catch (Throwable throwable) {
            LOGGER.warn("Could not read the range of Embeddium slider '{}'", title, throwable);
            section.value(title, String.valueOf(option.getValue()));
            return;
        }

        // Embeddium caches this slider's max when the option page is constructed. On a window or
        // GUI-scale resize the cached value is stale, so re-query Modern UI's current maximum when
        // the row is rebuilt by the shell resize path.
        if (isGuiScale(option)) {
            max = MuiGuiScaleHook.getMaxGuiScale();
        }

        if (step <= 0 || max <= min) {
            section.value(title, String.valueOf(option.getValue()));
            return;
        }

        final int rangeMax = max;
        int steps = (rangeMax - min) / step;
        if (steps <= 0) {
            section.value(title, String.valueOf(option.getValue()));
            return;
        }

        Option<Integer> integerOption = castInteger(option);
        section.slider(title, description, 0, steps,
                () -> clamp((integerOption.getValue() - min) / step, 0, steps),
                index -> {
                    int value = min + clamp(index, 0, steps) * step;
                    integerOption.setValue(Math.min(value, rangeMax));
                    state.markEdited();
                },
                index -> {
                    int value = min + clamp(index, 0, steps) * step;
                    return formatter == null ? Integer.toString(value) : formatter.format(value).getString();
                });
    }

    @SuppressWarnings("unchecked")
    private static Option<Integer> castInteger(@NotNull Option<?> option) {
        return (Option<Integer>) option;
    }

    private static boolean isGuiScale(@NotNull Option<?> option) {
        var id = option.getId();
        return id != null && "minecraft".equals(id.getModId()) && "gui_scale".equals(id.getPath());
    }

    private static void addCycling(@NotNull MuiSettingsSection section,
                                   @NotNull EmbeddiumState state,
                                   @NotNull Option<?> option,
                                   @NotNull CyclingControl<?> control,
                                   @NotNull String title,
                                   @NotNull String description) {
        Object current = option.getValue();
        if (!(current instanceof Enum<?> currentValue)) {
            section.value(title, String.valueOf(current));
            return;
        }

        Component[] names = control.getNames();
        Object[] constants = currentValue.getDeclaringClass().getEnumConstants();
        if (constants == null || constants.length == 0) {
            section.value(title, String.valueOf(current));
            return;
        }

        List<MuiOption<Enum<?>>> choices = new ArrayList<>();
        for (Object constant : constants) {
            if (!(constant instanceof Enum<?> value)) {
                continue;
            }
            Component label = value.ordinal() < names.length ? names[value.ordinal()] : Component.literal(value.name());
            MuiOption<Enum<?>> choice = MuiOption.of(value, label.getString());

            // Palette and color-scheme cards are more useful with artwork than with a name alone.
            if (value instanceof ThemePreset preset) {
                choice = choice.preview((context, theme) ->
                        ThemePresetPreview.create(context, preset));
            } else if (value instanceof GlowPalette palette) {
                choice = choice.preview((context, theme) -> MuiColorBandPreview.create(context,
                        ModernEmbSettings.config().rampFor(palette),
                        28));
            }

            choices.add(choice);
        }

        if (choices.isEmpty()) {
            section.value(title, String.valueOf(current));
            return;
        }

        section.choiceGridBound(title, description, choices,
                () -> (Enum<?>) option.getValue(),
                value -> {
                    setEnum(option, value);
                    state.markEdited();
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setEnum(@NotNull Option<?> option, @NotNull Enum<?> value) {
        ((Option) option).setValue(value);
    }

    private static @NotNull String label(@NotNull String key) {
        return Component.translatable(key).getString();
    }

    private static @NotNull String title(@NotNull Option<?> option) {
        String title = option.getName().getString();
        return title.isBlank() ? "Option" : title;
    }

    private static @NotNull String description(@NotNull Option<?> option) {
        String description = option.getTooltip().getString();
        OptionImpact impact = option.getImpact();

        if (impact != null) {
            String impactLine = Component.translatable("sodium.options.performance_impact_string", impact.getLocalizedName()).getString();
            return description.isBlank() ? impactLine : description + "\n" + impactLine;
        }

        return description;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}