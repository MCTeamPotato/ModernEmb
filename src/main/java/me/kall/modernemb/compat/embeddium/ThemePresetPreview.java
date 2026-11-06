package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.view.View;
import me.kall.modernemb.config.ModernEmbColors;
import me.kall.modernemb.config.ModernEmbSettings;
import me.kall.modernemb.config.ThemePalette;
import me.kall.modernemb.config.ThemePreset;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.theme.MuiTheme;
import org.jetbrains.annotations.NotNull;

/**
 * Small palette swatch used as the artwork of a color-scheme choice card.
 * <p>
 * The preview shows the actual surface, card, accent and text colors of the candidate scheme, so
 * choosing a palette is a visual decision instead of reading enum names. Auto uses the active
 * Modern UI theme, matching what the screen will look like after selection.
 */
final class ThemePresetPreview extends View {
    private final ThemePalette palette;
    private final Paint paint = new Paint();

    private ThemePresetPreview(@NotNull Context context, @NotNull ThemePalette palette) {
        super(context);
        this.palette = palette;
        this.paint.setAntiAlias(true);
        this.setMinimumHeight(MuiControls.dp(context, 56));
        this.setClickable(false);
    }

    /**
     * Builds the preview for one preset.
     *
     * @param context screen context
     * @param preset  candidate preset
     * @return the preview view
     */
    static @NotNull View create(@NotNull Context context, @NotNull ThemePreset preset) {
        ThemePalette palette;
        if (preset.auto()) {
            // Resolve the host theme instead of reusing the current screen theme: when the user
            // is on a fixed palette, the Auto card must still show what Auto would look like.
            MuiTheme auto = MuiTheme.modern(context);
            palette = new ThemePalette(
                    auto.primary(), auto.onPrimary(), auto.surface(), auto.surfaceLow(),
                    auto.surfaceHigh(), auto.onSurface(), auto.onSurfaceVariant(),
                    auto.outline(), auto.error());
        } else if (preset.custom() || preset.palette() == null) {
            palette = ModernEmbColors.customPalette(
                    ModernEmbSettings.config().accentColor(),
                    ModernEmbSettings.config().surfaceColor());
        } else {
            palette = preset.palette();
        }
        return new ThemePresetPreview(context, palette);
    }

    @Override
    protected void onDraw(@NotNull Canvas canvas) {
        int width = this.getWidth();
        int height = this.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        Context context = this.getContext();
        int inset = MuiControls.dp(context, 6);
        int accentWidth = MuiControls.dp(context, 6);
        int lineLeft = inset + MuiControls.dp(context, 18);
        int lineRight = width - inset - MuiControls.dp(context, 10);
        float panelRadius = MuiControls.dp(context, 4);

        this.paint.setColor(this.palette.surface());
        canvas.drawRect(0.0F, 0.0F, width, height, this.paint);

        this.paint.setColor(this.palette.surfaceLow());
        canvas.drawRoundRect(inset, inset, width - inset, height - inset, panelRadius, this.paint);

        this.paint.setColor(this.palette.primary());
        canvas.drawRoundRect(inset + MuiControls.dp(context, 6), inset + MuiControls.dp(context, 6),
                inset + MuiControls.dp(context, 6) + accentWidth, height - inset - MuiControls.dp(context, 6),
                MuiControls.dp(context, 3), this.paint);

        this.paint.setColor(this.palette.onSurface());
        canvas.drawRoundRect(lineLeft, inset + MuiControls.dp(context, 8),
                Math.max(lineLeft, lineRight), inset + MuiControls.dp(context, 12),
                MuiControls.dp(context, 2), this.paint);

        this.paint.setColor(this.palette.onSurfaceVariant());
        canvas.drawRoundRect(lineLeft, inset + MuiControls.dp(context, 17),
                Math.max(lineLeft, width - inset - MuiControls.dp(context, 24)),
                inset + MuiControls.dp(context, 20), MuiControls.dp(context, 2), this.paint);

        this.paint.setColor(this.palette.surfaceHigh());
        canvas.drawRoundRect(lineLeft, inset + MuiControls.dp(context, 27),
                Math.max(lineLeft, width - inset - MuiControls.dp(context, 30)),
                height - inset - MuiControls.dp(context, 6), MuiControls.dp(context, 3), this.paint);
    }
}