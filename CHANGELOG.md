# 1.1.0
- Derive the custom accent/surface palette with NarutoUI 1.9.0's `MuiSchemeGenerator`, so it gets the same contrast floors and OKLab interpolation as the built-in schemes instead of ModernEmb's own sRGB mixing. A light custom surface now gets dark text automatically.
- Follow the active scheme's generated shimmer ramp, including the Naruto chakra ramp, instead of re-deriving it per scheme.
- Bump NarutoUI to 1.9.0.
- Fix shader-pack option labels that use legacy color codes (for example `§e12 Chunks` and `§aVery Smooth`) rendering the raw `§` code: slider values are re-read every tick, and the bundled NarutoUI dependency now keeps their formatting on every refresh. Bump NarutoUI to 1.8.2, which also adds `§x` hexadecimal colors and dims disabled multi-color labels.
- Add a ModernEmb appearance page registered through Embeddium's `OptionPage` API, so it uses the same Apply, Revert and persistence flow as every other option.
- Store appearance settings in Forge's client config (`config/modernemb-client.toml`), with comments and range/color validation.
- Add color schemes: follow Modern UI, five fixed palettes, or a custom accent/surface pair with derived readable text and card colors.
- Add navigation styles: sidebar with the ModernEmb mark, classic title rail, or a top navigation bar.
- Add NarutoUI shimmer settings: palette, speed, strength, density and per-target switches. Option-row shimmer is on by default.
- Add a two-color custom shimmer band picked with NarutoUI's color picker; the nine-stop ramp is interpolated in OKLab and previewed live.
- Add a dynamic color-scheme picker backed by NarutoUI's `MuiColorSchemes`; Auto/Custom plus every shared scheme appear automatically, including schemes registered after ModernEmb is built.
- Add a `Theme colors` glow palette that follows the active color scheme; Naruto/Auto keep the chakra ramp, other schemes get a light/core/deep ramp from their accent and outline.
- Build custom bands with NarutoUI's `MuiShimmerRamps` light/core/deep generator; the band fields are now labelled Core color / Deep color.
- Move the OKLab band and two-color band row into NarutoUI's public API and consume NarutoUI from a locally built jar instead of CurseMaven.

# 1.0.0
- Initial release.
- Replace Configured's config editor with a Modern UI screen backed by the same config model.
- Support booleans, numbers, strings, enums, lists, nested folders, search, save/reset and restart hints.
- Add an unsaved-changes confirmation and a fixed action bar.