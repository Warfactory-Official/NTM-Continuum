package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/** The portable portion of the 1.12.2 cyclotron recipe table. */
public final class CyclotronRecipes {

    private CyclotronRecipes() {}

    public static final List<CyclotronRecipe> recipes = new ArrayList<>();
    private static boolean initialized;

    public static final class CyclotronRecipe {
        public final ComparableStack particle;
        public final AStack input;
        public final ItemStack output;
        public final int antimatter;

        private CyclotronRecipe(ComparableStack particle, AStack input, ItemStack output, int antimatter) {
            this.particle = particle;
            this.input = input;
            this.output = output;
            this.antimatter = antimatter;
        }
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // Lithium particle, 50 mB antimatter per conversion.
        add(ModItems.part_lithium.get(), LI.dust(), ModItems.powder_beryllium.get(), 50);
        add(ModItems.part_lithium.get(), BE.dust(), ModItems.powder_boron.get(), 50);
        add(ModItems.part_lithium.get(), B.dust(), ModFuelItems.powder_coal.get(), 50);
        add(ModItems.part_lithium.get(), dustTag("quartz"), ModFuelItems.powder_fire.get(), 50);
        // 1.7 OreDictStack("dustPhosphorus"): HBM 1.7 registers nothing under that name (its phosphorus dicts are
        // dustRedPhosphorus / ingotWhitePhosphorus), so like 1.7 this matches only another mod's phosphorus dust.
        add(ModItems.part_lithium.get(), dustTag("phosphorus"), ModItems.sulfur.get(), 50);
        add(ModItems.part_lithium.get(), IRON.dust(), ModItems.powder_cobalt.get(), 50);
        add(ModItems.part_lithium.get(), exact(ModItems.powder_strontium.get()), ModItems.powder_zirconium.get(), 50);
        // 1.7 ingot_mercury is registered as nugget_mercury (its nugget_mercury field is nugget_mercury_tiny).
        add(ModItems.part_lithium.get(), GOLD.dust(), ModItems.nugget_mercury.get(), 50);
        add(ModItems.part_lithium.get(), PO210.dust(), ModItems.powder_astatine.get(), 50);
        add(ModItems.part_lithium.get(), LA.dust(), ModItems.powder_cerium.get(), 50);
        add(ModItems.part_lithium.get(), AC227.dust(), ModItems.powder_thorium.get(), 50);
        add(ModItems.part_lithium.get(), U.dust(), ModItems.powder_neptunium.get(), 50);
        add(ModItems.part_lithium.get(), NP237.dust(), ModItems.powder_plutonium.get(), 50);

        // Beryllium particle, 25 mB antimatter per conversion.
        add(ModItems.part_beryllium.get(), LI.dust(), ModItems.powder_boron.get(), 25);
        add(ModItems.part_beryllium.get(), dustTag("quartz"), ModItems.sulfur.get(), 25);
        add(ModItems.part_beryllium.get(), TI.dust(), ModItems.powder_iron.get(), 25);
        add(ModItems.part_beryllium.get(), CO.dust(), ModItems.powder_copper.get(), 25);
        add(ModItems.part_beryllium.get(), exact(ModItems.powder_strontium.get()), ModItems.powder_niobium.get(), 25);
        add(ModItems.part_beryllium.get(), exact(ModItems.powder_cerium.get()), ModItems.powder_neodymium.get(), 25);
        add(ModItems.part_beryllium.get(), TH232.dust(), ModItems.powder_uranium.get(), 25);

        // Carbon particle, 10 mB antimatter per conversion.
        add(ModItems.part_carbon.get(), B.dust(), ModItems.powder_aluminium.get(), 10);
        add(ModItems.part_carbon.get(), S.dust(), ModItems.powder_titanium.get(), 10);
        add(ModItems.part_carbon.get(), TI.dust(), ModItems.powder_cobalt.get(), 10);
        add(ModItems.part_carbon.get(), exact(ModItems.powder_caesium.get()), ModItems.powder_lanthanium.get(), 10);
        add(ModItems.part_carbon.get(), exact(ModItems.powder_neodymium.get()), ModItems.powder_gold.get(), 10);
        add(ModItems.part_carbon.get(), exact(ModItems.nugget_mercury.get()), ModItems.powder_polonium.get(), 10);
        add(ModItems.part_carbon.get(), PB.dust(), ModItems.powder_ra226.get(), 10);
        add(ModItems.part_carbon.get(), exact(ModItems.powder_astatine.get()), ModItems.powder_actinium.get(), 10);

        // Copper particle, 15 mB antimatter per conversion.
        add(ModItems.part_copper.get(), BE.dust(), ModItems.powder_quartz.get(), 15);
        add(ModItems.part_copper.get(), dustTag("coal"), ModItems.powder_bromine.get(), 15);
        add(ModItems.part_copper.get(), TI.dust(), ModItems.powder_strontium.get(), 15);
        add(ModItems.part_copper.get(), IRON.dust(), ModItems.powder_niobium.get(), 15);
        add(ModItems.part_copper.get(), exact(ModItems.powder_bromine.get()), ModItems.powder_iodine.get(), 15);
        add(ModItems.part_copper.get(), exact(ModItems.powder_strontium.get()), ModItems.powder_neodymium.get(), 15);
        add(ModItems.part_copper.get(), exact(ModItems.powder_niobium.get()), ModItems.powder_caesium.get(), 15);
        add(ModItems.part_copper.get(), exact(ModItems.powder_iodine.get()), ModItems.powder_polonium.get(), 15);
        add(ModItems.part_copper.get(), exact(ModItems.powder_caesium.get()), ModItems.powder_actinium.get(), 15);
        add(ModItems.part_copper.get(), GOLD.dust(), ModItems.powder_uranium.get(), 15);

        // Plutonium particle, 100 mB antimatter per conversion.
        add(ModItems.part_plutonium.get(), dustTag("phosphorus"), ModItems.powder_tennessine.get(), 100); // 1.7 dustPhosphorus, as above
        add(ModItems.part_plutonium.get(), PU.dust(), ModItems.powder_tennessine.get(), 100);
        add(ModItems.part_plutonium.get(), exact(ModItems.powder_tennessine.get()), ModItems.powder_australium.get(), 100);
        add(ModItems.part_plutonium.get(), exact(ModItems.pellet_charged.get()), ModItems.nugget_schrabidium.get(), 1_000);

    }

    public static CyclotronRecipe getRecipe(ItemStack particle, ItemStack input) {
        if (particle == null || particle.isEmpty() || input == null || input.isEmpty()) return null;
        if (!initialized) register();
        for (CyclotronRecipe recipe : recipes) {
            if (recipe.particle.matchesRecipe(particle, true) && recipe.input.matchesRecipe(input, true)) return recipe;
        }
        return null;
    }

    public static Object[] getOutput(ItemStack input, ItemStack particle) {
        CyclotronRecipe recipe = getRecipe(particle, input);
        return recipe == null ? null : new Object[]{recipe.output.copy(), recipe.antimatter};
    }

    public static boolean isAnyParticle(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!initialized) register();
        for (CyclotronRecipe recipe : recipes) if (recipe.particle.matchesRecipe(stack, true)) return true;
        return false;
    }

    public static boolean isAnyInput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (!initialized) register();
        for (CyclotronRecipe recipe : recipes) if (recipe.input.matchesRecipe(stack, true)) return true;
        return false;
    }

    private static void add(Item particle, AStack input, Item output, int antimatter) {
        recipes.add(new CyclotronRecipe(new ComparableStack(particle), input, new ItemStack(output), antimatter));
    }

    private static ComparableStack exact(Item item) {
        return new ComparableStack(item);
    }

    private static TagStack dustTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "dusts/" + name)));
    }
}
