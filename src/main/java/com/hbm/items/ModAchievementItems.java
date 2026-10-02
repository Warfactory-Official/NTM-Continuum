package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The icon-only items 1.7 kept for its achievement page: {@code achievement_icon} (an {@code ItemEnumMulti} over
 * {@code ItemEnums.EnumAchievementType}, 1.7 ModItems:4377, no creative tab) and {@code nothing} (a plain
 * {@code Item}, 1.7 ModItems:4374, the "Literally impossible" icon). 1.21 has no metadata, so each enum variant is
 * its own item, {@code achievement_icon_<variant>}, as {@link ModIntermediateItems} does for the other enum
 * families. Nothing uses them but the advancement tree's icons (and Bobmazon's hidden-catalogue tab).
 * Each is a flat sprite ({@code achievement_icon.<variant>}, {@code nothing}), stated on its registration.
 */
public final class ModAchievementItems {

    private ModAchievementItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    /** 1.7 {@code ItemEnums.EnumAchievementType}, in ordinal order. */
    public enum Type {
        GOFISH, ACID, BALLS, DIGAMMASEE, DIGAMMAFEEL, DIGAMMAKNOW, DIGAMMAKAUAIMOHO, DIGAMMAUPONTOP,
        DIGAMMAFOROURRIGHT, QUESTIONMARK;

        public String id() {
            return "achievement_icon_" + name().toLowerCase(Locale.US);
        }
    }

    private static final Map<Type, RegistryObject<Item>> ICONS = new EnumMap<>(Type.class);

    public static final RegistryObject<Item> nothing = REG.item("nothing").transform(Presets.sprite());

    static {
        for (Type type : Type.values()) {
            ICONS.put(type, REG.item(type.id())
                    .transform(Presets.sprite("hbm:items/achievement_icon." + type.name().toLowerCase(Locale.US))));
        }
    }

    public static Item icon(Type type) {
        return ICONS.get(type).get();
    }

    public static void init() {}
}
