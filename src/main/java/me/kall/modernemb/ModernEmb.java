package me.kall.modernemb;

import me.kall.modernemb.config.ModernEmbConfig;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;

@Mod(ModernEmb.MOD_ID)
public final class ModernEmb {
    public static final String MOD_ID = "modernemb";

    public ModernEmb(FMLJavaModLoadingContext context) {
        // The appearance page is client-only and so is its Forge config.
        if (FMLLoader.getDist().isClient()) {
            context.registerConfig(ModConfig.Type.CLIENT, ModernEmbConfig.SPEC);
        }
    }
}