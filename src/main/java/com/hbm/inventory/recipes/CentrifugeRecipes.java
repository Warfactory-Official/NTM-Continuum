package com.hbm.inventory.recipes;

import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * The standard centrifuge recipe table, ported from 1.12.2 {@code CentrifugeRecipes}. The old ore
 * dictionary keys are represented by common item tags; exact metadata variants are separate modern
 * items. Recipes needing content that is not registered in this port are deliberately not added.
 */
public final class CentrifugeRecipes {

    private CentrifugeRecipes() {}

    public static final List<CentrifugeRecipe> recipes = new ArrayList<>();
    private static boolean initialized = false;

    public static final class CentrifugeRecipe {
        public final AStack input;
        public final ItemStack[] output;

        private CentrifugeRecipe(AStack input, ItemStack[] output) {
            this.input = input;
            this.output = output;
        }
    }

    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // Ore processing. These are the default (non-LBSM) 1.12.2 outputs.
        add(oreTag("coal"), fuel(ModFuelItems.powder_coal, 2), fuel(ModFuelItems.powder_coal, 2),
                fuel(ModFuelItems.powder_coal, 2), stack(Items.GRAVEL));
        add(oreTag("lignite"), fuel(ModFuelItems.powder_lignite, 2), fuel(ModFuelItems.powder_lignite, 2),
                fuel(ModFuelItems.powder_lignite, 2), stack(Items.GRAVEL));
        add(IRON.ore(), item(ModItems.powder_iron, 1), item(ModItems.powder_iron, 1),
                item(ModItems.powder_iron, 1), stack(Items.GRAVEL));
        add(GOLD.ore(), item(ModItems.powder_gold, 1), item(ModItems.powder_gold, 1),
                item(ModItems.powder_gold, 1), stack(Items.GRAVEL));
        add(oreTag("diamond"), item(ModItems.powder_diamond, 1), item(ModItems.powder_diamond, 1),
                item(ModItems.powder_diamond, 1), stack(Items.GRAVEL));
        add(oreTag("emerald"), item(ModItems.powder_emerald, 1), item(ModItems.powder_emerald, 1),
                item(ModItems.powder_emerald, 1), stack(Items.GRAVEL));
        add(TI.ore(), item(ModItems.powder_titanium, 1), item(ModItems.powder_titanium, 1),
                item(ModItems.powder_iron, 1), stack(Items.GRAVEL));
        add(exact(Items.NETHER_QUARTZ_ORE), item(ModItems.powder_quartz, 1),
                item(ModItems.powder_quartz, 1), item(ModItems.powder_lithium_tiny, 1), stack(Items.NETHERRACK));
        add(W.ore(), item(ModItems.powder_tungsten, 1), item(ModItems.powder_tungsten, 1),
                item(ModItems.powder_iron, 1), stack(Items.GRAVEL));
        add(CU.ore(), item(ModItems.powder_copper, 1), item(ModItems.powder_copper, 1),
                item(ModItems.powder_gold, 1), stack(Items.GRAVEL));
        add(AL.ore(), ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 2),
                item(ModItems.powder_titanium, 1), item(ModItems.powder_iron, 1), stack(Items.GRAVEL));
        add(PB.ore(), item(ModItems.powder_lead, 1), item(ModItems.powder_lead, 1),
                item(ModItems.powder_gold, 1), stack(Items.GRAVEL));
        add(SA326.ore(), item(ModItems.powder_schrabidium, 1), item(ModItems.powder_schrabidium, 1),
                item(ModItems.nugget_solinium, 1), stack(Items.GRAVEL));
        add(oreTag("rare"), item(ModItems.powder_desh_mix, 1), item(ModItems.nugget_zirconium, 1),
                item(ModItems.nugget_zirconium, 1), stack(Items.GRAVEL));
        add(exact(ModIntermediateItems.chunk(EnumChunkType.RARE)), item(ModItems.powder_cobalt_tiny, 2),
                item(ModItems.powder_boron_tiny, 2), item(ModItems.powder_niobium_tiny, 2),
                item(ModItems.nugget_zirconium, 3));
        add(PU.ore(), item(ModItems.powder_plutonium, 1), item(ModItems.powder_plutonium, 1),
                item(ModItems.nugget_polonium, 3), stack(Items.GRAVEL));
        add(U.ore(), item(ModItems.powder_uranium, 1), item(ModItems.powder_uranium, 1),
                item(ModItems.nugget_ra226, 1), stack(Items.GRAVEL));
        add(TH232.ore(), item(ModItems.powder_thorium, 1), item(ModItems.powder_thorium, 1),
                item(ModItems.powder_uranium, 1), stack(Items.GRAVEL));
        add(BE.ore(), item(ModItems.powder_beryllium, 1), item(ModItems.powder_beryllium, 1),
                item(ModItems.powder_emerald, 1), stack(Items.GRAVEL));
        add(F.ore(), item(ModItems.fluorite, 3), item(ModItems.fluorite, 3),
                item(ModItems.gem_sodalite, 1), stack(Items.GRAVEL));
        add(oreTag("lapis"), item(ModItems.powder_lapis, 6), item(ModItems.powder_cobalt_tiny, 1),
                item(ModItems.gem_sodalite, 1), stack(Items.GRAVEL));
        add(CO.ore(), item(ModItems.powder_cobalt, 2), item(ModItems.powder_iron, 1),
                item(ModItems.powder_copper, 1), stack(Items.GRAVEL));
        add(exact(ModBlocks.ore_tikite.get().asItem()), item(ModItems.powder_plutonium, 1),
                item(ModItems.powder_cobalt, 2), item(ModItems.powder_niobium, 2), stack(Items.END_STONE));
        add(exact(ModBlocks.ore_nether_fire.get().asItem()), stack(Items.BLAZE_POWDER, 2), fuel(ModFuelItems.powder_fire, 2),
                item(ModItems.ingot_phosphorus, 1), stack(Items.NETHERRACK));

        // Non-ore inputs from the default table.
        add(exact(ModItems.powder_tektite), item(ModItems.powder_meteorite_tiny, 1),
                item(ModItems.powder_paleogenite_tiny, 1), item(ModItems.powder_meteorite_tiny, 1),
                item(ModItems.dust, 6));
        add(exact(ModIngredientItems.ash(EnumAshType.COAL)), item(ModFuelItems.powder_coal_tiny, 2),
                item(ModItems.powder_boron_tiny, 1), item(ModItems.dust_tiny, 6));
        add(exact(Items.BLAZE_ROD), stack(Items.BLAZE_POWDER), stack(Items.BLAZE_POWDER),
                fuel(ModFuelItems.powder_fire, 1), fuel(ModFuelItems.powder_fire, 1));
        add(exact(ModFuelItems.ingot_schraranium), item(ModItems.nugget_schrabidium, 2),
                item(ModItems.nugget_schrabidium, 1), item(ModItems.nugget_uranium, 3),
                item(ModItems.nugget_neptunium, 2));
        add(exact(ModItems.centrifuge_element), item(ModItems.powder_cobalt_tiny, 2),
                item(ModItems.powder_boron_tiny, 2), item(ModItems.powder_niobium_tiny, 2),
                item(ModItems.nugget_zirconium, 3));

        // Crystal processing.
        add(exact(ModFuelItems.crystal_coal), fuel(ModFuelItems.powder_coal, 3), fuel(ModFuelItems.powder_coal, 3),
                fuel(ModFuelItems.powder_coal, 3), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_iron), item(ModItems.powder_iron, 2), item(ModItems.powder_iron, 2),
                item(ModItems.powder_titanium, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_lapis), item(ModItems.powder_lapis, 4), item(ModItems.powder_lapis, 4),
                item(ModItems.powder_cobalt, 1), item(ModItems.gem_sodalite, 2));
        add(exact(ModItems.crystal_diamond), item(ModItems.powder_diamond, 1), item(ModItems.powder_diamond, 1),
                item(ModItems.powder_diamond, 1), item(ModItems.powder_diamond, 1));
        add(exact(ModItems.crystal_uranium), item(ModItems.powder_uranium, 2), item(ModItems.powder_uranium, 2),
                item(ModItems.nugget_ra226, 2), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_thorium), item(ModItems.powder_thorium, 2), item(ModItems.powder_thorium, 2),
                item(ModItems.powder_uranium, 1), item(ModItems.nugget_ra226, 1));
        add(exact(ModItems.crystal_plutonium), item(ModItems.powder_plutonium, 2), item(ModItems.powder_plutonium, 2),
                item(ModItems.powder_polonium, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_titanium), item(ModItems.powder_titanium, 2), item(ModItems.powder_titanium, 2),
                item(ModItems.powder_iron, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_niter), item(ModItems.niter, 3), item(ModItems.niter, 3),
                item(ModItems.niter, 3), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_copper), item(ModItems.powder_copper, 2), item(ModItems.powder_copper, 2),
                item(ModItems.sulfur, 1), item(ModItems.powder_cobalt_tiny, 1));
        add(exact(ModItems.crystal_tungsten), item(ModItems.powder_tungsten, 2), item(ModItems.powder_tungsten, 2),
                item(ModItems.powder_iron, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_aluminium), ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 3),
                item(ModItems.powder_titanium, 1), item(ModItems.powder_iron, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_fluorite), item(ModItems.fluorite, 4), item(ModItems.fluorite, 4),
                item(ModItems.gem_sodalite, 2), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_beryllium), item(ModItems.powder_beryllium, 2), item(ModItems.powder_beryllium, 2),
                item(ModItems.powder_quartz, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_lead), item(ModItems.powder_lead, 2), item(ModItems.powder_lead, 2),
                item(ModItems.powder_gold, 1), item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_schraranium), item(ModItems.nugget_schrabidium, 2),
                item(ModItems.nugget_schrabidium, 2), item(ModItems.nugget_uranium, 2),
                item(ModItems.nugget_neptunium, 2));
        add(exact(ModItems.crystal_schrabidium), item(ModItems.powder_schrabidium, 2),
                item(ModItems.powder_schrabidium, 2), item(ModItems.powder_plutonium, 1),
                item(ModItems.powder_lithium_tiny, 1));
        add(exact(ModItems.crystal_rare), item(ModItems.powder_desh_mix, 1), item(ModItems.powder_desh_mix, 1),
                item(ModItems.nugget_zirconium, 2), item(ModItems.nugget_zirconium, 2));
        add(exact(ModItems.crystal_phosphorus), fuel(ModFuelItems.powder_fire, 3), fuel(ModFuelItems.powder_fire, 3),
                item(ModItems.ingot_phosphorus, 2), stack(Items.BLAZE_POWDER, 2));
        add(exact(ModItems.crystal_trixite), item(ModItems.powder_plutonium, 2), item(ModItems.powder_cobalt, 3),
                item(ModItems.powder_niobium, 2), item(ModItems.powder_nitan_mix, 1));
        add(exact(ModItems.crystal_lithium), item(ModItems.powder_lithium, 2), item(ModItems.powder_lithium, 2),
                item(ModItems.powder_quartz, 1), item(ModItems.fluorite, 1));
        add(exact(ModItems.crystal_cobalt), item(ModItems.powder_cobalt, 2), item(ModItems.powder_iron, 3),
                item(ModItems.powder_copper, 3), item(ModItems.powder_lithium_tiny, 1));
        BedrockOreChainRecipes.centrifuge(CentrifugeRecipes::add); // 1.7 ItemBedrockOreNew grade chain
    }

    public static ItemStack[] getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        if (!initialized) register();
        for (CentrifugeRecipe recipe : recipes) {
            if (recipe.input.matchesRecipe(stack, true)) {
                ItemStack[] result = new ItemStack[recipe.output.length];
                for (int i = 0; i < result.length; i++) result[i] = recipe.output[i].copy();
                return result;
            }
        }
        return null;
    }

    public static boolean isAnyRecipeInput(ItemStack stack) {
        return getOutput(stack) != null;
    }

    private static void add(AStack input, ItemStack... output) {
        recipes.add(new CentrifugeRecipe(input, output));
    }

    private static ComparableStack exact(Item item) {
        return new ComparableStack(item);
    }

    private static <T extends Item> ComparableStack exact(RegistryObject<T> item) {
        return exact(item.get());
    }

    private static ItemStack item(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static <T extends Item> ItemStack item(RegistryObject<T> item, int count) {
        return new ItemStack(item.get(), count);
    }

    private static ItemStack stack(Item item) {
        return new ItemStack(item);
    }

    private static ItemStack stack(Item item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack fuel(dev.golem.core.registry.RegistryObject<Item> item, int count) {
        return new ItemStack(item.get(), count);
    }

    private static TagStack oreTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", "ores/" + name)));
    }
}
