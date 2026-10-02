package com.hbm.items;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.blocks.EnergyBedrockDeclarations;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * The rest of the 1.7 {@code ItemBedrockOreNew} ({@code bedrock_ore}, metadata {@code grade << 4 | type}): every
 * processing grade but {@link Grade#BASE}, which the ore slopper family already registers as
 * {@code bedrock_ore_base_<type>}. Flattened the same way, {@code bedrock_ore_<grade>_<type>}, e.g.
 * {@code bedrock_ore_primary_sulfuric_light}, 25 grades by 6 types.
 *
 * <p>The item art is 1.7's: a greyscale sprite per grade prefix and type, recoloured into the type's light/dark
 * band on the atlas ({@code EnergyBedrockClientRegistrations}), multiplied by the grade's tint, with one overlay
 * per processing trait drawn on top.
 */
public final class ModBedrockOreItems {

    private ModBedrockOreItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    private static final Map<Grade, Map<BedrockOreType, RegistryObject<ItemBedrockOreGraded>>> GRADED = new EnumMap<>(Grade.class);

    static {
        for (Grade grade : Grade.values()) {
            if (grade == Grade.BASE) continue;
            Map<BedrockOreType, RegistryObject<ItemBedrockOreGraded>> byType = new EnumMap<>(BedrockOreType.class);
            for (BedrockOreType type : BedrockOreType.VALUES) {
                byType.put(type, REG.item(id(grade, type), p -> new ItemBedrockOreGraded(p, grade, type))
                        .icon(d -> EnergyBedrockDeclarations.gradeModel(d, grade.name().toLowerCase(Locale.ROOT),
                                type.suffix)));
            }
            GRADED.put(grade, byType);
        }
        // 1.7 getSubItems: type-major, every grade of one type before the next (BASE sits with the slopper's items)
        for (BedrockOreType type : BedrockOreType.VALUES) {
            for (Grade grade : Grade.values()) {
                if (grade != Grade.BASE) ModTabs.addTo(HbmTab.MATERIALS, GRADED.get(grade).get(type));
            }
        }
    }

    /** {@code bedrock_ore_<grade>_<type>}. */
    public static String id(Grade grade, BedrockOreType type) {
        return "bedrock_ore_" + grade.name().toLowerCase(Locale.ROOT) + "_" + type.suffix;
    }

    /** 1.7 {@code ItemBedrockOreNew.make(grade, type)}. */
    public static ItemBedrockOreGraded get(Grade grade, BedrockOreType type) {
        if (grade == Grade.BASE) return ModProduction2Blocks.bedrockOreBase(type);
        return GRADED.get(grade).get(type).get();
    }

    /** 1.7 {@code ItemBedrockOreNew.make(grade, type, amount)}. */
    public static ItemStack make(Grade grade, BedrockOreType type, int amount) {
        return new ItemStack(get(grade, type), amount);
    }

    public static ItemStack make(Grade grade, BedrockOreType type) {
        return make(grade, type, 1);
    }

    public static void init() {}
}
