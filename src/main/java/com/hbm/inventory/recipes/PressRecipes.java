package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ItemEnums.EnumCircuitType;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.OreDictManager.*;

/**
 * Press recipes — shared by the burner press and the electric press. A recipe is keyed by
 * (ingredient, {@link StampType}): inserting an ingredient plus a stamp of the matching type yields the
 * output. Complete port of the 1.7 {@code PressRecipes} defaults: the flat-stamp table, the meteorite
 * sword ({@link AbilityToolMachineRecipes#press}), the three briquettes and eight printing-press pages
 * ({@link FinalConsumableMachineRecipes#press}), the metal-<b>plate</b> family, the casing family and the
 * material fine-wire loop. The plate set matches {@link AssemblyMachineRecipes} (ingot → plate) so the two
 * machines stay consistent.
 */
public final class PressRecipes {

    private PressRecipes() {}

    /** One press recipe: a stamp type + an ingredient matcher → an output stack. */
    public record PressRecipe(StampType type, AStack input, ItemStack output) {}

    private static final List<PressRecipe> RECIPES = new ArrayList<>();
    private static boolean initialized = false;

    /** All press recipes (for JEI / tooling); triggers lazy {@link #register()} first. Mirrors
     * {@link ShredderRecipes#getRecipes()}. */
    public static List<PressRecipe> getRecipes() {
        if (!initialized) register();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    /**
     * @return the output for {@code ingredient} pressed with {@code stamp}, or {@link ItemStack#EMPTY} if
     * there is no matching recipe (or {@code stamp} is not an {@link ItemStamp}).
     */
    public static ItemStack getOutput(ItemStack ingredient, ItemStack stamp) {
        if (!initialized) register();
        if (ingredient == null || ingredient.isEmpty() || stamp == null || stamp.isEmpty()) return ItemStack.EMPTY;
        if (!(stamp.getItem() instanceof ItemStamp itemStamp)) return ItemStack.EMPTY;

        StampType type = itemStamp.getStampType();
        for (PressRecipe recipe : RECIPES) {
            if (recipe.type == type && recipe.input.matchesRecipe(ingredient, true)) {
                return recipe.output;
            }
        }
        return ItemStack.EMPTY;
    }

    /** Idempotent; recipes reference items so this runs lazily after the registries have flushed. */
    public static synchronized void register() {
        if (initialized) return;
        initialized = true;

        // The non-plate flat/circuit defaults from the 1.7 PressRecipes table.
        AbilityToolMachineRecipes.press(); // reforged -> hardened meteorite sword (1.7 PressRecipes:67)
        FinalConsumableMachineRecipes.press(); // briquettes + the eight printing-press pages (1.7 PressRecipes:70-72, 101-108)
        makeRecipe(StampType.FLAT, dustTag("quartz"), new ItemStack(Items.QUARTZ));
        makeRecipe(StampType.FLAT, dustTag("lapis"), new ItemStack(Items.LAPIS_LAZULI));
        makeRecipe(StampType.FLAT, DIAMOND.dust(), new ItemStack(Items.DIAMOND));
        makeRecipe(StampType.FLAT, EMERALD.dust(), new ItemStack(Items.EMERALD));
        makeRecipe(StampType.FLAT, new ComparableStack(ModFuelItems.biomass.get()),
                new ItemStack(ModFuelItems.biomass_compressed.get()));
        makeRecipe(StampType.FLAT, new TagStack(ANY_COKE), new ItemStack(ModFuelItems.ingot_graphite.get()));
        // Old Blocks.LOG metadata 3 is jungle; do not broaden this to every modern log.
        makeRecipe(StampType.FLAT, new ComparableStack(Blocks.JUNGLE_LOG.asItem()),
                new ItemStack(ModFuelItems.ball_resin.get()));
        makeRecipe(StampType.CIRCUIT, SI.billet(),
                new ItemStack(ModIntermediateItems.circuit(EnumCircuitType.SILICON)));

        // --- plates (ingot -> plate), the feasible slice (matches AssemblyMachineRecipes). Ingredients are
        // material TAGS (c:ingots/<metal>) so any mod's ingot works (the modern ore-dictionary equivalent). ---
        plate(IRON, ModItems.plate_iron.get());
        plate(GOLD, ModItems.plate_gold.get());
        plate(TI, ModItems.plate_titanium.get());
        plate(AL, ModItems.plate_aluminium.get());
        plate(STEEL, ModItems.plate_steel.get());
        plate(PB, ModItems.plate_lead.get());
        plate(CU, ModItems.plate_copper.get());
        plate(GUNMETAL, ModItems.plate_gunmetal.get());
        plate(WEAPONSTEEL, ModItems.plate_weaponsteel.get());
        plate(CMB, ModItems.plate_combine_steel.get());
        plate(DURA, ModItems.plate_dura_steel.get());
        plate(BIGMT, ModItems.plate_saturnite.get());
        plate(SA326, ModItems.plate_schrabidium.get());

        // --- casings (plate -> casing), verbatim old EnumCasingType variants and counts.
        makeRecipe(StampType.C9, GUNMETAL.plate(), ModCasingItems.casing(EnumCasingType.SMALL, 4));
        makeRecipe(StampType.C50, GUNMETAL.plate(), ModCasingItems.casing(EnumCasingType.LARGE, 2));
        makeRecipe(StampType.C9, WEAPONSTEEL.plate(), ModCasingItems.casing(EnumCasingType.SMALL_STEEL, 4));
        makeRecipe(StampType.C50, WEAPONSTEEL.plate(), ModCasingItems.casing(EnumCasingType.LARGE_STEEL, 2));

        // --- fine wire (ingot -> 8 wire) with the WIRE stamp, for every material that auto-generates WIRE
        // and has a ported ingot — a faithful port of the 1.12.2 PressRecipes Mats.orderedList loop. The
        // output is the autogen wire item; the ingot input is the c:ingots/<mat> tag. ---
        for (NTMMaterial mat : Mats.orderedList) {
            Item wire = ModAutogenItems.get(MaterialShapes.WIRE, mat);
            // 1.7 gates on the "ingot<Mat>" ore name; carbon's ingot is ingot_graphite (c:ingots/carbon), not ingot_carbon
            if (wire != null && (AssemblyMachineRecipes.hasItem(MaterialShapes.INGOT, mat) || mat == Mats.MAT_CARBON)) {
                makeRecipe(StampType.WIRE, mat.ingot(), new ItemStack(wire, 8));
            }
        }
    }

    private static void plate(NTMMaterial mat, Item plate) {
        makeRecipe(StampType.PLATE, mat.ingot(), new ItemStack(plate));
    }

    private static AStack dustTag(String name) {
        return new TagStack(TagKey.create(Registries.ITEM,
                dev.golem.core.Ids.of("c", "dusts/" + name)));
    }

    public static void makeRecipe(StampType type, AStack in, ItemStack out) {
        RECIPES.add(new PressRecipe(type, in, out));
    }

    private static final TagKey<Item> ANY_COKE = TagKey.create(Registries.ITEM,
            dev.golem.core.Ids.of("c", "cokes"));
}
