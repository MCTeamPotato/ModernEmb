package me.kall.modernemb.compat.oculus;

import me.kall.modernemb.compat.embeddium.ShaderState;
import org.jetbrains.annotations.NotNull;

/**
 * Loads the Oculus-backed shader session without touching Oculus classes when the mod is absent.
 * <p>
 * {@link me.kall.modernemb.compat.embeddium.EmbeddiumMuiScreen} resolves this class by name after
 * checking the mod list, so an installation without Oculus/Iris never links these classes.
 */
public final class OculusShaderFactory {
    private OculusShaderFactory() {}

    /**
     * Creates the shader session.
     *
     * @return a new session backed by Oculus' live configuration
     */
    public static @NotNull ShaderState create() {
        return new OculusShaderState();
    }
}