package me.kall.modernemb.compat.embeddium;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import net.minecraftforge.fml.ModList;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Groups Embeddium option pages by the mod that registered them.
 * <p>
 * Embeddium assigns every {@link OptionPage} an {@link OptionIdentifier}; its mod id is the mod
 * that created the page (Chloride's pages, for example, are {@code chloride:interface} and
 * {@code chloride:world}). ModernEmb previously flattened the pages into the navigation rail and
 * dropped that information, so feature pages such as "Interface" and "World" could not show their
 * source mod.
 * <p>
 * Pages are grouped only for mods that contributed more than one page. Embeddium's own pages and
 * single-page mods stay as normal navigation entries; grouping a single page would add a pointless
 * extra click. Pages without a usable identifier are rendered as plain entries instead of being
 * hidden.
 */
final class EmbeddiumPageGroups {
    private EmbeddiumPageGroups() {}

    /**
     * Builds the ordered navigation groups.
     *
     * @param pages             pages built by Embeddium
     * @param oculusPlaceholder Oculus' empty shader page, or {@code null}
     * @return groups in the order their first page appeared
     */
    static @NotNull List<Group> build(@NotNull List<OptionPage> pages, @Nullable OptionPage oculusPlaceholder) {
        Map<String, List<OptionPage>> pagesByMod = new LinkedHashMap<>();
        for (OptionPage page : pages) {
            if (page == oculusPlaceholder) continue;

            String modId = modId(page);
            if (modId == null || isBuiltIn(modId)) continue;

            pagesByMod.computeIfAbsent(modId, key -> new ArrayList<>()).add(page);
        }

        List<Group> groups = new ArrayList<>();
        Set<String> emitted = new HashSet<>();
        for (OptionPage page : pages) {
            if (page == oculusPlaceholder) continue;

            String modId = modId(page);
            if (modId == null || isBuiltIn(modId)) {
                groups.add(new Group(null, "Embeddium", List.of(page)));
                continue;
            }

            if (!emitted.add(modId)) continue;
            List<OptionPage> modPages = pagesByMod.getOrDefault(modId, List.of(page));
            groups.add(new Group(modId, displayName(modId), List.copyOf(modPages)));
        }
        return groups;
    }

    private static @Nullable String modId(@NotNull OptionPage page) {
        OptionIdentifier<Void> id = page.getId();
        if (!OptionIdentifier.isPresent(id)) return null;

        String modId = id.getModId();
        return modId == null || modId.isBlank() ? null : modId;
    }

    private static boolean isBuiltIn(@NotNull String modId) {
        return "embeddium".equals(modId) || "sodium".equals(modId) || "minecraft".equals(modId);
    }

    private static @NotNull String displayName(@NotNull String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .filter(name -> !name.isBlank())
                .orElse(modId);
    }

    /**
     * One navigation entry: either a single page, or all pages contributed by one mod.
     *
     * @param modId       source mod id, or {@code null} for Embeddium's own/unknown pages
     * @param displayName source mod display name
     * @param pages       pages rendered by this entry
     */
    record Group(@Nullable String modId, @NotNull String displayName, @NotNull List<OptionPage> pages) {
        /**
         * @return {@code true} when this entry renders multiple pages as collapsible sections
         */
        boolean grouped() {
            return this.pages.size() > 1;
        }
    }
}