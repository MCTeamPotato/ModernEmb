package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlElement;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import me.kall.modernemb.config.ModernEmbColors;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.NotNull;

/**
 * Marker control for the single-color appearance options (custom accent and surface).
 * <p>
 * The Modern UI bridge recognizes this control and renders a NarutoUI color field that opens the
 * color picker overlay. Embeddium's deprecated native screen still calls {@link #createElement}, so
 * it gets a read-only hex label instead of crashing on an unknown control.
 */
final class ModernEmbColorControl implements Control<Integer> {
    private final Option<Integer> option;

    ModernEmbColorControl(@NotNull Option<Integer> option) {
        this.option = option;
    }

    @Override
    public @NotNull Option<Integer> getOption() {
        return this.option;
    }

    @Override
    public @NotNull ControlElement<Integer> createElement(@NotNull Dim2i dim) {
        return new Element(this.option, dim);
    }

    @Override
    public int getMaxWidth() {
        return 110;
    }

    /**
     * Read-only fallback used by Embeddium's old options screen.
     */
    private static final class Element extends ControlElement<Integer> {
        private Element(Option<Integer> option, Dim2i dim) {
            super(option, dim);
        }

        @Override
        public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            super.render(graphics, mouseX, mouseY, delta);

            String value = ModernEmbColors.format(this.option.getValue());
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