package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingHotRecipe;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemStamp.StampType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.hbm.inventory.OreDictManager.BIGMT;
import static com.hbm.inventory.OreDictManager.CO;

/**
 * The machine recipes of the ability tool family, each handed to its registry from one line in that
 * registry's own registration, so the family's entries live together.
 *
 * <p>The meteorite sword chain, in 1.7 order: crafted, seared (furnace), reforged (anvil), hardened
 * (press), alloyed (blast furnace), machined (assembler), treated (chemical plant), etched
 * (crystallizer), bred (breeder), irradiated (research reactor / ZIRNOX), fused (fusion breeder),
 * baleful (never obtainable in 1.7).
 */
public final class AbilityToolMachineRecipes {

    private AbilityToolMachineRecipes() {}

    private static ComparableStack item(Item item) {
        return new ComparableStack(item);
    }

    /** 1.7 AnvilRecipes:82-91, all tier 3 hot smithing. */
    public static void smithing(List<AnvilSmithingRecipe> recipes) {
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.ingot_meteorite_forged.get()),
                item(ModAbilityToolItems.ingot_meteorite.get()), item(ModAbilityToolItems.ingot_meteorite.get())));
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.blade_meteorite.get()),
                item(ModAbilityToolItems.ingot_meteorite_forged.get()), item(ModAbilityToolItems.ingot_meteorite_forged.get())));
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.meteorite_sword_reforged.get()),
                item(ModAbilityToolItems.meteorite_sword_seared.get()), item(ModAbilityToolItems.ingot_meteorite_forged.get())));

        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_sword.get()),
                item(ModAbilityToolItems.cobalt_sword.get()), item(ModAbilityToolItems.ingot_meteorite.get())));
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_pickaxe.get()),
                item(ModAbilityToolItems.cobalt_pickaxe.get()), item(ModAbilityToolItems.ingot_meteorite.get())));
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_axe.get()),
                item(ModAbilityToolItems.cobalt_axe.get()), item(ModAbilityToolItems.ingot_meteorite.get())));
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_shovel.get()),
                item(ModAbilityToolItems.cobalt_shovel.get()), item(ModAbilityToolItems.ingot_meteorite.get())));
        recipes.add(new AnvilSmithingHotRecipe(3, new ItemStack(ModAbilityToolItems.cobalt_decorated_hoe.get()),
                item(ModAbilityToolItems.cobalt_hoe.get()), item(ModAbilityToolItems.ingot_meteorite.get())));
    }

    /** 1.7 PressRecipes:67. */
    public static void press() {
        PressRecipes.makeRecipe(StampType.FLAT, item(ModAbilityToolItems.meteorite_sword_reforged.get()),
                new ItemStack(ModAbilityToolItems.meteorite_sword_hardened.get()));
    }

    /** 1.7 CrystallizerRecipes:123: peroxide, base time, the default 500 mB. */
    public static void crystallizer(List<CrystallizerRecipe> recipes, int baseTime) {
        recipes.add(new CrystallizerRecipe(item(ModAbilityToolItems.meteorite_sword_treated.get()), Fluids.PEROXIDE, 500,
                baseTime, 0F, new ItemStack(ModAbilityToolItems.meteorite_sword_etched.get()))); // 1.7: no .prod(), so 0
    }

    /** 1.7 BlastFurnaceRecipesNT: the meteor sword, meteorite ingot and starmetal recipes. */
    public static void blastFurnace(BlastFurnaceRecipesNT registry) {
        registry.registerGenerated(new GenericRecipe("blast.meteorSword").setup(1_200, 0)
                .inputItems(CO.ingot(), item(ModAbilityToolItems.meteorite_sword_hardened.get()))
                .outputItems(new ItemStack(ModAbilityToolItems.meteorite_sword_alloyed.get())));
        registry.registerGenerated(new GenericRecipe("blast.meteor").setup(600, 0)
                .inputItems(CO.ingot(), item(ModItems.powder_meteorite.get()))
                .outputItems(new ItemStack(ModAbilityToolItems.ingot_meteorite.get())));
        registry.registerGenerated(new GenericRecipe("blast.starmetal").setup(600, 0)
                .inputItems(BIGMT.ingot(), item(ModAbilityToolItems.ingot_meteorite.get()))
                .outputItems(new ItemStack(ModItems.ingot_starmetal.get())));
    }
}
