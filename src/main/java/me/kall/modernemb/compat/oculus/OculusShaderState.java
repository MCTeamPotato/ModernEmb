package me.kall.modernemb.compat.oculus;

import icyllis.modernui.core.Context;
import icyllis.modernui.mc.MuiScreen;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import me.kall.modernemb.compat.embeddium.ShaderState;
import me.kall.narutoui.mui.choice.MuiOption;
import me.kall.narutoui.mui.layout.MuiControls;
import me.kall.narutoui.mui.screen.MuiConfigFragment;
import me.kall.narutoui.mui.screen.MuiSettingsPage;
import me.kall.narutoui.mui.screen.MuiSettingsSection;
import me.kall.narutoui.mui.theme.MuiTheme;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.config.IrisConfig;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.option.BooleanOption;
import net.irisshaders.iris.shaderpack.option.MergedBooleanOption;
import net.irisshaders.iris.shaderpack.option.MergedStringOption;
import net.irisshaders.iris.shaderpack.option.OptionSet;
import net.irisshaders.iris.shaderpack.option.StringOption;
import net.irisshaders.iris.shaderpack.option.Profile;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuBooleanOptionElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElementScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuLinkElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuOptionElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuProfileElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuStringOptionElement;
import net.irisshaders.iris.shaderpack.option.values.OptionValues;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Shader pack session backed by Oculus' public Iris API and configuration.
 * <p>
 * Pack discovery and selection are reimplemented with Oculus' own
 * {@link net.irisshaders.iris.shaderpack.discovery.ShaderpackDirectoryManager} and
 * {@link IrisConfig}; applying a selection goes through
 * {@link net.irisshaders.iris.api.v0.IrisApiConfig#setShadersEnabledAndApply(boolean)}, which
 * saves the config and reloads the pipeline on the render thread. Per-pack shader options stay in
 * Oculus' native settings screen, opened from this page.
 */
final class OculusShaderState implements ShaderState {
    private static final Logger LOGGER = LogManager.getLogger("ModernEmb/Oculus");
    private static final Object PROFILE_ENTRY = new Object();

    private final List<String> packs;
    private String selectedPack;
    private boolean shadersEnabled;
    private String baselinePack;
    private boolean baselineEnabled;
    private @Nullable Runnable changeListener;

    private final Map<String, Boolean> booleanDraft = new LinkedHashMap<>();
    private final Map<String, Boolean> booleanBaseline = new LinkedHashMap<>();
    private final Map<String, String> booleanComments = new LinkedHashMap<>();
    private final Map<String, String> stringDraft = new LinkedHashMap<>();
    private final Map<String, String> stringBaseline = new LinkedHashMap<>();
    private final Map<String, String> stringComments = new LinkedHashMap<>();
    private final Map<String, List<String>> stringAllowedValues = new LinkedHashMap<>();
    private final Map<String, Boolean> stringSliders = new LinkedHashMap<>();
    private final List<Profile> profiles = new ArrayList<>();
    private String selectedProfile = "";
    private boolean hasOptions;
    private @Nullable OptionMenuContainer menu;
    private @Nullable ShaderCategory categoryRoot;

    OculusShaderState() {
        IrisConfig config = Iris.getIrisConfig();
        this.selectedPack = config.getShaderPackName().orElse("");
        this.shadersEnabled = config.areShadersEnabled();
        this.baselinePack = this.selectedPack;
        this.baselineEnabled = this.shadersEnabled;

        List<String> discovered = new ArrayList<>();
        try {
            discovered.addAll(Iris.getShaderpacksDirectoryManager().enumerate());
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to list Oculus shader packs", throwable);
        }
        if (!this.selectedPack.isBlank() && !discovered.contains(this.selectedPack)) {
            discovered.add(this.selectedPack);
        }
        Collections.sort(discovered);
        this.packs = List.copyOf(discovered);
        this.loadOptions();
    }

    @Override
    public boolean hasChanges() {
        return !Objects.equals(this.selectedPack, this.baselinePack)
                || this.shadersEnabled != this.baselineEnabled
                || !this.booleanDraft.equals(this.booleanBaseline)
                || !this.stringDraft.equals(this.stringBaseline);
    }

    @Override
    public void applyOnRenderThread() {
        LOGGER.info("Applying Oculus shader state: enabled={}, pack='{}'", this.shadersEnabled, this.selectedPack);
        IrisConfig config = Iris.getIrisConfig();
        String currentPack = config.getShaderPackName().orElse("");
        boolean packChanging = !this.selectedPack.isBlank() && !this.selectedPack.equals(currentPack);

        if (packChanging) {
            Iris.clearShaderPackOptionQueue();
            config.setShaderPackName(this.selectedPack);
        } else if (this.hasOptions) {
            Iris.clearShaderPackOptionQueue();
            Profile selected = this.findProfile(this.selectedProfile);
            if (selected != null) {
                // Use Oculus' own profile queue so profile-specific disabled programs are applied
                // too, not just the option values.
                Iris.queueShaderPackOptionsFromProfile(selected);
            } else {
                Map<String, String> queue = Iris.getShaderPackOptionQueue();
                for (Map.Entry<String, Boolean> entry : this.booleanDraft.entrySet()) {
                    queue.put(entry.getKey(), Boolean.toString(entry.getValue()));
                }
                for (Map.Entry<String, String> entry : this.stringDraft.entrySet()) {
                    queue.put(entry.getKey(), entry.getValue());
                }
            }
        }

        IrisApi.getInstance().getConfig().setShadersEnabledAndApply(this.shadersEnabled);

        this.baselinePack = this.selectedPack;
        this.baselineEnabled = this.shadersEnabled;
        this.loadOptions();
    }

    @Override
    public void reset() {
        IrisConfig config = Iris.getIrisConfig();
        this.selectedPack = config.getShaderPackName().orElse("");
        this.shadersEnabled = config.areShadersEnabled();
        this.baselinePack = this.selectedPack;
        this.baselineEnabled = this.shadersEnabled;
        this.loadOptions();
    }

    private void loadOptions() {
        this.booleanDraft.clear();
        this.booleanBaseline.clear();
        this.booleanComments.clear();
        this.stringDraft.clear();
        this.stringBaseline.clear();
        this.stringComments.clear();
        this.stringAllowedValues.clear();
        this.stringSliders.clear();
        this.profiles.clear();
        this.selectedProfile = "";
        this.hasOptions = false;
        this.menu = null;
        this.categoryRoot = null;

        try {
            ShaderPack pack = Iris.getCurrentPack().orElse(null);
            if (pack == null) {
                return;
            }

            OptionMenuContainer container = pack.getMenuContainer();
            this.menu = container;
            OptionSet set = pack.getShaderPackOptions().getOptionSet();
            OptionValues values = pack.getShaderPackOptions().getOptionValues();

            if (container != null) {
                // Only options present in the pack's own menu are meaningful to the player. Options
                // outside that menu are internal/unused and would otherwise become a huge "Other"
                // bucket.
                this.categoryRoot = this.buildCategoryTree(container, values);
            } else {
                for (Map.Entry<String, MergedBooleanOption> entry : set.getBooleanOptions().entrySet()) {
                    this.addBooleanOption(entry.getKey(), entry.getValue().getOption(), values);
                }
                for (Map.Entry<String, MergedStringOption> entry : set.getStringOptions().entrySet()) {
                    this.addStringOption(entry.getKey(), entry.getValue().getOption(), values);
                }
            }

            if (container != null) {
                container.getProfiles().forEach((name, profile) -> this.profiles.add(profile));
                if (!this.profiles.isEmpty()) {
                    var result = container.getProfiles().scan(set, values);
                    this.selectedProfile = result.current.map(profile -> profile.name).orElse("");
                }
            }

            this.hasOptions = !this.booleanDraft.isEmpty() || !this.stringDraft.isEmpty();
        } catch (Throwable throwable) {
            LOGGER.warn("Failed to read Oculus shader options", throwable);
            this.booleanDraft.clear();
            this.booleanBaseline.clear();
            this.booleanComments.clear();
            this.stringDraft.clear();
            this.stringBaseline.clear();
            this.stringComments.clear();
            this.stringAllowedValues.clear();
            this.categoryRoot = null;
            this.hasOptions = false;
        }
    }

    private void addBooleanOption(@NotNull String key, @NotNull BooleanOption option, @NotNull OptionValues values) {
        if (this.booleanDraft.containsKey(key)) {
            return;
        }
        boolean value = values.getBooleanValueOrDefault(key);
        this.booleanDraft.put(key, value);
        this.booleanBaseline.put(key, value);
        this.booleanComments.put(key, option.getComment().orElse(""));
    }

    private void addStringOption(@NotNull String key, @NotNull StringOption option, @NotNull OptionValues values) {
        if (this.stringDraft.containsKey(key)) {
            return;
        }
        this.stringDraft.put(key, values.getStringValueOrDefault(key));
        this.stringBaseline.put(key, values.getStringValueOrDefault(key));
        this.stringComments.put(key, option.getComment().orElse(""));
        List<String> allowed = option.getAllowedValues();
        this.stringAllowedValues.put(key, allowed == null ? List.of() : allowed);
    }

    private @NotNull ShaderCategory buildCategoryTree(@NotNull OptionMenuContainer container, @NotNull OptionValues values) {
        ShaderCategory root = new ShaderCategory(label("modernemb.embeddium.shaders.general"), "", false);
        Set<OptionMenuElementScreen> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> used = new LinkedHashSet<>();

        OptionMenuElementScreen main = container.mainScreen;
        if (main != null) {
            visited.add(main);
            this.collectCategory(container, main, root, visited, used, values);
        }

        for (Map.Entry<String, OptionMenuElementScreen> entry : container.subScreens.entrySet()) {
            OptionMenuElementScreen screen = entry.getValue();
            if (screen == null || !visited.add(screen)) {
                continue;
            }

            ShaderCategory child = new ShaderCategory(this.screenLabel(entry.getKey()),
                    this.screenComment(entry.getKey()), isInformationScreen(entry.getKey()));
            this.collectCategory(container, screen, child, visited, used, values);
            if (child.count() > 0) {
                root.children.add(child);
            }
        }
        return root;
    }

    private void collectCategory(@NotNull OptionMenuContainer container, @Nullable OptionMenuElementScreen screen,
                                 @NotNull ShaderCategory category, @NotNull Set<OptionMenuElementScreen> visited,
                                 @NotNull Set<String> used, @NotNull OptionValues fallbackValues) {
        if (screen == null) {
            return;
        }

        for (OptionMenuElement element : screen.elements) {
            if (element instanceof OptionMenuOptionElement option) {
                String key = this.addOptionElement(option, fallbackValues);
                if (key != null && used.add(key)) {
                    category.options.add(key);
                    category.entries.add(key);
                }
            } else if (element instanceof OptionMenuProfileElement) {
                if (!category.entries.contains(PROFILE_ENTRY)) {
                    category.entries.add(PROFILE_ENTRY);
                }
            } else if (element instanceof OptionMenuLinkElement link) {
                OptionMenuElementScreen childScreen = container.subScreens.get(link.targetScreenId);
                if (childScreen != null && visited.add(childScreen)) {
                    ShaderCategory child = new ShaderCategory(this.screenLabel(link.targetScreenId),
                            this.screenComment(link.targetScreenId), isInformationScreen(link.targetScreenId));
                    this.collectCategory(container, childScreen, child, visited, used, fallbackValues);
                    if (child.count() > 0) {
                        category.children.add(child);
                        category.entries.add(child);
                    }
                }
            }
        }
    }

    private @Nullable String addOptionElement(@NotNull OptionMenuOptionElement element, @NotNull OptionValues fallbackValues) {
        OptionValues values = element.getAppliedOptionValues();
        if (values == null) {
            values = fallbackValues;
        }

        if (element instanceof OptionMenuBooleanOptionElement bool && bool.option != null) {
            this.addBooleanOption(bool.option.getName(), bool.option, values);
            return bool.option.getName();
        }
        if (element instanceof OptionMenuStringOptionElement text && text.option != null) {
            this.addStringOption(text.option.getName(), text.option, values);
            this.stringSliders.put(text.option.getName(), text.slider);
            return text.option.getName();
        }
        return null;
    }

    @Override
    public void buildPage(@NotNull MuiSettingsPage ui) {
        // Pick up changes made in Oculus' native screen while this page was not being edited.
        if (!this.hasChanges()) {
            this.reset();
        }

        ui.section(section -> {
            section.toggle(label("modernemb.embeddium.shaders.enable"), label("modernemb.embeddium.shaders.enable_hint"),
                            () -> this.shadersEnabled, this::setShadersEnabled)
                    .enabledWhen(() -> !this.packs.isEmpty());

            section.dropdownBound(label("modernemb.embeddium.shaders.pack"), label("modernemb.embeddium.shaders.pack_hint"),
                    this.choices(), () -> this.selectedPack, this::setSelectedPack);
        });

        this.buildShaderOptions(ui);

        ui.section(section -> section.customLayout(column -> {
            Context context = section.page().context();
            MuiTheme theme = section.page().theme();

            Button settings = MuiControls.button(context, theme,
                    label("modernemb.embeddium.shaders.advanced"), true,
                    view -> this.openNativeSettings());
            column.add(settings, MuiControls.fill());

            Button folder = MuiControls.button(context, theme,
                    label("modernemb.embeddium.shaders.folder"), true,
                    view -> this.openPackFolder());
            LinearLayout.LayoutParams folderParams = MuiControls.fill();
            folderParams.topMargin = MuiControls.dp(context, 10);
            column.add(folder, folderParams);
        }));

        if (this.packs.isEmpty()) {
            ui.text(label("modernemb.embeddium.shaders.empty"));
        }
    }

    private List<MuiOption<String>> choices() {
        List<MuiOption<String>> choices = new ArrayList<>();
        choices.add(MuiOption.of("", label("modernemb.embeddium.shaders.off")));

        for (String pack : this.packs) {
            choices.add(MuiOption.of(pack, pack));
        }

        // Keep an externally selected pack visible even if it was not in the initial directory scan.
        if (!this.selectedPack.isBlank() && !this.packs.contains(this.selectedPack)) {
            choices.add(MuiOption.of(this.selectedPack, this.selectedPack));
        }
        return choices;
    }

    private void setSelectedPack(@NotNull String pack) {
        this.selectedPack = pack;
        if (pack.isBlank()) {
            this.shadersEnabled = false;
        }
        this.notifyEdited();
    }

    private void setShadersEnabled(boolean enabled) {
        this.shadersEnabled = enabled;
        if (enabled && this.selectedPack.isBlank() && !this.packs.isEmpty()) {
            this.selectedPack = this.packs.get(0);
        }
        this.notifyEdited();
    }

    private void buildShaderOptions(@NotNull MuiSettingsPage ui) {
        if (!this.hasOptions) {
            ui.text(label("modernemb.embeddium.shaders.no_options"));
            return;
        }

        if (!this.editingCurrentPack()) {
            ui.text(label("modernemb.embeddium.shaders.apply_pack_first"));
        }

        if (this.categoryRoot != null && this.categoryRoot.count() > 0) {
            this.addCategoryPage(ui, this.categoryRoot);
        } else {
            // Packs without a menu container fall back to two simple cards.
            if (!this.booleanDraft.isEmpty()) {
                ui.collapsibleSection(label("modernemb.embeddium.shaders.toggles"),
                        this.optionCount(this.booleanDraft.size()), true,
                        section -> {
                            for (String key : this.booleanDraft.keySet()) {
                                this.addOptionRow(section, key);
                            }
                        });
            }
            if (!this.stringDraft.isEmpty()) {
                ui.collapsibleSection(label("modernemb.embeddium.shaders.values"),
                        this.optionCount(this.stringDraft.size()), true,
                        section -> {
                            for (String key : this.stringDraft.keySet()) {
                                this.addOptionRow(section, key);
                            }
                        });
            }
        }
    }

    private void addCategoryPage(@NotNull MuiSettingsPage ui, @NotNull ShaderCategory root) {
        boolean hasDirect = false;
        for (Object entry : root.entries) {
            if (!(entry instanceof ShaderCategory)) {
                hasDirect = true;
                break;
            }
        }

        boolean generalRendered = false;
        Set<ShaderCategory> rendered = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Object entry : root.entries) {
            if (entry instanceof ShaderCategory child) {
                if (child.count() == 0) {
                    continue;
                }
                if (!generalRendered && hasDirect) {
                    this.renderGeneralCategory(ui, root);
                    generalRendered = true;
                }
                ui.collapsibleSection(child.title, this.categorySummary(child), false,
                        section -> this.addCategoryBody(section, child));
                rendered.add(child);
            } else if (!generalRendered && hasDirect) {
                this.renderGeneralCategory(ui, root);
                generalRendered = true;
            }
        }

        if (!generalRendered && hasDirect) {
            this.renderGeneralCategory(ui, root);
        }

        // Screens that were never linked from the main screen still belong at the top level.
        for (ShaderCategory child : root.children) {
            if (child.count() == 0 || rendered.contains(child)) {
                continue;
            }
            ui.collapsibleSection(child.title, this.categorySummary(child), false,
                    section -> this.addCategoryBody(section, child));
        }
    }

    private void renderGeneralCategory(@NotNull MuiSettingsPage ui, @NotNull ShaderCategory root) {
        ui.collapsibleSection(root.title, this.optionCount(root.options.size()), true,
                section -> this.addCategoryDirect(section, root));
    }

    private @NotNull String categorySummary(@NotNull ShaderCategory category) {
        if (!category.description.isEmpty()) {
            return category.description;
        }
        return this.optionCount(category.count());
    }

    private void addCategoryDirect(@NotNull MuiSettingsSection section, @NotNull ShaderCategory category) {
        for (Object entry : category.entries) {
            if (entry instanceof ShaderCategory) {
                continue;
            }
            if (entry == PROFILE_ENTRY) {
                this.addProfileRow(section);
            } else if (entry instanceof String key) {
                this.addCategoryOption(section, category, key);
            }
        }
    }

    private void addCategoryBody(@NotNull MuiSettingsSection section, @NotNull ShaderCategory category) {
        for (Object entry : category.entries) {
            if (entry == PROFILE_ENTRY) {
                this.addProfileRow(section);
            } else if (entry instanceof String key) {
                this.addCategoryOption(section, category, key);
            } else if (entry instanceof ShaderCategory child && child.count() > 0) {
                section.collapsible(child.title, this.categorySummary(child), false,
                        nested -> this.addCategoryBody(nested, child));
            }
        }
    }

    private void addCategoryOption(@NotNull MuiSettingsSection section, @NotNull ShaderCategory category, @NotNull String key) {
        if (category.information) {
            String body = this.optionComment(key);
            section.note(this.optionLabel(key), body.isEmpty() ? null : body);
        } else {
            this.addOptionRow(section, key);
        }
    }

    private void addProfileRow(@NotNull MuiSettingsSection section) {
        if (this.profiles.isEmpty()) {
            return;
        }
        section.dropdownBound(
                        label("modernemb.embeddium.shaders.profile"),
                        label("modernemb.embeddium.shaders.profile_hint"),
                        this.profileChoices(), () -> this.selectedProfile, this::selectProfile)
                .enabledWhen(this::editingCurrentPack);
    }

    private void addOptionRow(@NotNull MuiSettingsSection section, @NotNull String key) {
        if (this.booleanDraft.containsKey(key)) {
            section.toggle(this.optionLabel(key), this.optionComment(key),
                            () -> this.booleanDraft.getOrDefault(key, false),
                            value -> {
                                this.booleanDraft.put(key, value);
                                this.selectedProfile = "";
                                this.notifyEdited();
                            })
                    .enabledWhen(this::editingCurrentPack);
            return;
        }

        if (!this.stringDraft.containsKey(key)) {
            return;
        }

        String comment = this.optionComment(key);
        List<String> allowed = this.stringAllowedValues.get(key);
        boolean slider = Boolean.TRUE.equals(this.stringSliders.get(key));

        // Oculus only renders a slider when the option id is listed in shaders.properties'
        // "sliders" set. Numeric-looking values such as SHADER_STYLE/RP_MODE are buttons in the
        // native UI, so never guess a slider from numeric values alone.
        if (slider && allowed != null && allowed.size() > 1 && isNumeric(allowed)) {
            int max = allowed.size() - 1;
            section.slider(this.optionLabel(key), comment, 0, max,
                            () -> this.indexOf(key, allowed),
                            value -> {
                                this.stringDraft.put(key, allowed.get(clamp(value, 0, max)));
                                this.selectedProfile = "";
                                this.notifyEdited();
                            },
                            value -> this.optionValueLabel(key, allowed.get(clamp(value, 0, max))))
                    .enabledWhen(this::editingCurrentPack);
        } else if (allowed != null && !allowed.isEmpty()) {
            List<MuiOption<String>> choices = new ArrayList<>();
            for (String value : allowed) {
                if (value != null) {
                    choices.add(MuiOption.of(value, this.optionValueLabel(key, value)));
                }
            }
            String current = this.stringDraft.getOrDefault(key, "");
            if (choices.stream().noneMatch(option -> Objects.equals(option.value(), current))) {
                choices.add(MuiOption.of(current, this.optionValueLabel(key, current)));
            }

            section.dropdownBound(this.optionLabel(key), comment, choices,
                            () -> this.stringDraft.getOrDefault(key, ""),
                            value -> {
                                this.stringDraft.put(key, value);
                                this.selectedProfile = "";
                                this.notifyEdited();
                            })
                    .enabledWhen(this::editingCurrentPack);
        } else {
            section.textField(this.optionLabel(key), comment,
                            () -> this.stringDraft.getOrDefault(key, ""),
                            value -> {
                                this.stringDraft.put(key, value);
                                this.selectedProfile = "";
                                this.notifyEdited();
                            })
                    .enabledWhen(this::editingCurrentPack);
        }
    }

    private int indexOf(@NotNull String key, @NotNull List<String> allowed) {
        String current = this.stringDraft.getOrDefault(key, "");
        for (int i = 0; i < allowed.size(); i++) {
            if (Objects.equals(allowed.get(i), current)) {
                return i;
            }
        }
        return 0;
    }

    private @NotNull String optionCount(int count) {
        return label("modernemb.embeddium.shaders.option_count").replace("%s", Integer.toString(count));
    }

    private @NotNull String optionLabel(@NotNull String key) {
        String translationKey = "option." + key;
        // Read the raw language value: Component#getString() flattens styles and would discard
        // the legacy § color codes before MuiFormatting can translate them into spans.
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        return translated.equals(translationKey) ? pretty(key) : translated;
    }

    private @NotNull String optionValueLabel(@NotNull String key, @NotNull String value) {
        String translationKey = "value." + key + "." + value;
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        if (!translated.equals(translationKey)) {
            return translated;
        }

        translationKey = "option." + key + "." + value;
        translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        String label = translated.equals(translationKey) ? value : translated;
        return this.optionAffix("prefix." + key) + label + this.optionAffix("suffix." + key);
    }

    private @NotNull String optionAffix(@NotNull String translationKey) {
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        return translated.equals(translationKey) ? "" : translated;
    }

    private @NotNull String profileLabel(@NotNull String name) {
        String translationKey = "profile." + name;
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        return translated.equals(translationKey) ? name : translated;
    }

    private @NotNull String optionComment(@NotNull String key) {
        String translationKey = "option." + key + ".comment";
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        if (!translated.equals(translationKey)) {
            return this.formatComment(translated);
        }

        String raw = this.booleanComments.get(key);
        if (raw == null) {
            raw = this.stringComments.get(key);
        }
        return raw == null ? "" : this.formatComment(raw);
    }

    private @NotNull String formatComment(@NotNull String comment) {
        // Oculus comments use " - " as a bullet separator on a single line. Split them so the
        // information rows wrap at the same places as the native comments instead of running on.
        return comment.replace(" - ", "\n- ");
    }

    private static boolean isInformationScreen(@Nullable String id) {
        return id != null && id.toLowerCase(Locale.ROOT).contains("information");
    }

    private @NotNull String screenLabel(@Nullable String id) {
        if (id == null || id.isBlank() || "<empty>".equals(id)) {
            return label("modernemb.embeddium.shaders.general");
        }

        String translationKey = "screen." + id;
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        return translated.equals(translationKey) ? pretty(id) : translated;
    }

    private @NotNull String screenComment(@Nullable String id) {
        if (id == null || id.isBlank() || "<empty>".equals(id)) {
            return "";
        }
        String translationKey = "screen." + id + ".comment";
        String translated = net.minecraft.locale.Language.getInstance().getOrDefault(translationKey);
        return translated.equals(translationKey) ? "" : this.formatComment(translated);
    }

    private static boolean isNumeric(@NotNull List<String> values) {
        for (String value : values) {
            if (value == null) {
                return false;
            }
            try {
                Double.parseDouble(value.trim());
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return true;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private List<MuiOption<String>> profileChoices() {
        List<MuiOption<String>> choices = new ArrayList<>();
        choices.add(MuiOption.of("", label("modernemb.embeddium.shaders.profile_custom")));

        for (Profile profile : this.profiles) {
            choices.add(MuiOption.of(profile.name, this.profileLabel(profile.name)));
        }
        return choices;
    }

    private @Nullable Profile findProfile(@NotNull String name) {
        for (Profile profile : this.profiles) {
            if (profile.name.equals(name)) {
                return profile;
            }
        }
        return null;
    }

    private void selectProfile(@NotNull String name) {
        if (name.isBlank()) {
            this.selectedProfile = "";
            this.notifyEdited();
            return;
        }

        Profile selected = this.findProfile(name);
        if (selected == null || selected.optionValues == null) {
            return;
        }

        for (Map.Entry<String, String> entry : selected.optionValues.entrySet()) {
            String key = entry.getKey();
            if (this.booleanDraft.containsKey(key)) {
                this.booleanDraft.put(key, Boolean.parseBoolean(entry.getValue()));
            } else if (this.stringDraft.containsKey(key)) {
                this.stringDraft.put(key, entry.getValue());
            }
        }
        this.selectedProfile = name;
        this.notifyEdited();
    }

    private boolean editingCurrentPack() {
        try {
            String current = Iris.getIrisConfig().getShaderPackName().orElse("");
            return this.selectedPack.isBlank() || this.selectedPack.equals(current);
        } catch (Throwable throwable) {
            return true;
        }
    }

    private static @NotNull String pretty(@NotNull String key) {
        String text = key.replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ROOT);
        if (text.isEmpty()) {
            return key;
        }

        StringBuilder result = new StringBuilder(text.length());
        for (String word : text.split("\\s+")) {
            if (word.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    private void notifyEdited() {
        Runnable listener = this.changeListener;
        if (listener != null) {
            listener.run();
        }
    }

    @Override
    public void setChangeListener(@NotNull Runnable listener) {
        this.changeListener = listener;
    }

    /**
     * Opens Oculus' native shader screen. It still owns the per-pack option editor; this page owns
     * pack selection and the global enabled state.
     */
    private void openNativeSettings() {
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.screen;

        // The native screen replaces the Modern UI shell. Keep the shell model alive so returning
        // from Oculus restores the same screen instead of the "Configuration unavailable" view.
        if (parent instanceof MuiScreen muiScreen
                && muiScreen.getFragment() instanceof MuiConfigFragment fragment) {
            fragment.retainModelForChildScreen();
        }

        Runnable task = () -> {
            try {
                Object opened = IrisApi.getInstance().openMainIrisScreenObj(parent);
                if (opened instanceof Screen screen) {
                    client.setScreen(screen);
                }
            } catch (Throwable throwable) {
                LOGGER.error("Failed to open the Oculus shader settings screen", throwable);
            }
        };

        if (client.isSameThread()) {
            task.run();
        } else {
            client.execute(task);
        }
    }

    private void openPackFolder() {
        CompletableFuture.runAsync(() -> {
            try {
                Util.getPlatform().openUri(Iris.getShaderpacksDirectory().toUri().toString());
            } catch (Throwable throwable) {
                LOGGER.warn("Failed to open the shaderpacks folder", throwable);
            }
        });
    }

    private static @NotNull String label(@NotNull String key) {
        return Component.translatable(key).getString();
    }

    private static final class ShaderCategory {
        private final String title;
        private final String description;
        private final boolean information;
        private final List<Object> entries = new ArrayList<>();
        private final List<String> options = new ArrayList<>();
        private final List<ShaderCategory> children = new ArrayList<>();

        private ShaderCategory(@NotNull String title, @NotNull String description, boolean information) {
            this.title = title;
            this.description = description;
            this.information = information;
        }

        private int count() {
            int total = this.options.size();
            for (ShaderCategory child : this.children) {
                total += child.count();
            }
            return total;
        }
    }
}