package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Marker control for the dynamic NarutoUI color-scheme selector.
 * <p>
 * The Modern UI bridge builds the choice grid from {@code MuiColorSchemes.all()} at page-build
 * time, so a scheme registered later appears without changing ModernEmb. Embeddium's deprecated
 * native screen gets a read-only id label.
 */
final class ModernEmbSchemeControl implements Control<String> {
    private final Option<String> option;

    ModernEmbSchemeControl(@NotNull Option<String> option) {
        this.option = option;
    }

    @Override
    public @NotNull Option<String> getOption() {
        return this.option;
    }

    @Override
    public @NotNull ControlElement<String> createElement(@NotNull Dim2i dim) {
        return new Element(this.option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 150;
    }

    /**
     * Read-only fallback used by Embeddium's old options screen.
     */
    private static final class Element extends ControlElement<String> {
        private Element(Option<String> option, Dim2i dim) {
            super(option, dim);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            String value = String.valueOf(this.option.getValue());
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