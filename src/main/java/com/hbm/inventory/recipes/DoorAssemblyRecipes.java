package com.hbm.inventory.recipes;

import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModGlassBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import com.hbm.items.ModItems;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import static com.hbm.inventory.OreDictManager.*;
import com.hbm.blocks.ModDoorBlocks;

/**
 * The 1.7.10 {@code AssemblyMachineRecipes} "nuclear door mod" block, kept with the door feature. The
 * dye ore-dict keys ({@code KEY_RED} etc.) are the {@code c:dyes/<colour>} tags.
 */
public final class DoorAssemblyRecipes {

    private DoorAssemblyRecipes() {}

    private static TagStack dye(String colour, int count) {
        return new TagStack(TagKey.create(Registries.ITEM, Ids.of("c", "dyes/" + colour)), count);
    }

    public static void registerAll(GenericRecipes recipes) {
        Item motor = ModItems.motor.get();
        Item motorDesh = ModItems.motor_desh.get();
        Item polymer = ModItems.plate_polymer.get();

        recipes.registerGenerated(new GenericRecipe("ass.vaultdoor").setup(600, 100).outputItems(new ItemStack(ModDoorBlocks.VAULT_DOOR.get(), 1))
                .inputItems(STEEL.ingot().count(32), DURA.ingot().count(32), PB.plateCast().count(8), ANY_RUBBER.ingot().count(12), DURA.bolt().count(32), new ComparableStack(motor, 3)));
        recipes.registerGenerated(new GenericRecipe("ass.blastdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.BLAST_DOOR.get(), 1))
                .inputItems(STEEL.ingot().count(12), PB.plate().count(6), ANY_RUBBER.ingot().count(2), DURA.bolt().count(8), new ComparableStack(motor, 1)));
        recipes.registerGenerated(new GenericRecipe("ass.firedoor").setup(300, 100).outputItems(new ItemStack(ModDoorBlocks.FIRE_DOOR.get(), 1))
                .inputItems(STEEL.plate().count(16), DURA.bolt().count(8), new ComparableStack(motor, 2)));
        recipes.registerGenerated(new GenericRecipe("ass.seal").setup(1_200, 100).outputItems(new ItemStack(ModDoorBlocks.TRANSITION_SEAL.get(), 1))
                .inputItems(new ComparableStack(ModHazardBlocks.cmb_brick_reinforced.get().asItem(), 16), STEEL.plate().count(64), ANY_RUBBER.ingot().count(36), STEEL.block().count(32), new ComparableStack(motorDesh, 16), dye("yellow", 4)));
        recipes.registerGenerated(new GenericRecipe("ass.slidingdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.SLIDING_BLAST_DOOR.get(), 1))
                .inputItems(STEEL.plate().count(16), W.ingot().count(8), new ComparableStack(ModGlassBlocks.reinforced_glass.get().asItem(), 4), ANY_RUBBER.ingot().count(4), DURA.bolt().count(16), new ComparableStack(motor, 2)));
        recipes.registerGenerated(new GenericRecipe("ass.vehicledoor").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.LARGE_VEHICLE_DOOR.get(), 1))
                .inputItems(STEEL.plateCast().count(16), new ComparableStack(polymer, 4), new ComparableStack(motor, 4), DURA.bolt().count(16), dye("green", 4)));
        recipes.registerGenerated(new GenericRecipe("ass.waterdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.WATER_DOOR.get(), 1))
                .inputItems(STEEL.plate().count(16), DURA.bolt().count(4), dye("red", 1)));
        recipes.registerGenerated(new GenericRecipe("ass.qedoor").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.QE_CONTAINMENT.get(), 1))
                .inputItems(STEEL.plateCast().count(6), new ComparableStack(polymer, 8), new ComparableStack(motor, 2), DURA.bolt().count(32), dye("black", 4)));
        recipes.registerGenerated(new GenericRecipe("ass.queslidingdoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.QE_SLIDING_DOOR.get(), 1))
                .inputItems(STEEL.plate().count(4), new ComparableStack(polymer, 4), new ComparableStack(motor, 2), DURA.bolt().count(4), dye("white", 4), new ComparableStack(Items.GLASS, 4)));
        recipes.registerGenerated(new GenericRecipe("ass.roundairlock").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.ROUND_AIRLOCK_DOOR.get(), 1))
                .inputItems(STEEL.plateCast().count(12), new ComparableStack(polymer, 16), new ComparableStack(motor, 4), DURA.bolt().count(16), dye("green", 4)));
        recipes.registerGenerated(new GenericRecipe("ass.secureaccess").setup(400, 100).outputItems(new ItemStack(ModDoorBlocks.SECURE_ACCESS_DOOR.get(), 1))
                .inputItems(STEEL.plateCast().count(12), new ComparableStack(polymer, 8), new ComparableStack(motor, 4), DURA.bolt().count(32), dye("red", 8)));
        recipes.registerGenerated(new GenericRecipe("ass.slidingseal").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.SLIDING_SEAL_DOOR.get(), 1))
                .inputItems(STEEL.plate().count(12), new ComparableStack(polymer, 4), new ComparableStack(motor, 2), DURA.bolt().count(4), dye("white", 2)));
        recipes.registerGenerated(new GenericRecipe("ass.silohatch").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.SILO_HATCH.get(), 1))
                .inputItems(STEEL.plateWelded().count(4), new ComparableStack(polymer, 4), new ComparableStack(motor, 2), STEEL.bolt().count(16), dye("green", 4)));
        recipes.registerGenerated(new GenericRecipe("ass.silohatchlarge").setup(300, 100).outputItems(new ItemStack(ModDoorBlocks.SILO_HATCH_LARGE.get(), 1))
                .inputItems(STEEL.plateWelded().count(6), new ComparableStack(polymer, 8), new ComparableStack(motor, 2), STEEL.bolt().count(16), dye("green", 8)));
        recipes.registerGenerated(new GenericRecipe("ass.cargodoor").setup(200, 100).outputItems(new ItemStack(ModDoorBlocks.CARGO_DOOR.get(), 1))
                .inputItems(new ComparableStack(ModDecoBlocks.steel_beam.get().asItem(), 32), STEEL.plate().count(4), DURA.bolt().count(4), new ComparableStack(motor, 2), dye("gray", 1)));
    }
}
