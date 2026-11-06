package me.kall.modernemb.compat.embeddium;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.view.View;
import me.kall.modernemb.config.ModernEmbColors;
import me.kall.modernemb.config.ModernEmbConfig;
import me.kall.narutoui.mui.color.MuiThemePalette;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.theme.MuiTheme;
import org.jetbrains.annotations.NotNull;

/**
 * Small palette swatch used as the artwork of a color-scheme choice card.
 * <p>
 * The preview shows the actual surface, card, accent and text colors of the candidate scheme, so
 * choosing a scheme is a visual decision instead of reading ids. Auto resolves the host Modern UI
 * theme; Custom derives its palette from the configured accent and surface.
 */
final class ThemeSchemePreview extends View {
    private final MuiThemePalette palette;
    private final Paint paint = new Paint();

    private ThemeSchemePreview(@NotNull Context context, @NotNull MuiThemePalette palette) {
        super(context);
        this.palette = palette;
        this.paint.setAntiAlias(true);
        this.setMinimumHeight(MuiControls.dp(context, 56));
        this.setClickable(false);
    }

    /**
     * Builds the auto-scheme preview from Modern UI's resolved theme.
     *
     * @param context screen context
     * @return the preview view
     */
    static @NotNull View auto(@NotNull Context context) {
        MuiTheme active = MuiTheme.modern(context);
        return new ThemeSchemePreview(context, new MuiThemePalette(
                active.primary(), active.onPrimary(), active.surface(), active.surfaceLow(),
                active.surfaceHigh(), active.onSurface(), active.onSurfaceVariant(),
                active.outline(), active.error()));
    }

    /**
     * Builds the custom-scheme preview from the configured accent and surface.
     *
     * @param context screen context
     * @param config  appearance config
     * @return the preview view
     */
    static @NotNull View custom(@NotNull Context context, @NotNull ModernEmbConfig config) {
        return new ThemeSchemePreview(context,
                ModernEmbColors.customPalette(config.accentColor(), config.surfaceColor()));
    }

    /**
     * Builds a preview for a fixed shared palette.
     *
     * @param context screen context
     * @param palette candidate palette
     * @return the preview view
     */
    static @NotNull View create(@NotNull Context context, @NotNull MuiThemePalette palette) {
        return new ThemeSchemePreview(context, palette);
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