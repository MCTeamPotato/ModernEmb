package me.kall.modernemb.mixin;

import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the range and formatter of Embeddium's slider so the Modern UI bridge can rebuild the
 * same discrete steps without copying the values from the option pages.
 */
@Mixin(SliderControl.class)
public interface SliderControlAccessor {
    @Accessor("min")
    int modernemb$min();

    @Accessor("max")
    int modernemb$max();

    @Accessor("interval")
    int modernemb$interval();

    @Accessor("mode")
    ControlValueFormatter modernemb$formatter();
}