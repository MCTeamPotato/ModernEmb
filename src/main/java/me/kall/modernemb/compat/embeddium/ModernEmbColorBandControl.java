package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import me.kall.narutoui.mui.color.MuiColorBand;
import me.kall.narutoui.mui.color.MuiColorBands;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Marker control for the two-color shimmer band.
 * <p>
 * The Modern UI bridge renders a NarutoUI {@code MuiColorBandField} with a live OKLab preview and
 * two color pickers. The native Embeddium screen gets a read-only summary of both endpoints.
 */
final class ModernEmbColorBandControl implements Control<MuiColorBand> {
    private final Option<MuiColorBand> option;

    ModernEmbColorBandControl(@NotNull Option<MuiColorBand> option) {
        this.option = option;
    }

    @Override
    public @NotNull Option<MuiColorBand> getOption() {
        return this.option;
    }

    @Override
    public @NotNull ControlElement<MuiColorBand> createElement(@NotNull Dim2i dim) {
        return new Element(this.option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 150;
    }

    /**
     * Read-only fallback used by Embeddium's old options screen.
     */
    private static final class Element extends ControlElement<MuiColorBand> {
        private Element(Option<MuiColorBand> option, Dim2i dim) {
            super(option, dim);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            MuiColorBand band = this.option.getValue();
            String value = band == null
                    ? ""
                    : MuiColorBands.toHex(band.first()) + " / " + MuiColorBands.toHex(band.second());
            int width = this.font.width(value);
            this.drawString(graphics, value, this.dim.getLimitX() - width - 6,
                    this.dim.getCenterY() - 4, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return false;
        }

        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            return false;
        }
    }
}