package com.hbm.qmaw;

import net.minecraft.client.resources.language.LanguageManager;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/** One resource-backed Quick Manual And Wiki page. */
public final class QuickManualAndWiki {
    public final String name;
    public final ItemStack icon;
    public final Map<String, String> title;
    public final Map<String, String> contents;
    public final List<Trigger> triggers;

    public QuickManualAndWiki(String name, ItemStack icon, Map<String, String> title,
                              Map<String, String> contents, List<Trigger> triggers) {
        this.name = name;
        this.icon = icon;
        this.title = Map.copyOf(title);
        this.contents = Map.copyOf(contents);
        this.triggers = List.copyOf(triggers);
    }

    public String getTitle(LanguageManager lang) {
        String localizedTitle = title.get(lang.getSelected());
        if (localizedTitle == null) localizedTitle = title.get(QMAWLoader.EN_US);
        if (localizedTitle == null) localizedTitle = "Missing Localization!";
        return localizedTitle;
    }

    public record Trigger(ItemStack stack, boolean wildcard, String itemPathPrefix) {
        public Trigger(ItemStack stack, boolean wildcard) {
            this(stack, wildcard, null);
        }

        public Trigger(String itemPathPrefix) {
            this(null, true, itemPathPrefix);
        }
    }
}
