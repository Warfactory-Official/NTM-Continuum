package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModCasingItems;
import dev.golem.core.Ids;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hbm.inventory.OreDictManager.ANY_PLASTIC;
import static com.hbm.inventory.OreDictManager.CU;
import static com.hbm.inventory.OreDictManager.PB;
import static com.hbm.inventory.OreDictManager.STEEL;
import static com.hbm.inventory.OreDictManager.WEAPONSTEEL;

/** The feasible positional slice of the legacy {@code AmmoPressRecipes} catalogue. */
public final class AmmoPressRecipes {

    public static final List<AmmoPressRecipe> recipes = new ArrayList<>();
    private static final TagKey<Item> ANY_SMOKELESS = TagKey.create(Registries.ITEM,
            Ids.of("c", "dusts/any_smokeless"));
    private static boolean initialized;

    private AmmoPressRecipes() {}

    public static List<AmmoPressRecipe> getRecipes() {
        register();
        return Collections.unmodifiableList(recipes);
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // These are the first four legacy families whose flattened output identities and every input
        // identity are present in this port. The nine entries remain strictly positional.
        add(ModAmmoItems.m357_bp, 16, null, PB.ingot(), null, null, exact(Items.GUNPOWDER), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m357_sp, 8, null, PB.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m357_fmj, 8, null, STEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m357_jhp, 8, ANY_PLASTIC.ingot(), CU.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m357_ap, 8, null, WEAPONSTEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL_STEEL), null);
        add(ModAmmoItems.m357_express, 8, null, STEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);

        add(ModAmmoItems.m44_bp, 12, null, PB.ingot(), null, null, exact(Items.GUNPOWDER), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m44_sp, 6, null, PB.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m44_fmj, 6, null, STEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m44_jhp, 6, ANY_PLASTIC.ingot(), CU.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.m44_ap, 6, null, WEAPONSTEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL_STEEL), null);
        add(ModAmmoItems.m44_express, 6, null, STEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);

        add(ModAmmoItems.p22_sp, 24, null, PB.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.p22_fmj, 24, null, STEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.p22_jhp, 24, ANY_PLASTIC.ingot(), CU.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.p22_ap, 24, null, WEAPONSTEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL_STEEL), null);

        add(ModAmmoItems.p9_sp, 12, null, PB.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.p9_fmj, 12, null, STEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.p9_jhp, 12, ANY_PLASTIC.ingot(), CU.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL), null);
        add(ModAmmoItems.p9_ap, 12, null, WEAPONSTEEL.ingot(), null, null, smokeless(), null, null,
                casing(EnumCasingType.SMALL_STEEL), null);
    }

    private static AStack exact(Item item) {
        return new ComparableStack(item);
    }

    private static AStack smokeless() {
        return new TagStack(ANY_SMOKELESS);
    }

    private static AStack casing(EnumCasingType type) {
        return new ComparableStack(ModCasingItems.casing(type));
    }

    private static void add(RegistryObject<Item> output, int count, AStack... input) {
        if (input.length != 9) throw new IllegalArgumentException("Ammo Press recipes require exactly nine inputs");
        recipes.add(new AmmoPressRecipe(new net.minecraft.world.item.ItemStack(output.get(), count), input));
    }

    public static final class AmmoPressRecipe {
        public final net.minecraft.world.item.ItemStack output;
        public final AStack[] input;

        private AmmoPressRecipe(net.minecraft.world.item.ItemStack output, AStack[] input) {
            this.output = output;
            this.input = input;
        }
    }
}
