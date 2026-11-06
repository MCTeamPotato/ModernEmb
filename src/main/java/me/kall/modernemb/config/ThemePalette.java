package me.kall.modernemb.config;

/**
 * Complete set of color tokens for one color scheme.
 *
 * @param primary          main accent color
 * @param onPrimary        content color on {@code primary}
 * @param surface          screen surface color
 * @param surfaceLow       card color
 * @param surfaceHigh      control color
 * @param onSurface        primary text color
 * @param onSurfaceVariant secondary text color
 * @param outline          divider and border color
 * @param error            destructive-action color
 */
public record ThemePalette(int primary, int onPrimary, int surface, int surfaceLow, int surfaceHigh,
                           int onSurface, int onSurfaceVariant, int outline, int error) {
}