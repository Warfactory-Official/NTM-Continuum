package com.hbm.recipes.tables;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.recipes.CrucibleRecipes;
import com.hbm.inventory.recipes.CrucibleRecipes.CrucibleRecipe;
import com.hbm.items.ModItems;
import dev.golem.core.world.RecipeSource;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/** 1.7 {@code CrucibleRecipes.registerDefaults}. */
public final class CrucibleTable extends RecipeSource<CrucibleRecipe> {

    public CrucibleTable() {
        super(CrucibleRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        int n = MaterialShapes.NUGGET.quantity;
        int i = MaterialShapes.INGOT.quantity;

        this.register(new CrucibleRecipe("crucible.steel").crucibleSetup(20, new ItemStack(ModItems.ingot_steel.get()))
                .inputs(new MaterialStack(Mats.MAT_IRON, n * 2), new MaterialStack(Mats.MAT_CARBON, n * 3), new MaterialStack(Mats.MAT_FLUX, n))
                .outputs(new MaterialStack(Mats.MAT_STEEL, n * 2)));

        // port: 1.7's Compat.isModLoaded(Compat.MOD_GT6) rows (crucible.steelWrought, steelPig, steelMeteoric) are not rows:
        // GregTech 6 exists for 1.7.10 only, on no port target

        this.register(new CrucibleRecipe("crucible.hematite").crucibleSetup(6, new ItemStack(ModOreBlocks.stone_resource_hematite.get().asItem()))
                .inputs(new MaterialStack(Mats.MAT_HEMATITE, i * 2), new MaterialStack(Mats.MAT_FLUX, n * 2))
                .outputs(new MaterialStack(Mats.MAT_IRON, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        this.register(new CrucibleRecipe("crucible.malachite").crucibleSetup(6, new ItemStack(ModOreBlocks.stone_resource_malachite.get().asItem()))
                .inputs(new MaterialStack(Mats.MAT_MALACHITE, i * 2), new MaterialStack(Mats.MAT_FLUX, n * 2))
                .outputs(new MaterialStack(Mats.MAT_COPPER, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        this.register(new CrucibleRecipe("crucible.redcopper").crucibleSetup(2, new ItemStack(ModItems.ingot_red_copper.get()))
                .inputs(new MaterialStack(Mats.MAT_COPPER, n), new MaterialStack(Mats.MAT_REDSTONE, n))
                .outputs(new MaterialStack(Mats.MAT_RED_COPPER, n * 2)));

        this.register(new CrucibleRecipe("crucible.hss").crucibleSetup(9, new ItemStack(ModItems.ingot_dura_steel.get()))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 5), new MaterialStack(Mats.MAT_TUNGSTEN, n * 3), new MaterialStack(Mats.MAT_COBALT, n * 1))
                .outputs(new MaterialStack(Mats.MAT_DURA, n * 9)));

        this.register(new CrucibleRecipe("crucible.ferro").crucibleSetup(3, new ItemStack(ModItems.ingot_ferrouranium.get()))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 2), new MaterialStack(Mats.MAT_U238, n))
                .outputs(new MaterialStack(Mats.MAT_FERRO, n * 3)));

        this.register(new CrucibleRecipe("crucible.tcalloy").crucibleSetup(9, new ItemStack(ModItems.ingot_tcalloy.get()))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 8), new MaterialStack(Mats.MAT_TECHNETIUM, n))
                .outputs(new MaterialStack(Mats.MAT_TCALLOY, i)));

        this.register(new CrucibleRecipe("crucible.cdalloy").crucibleSetup(9, new ItemStack(ModItems.ingot_cdalloy.get()))
                .inputs(new MaterialStack(Mats.MAT_STEEL, n * 8), new MaterialStack(Mats.MAT_CADMIUM, n))
                .outputs(new MaterialStack(Mats.MAT_CDALLOY, i)));

        this.register(new CrucibleRecipe("crucible.bbronze").crucibleSetup(9, new ItemStack(ModItems.ingot_bismuth_bronze.get()))
                .inputs(new MaterialStack(Mats.MAT_COPPER, n * 8), new MaterialStack(Mats.MAT_BISMUTH, n), new MaterialStack(Mats.MAT_FLUX, n * 3))
                .outputs(new MaterialStack(Mats.MAT_BBRONZE, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        this.register(new CrucibleRecipe("crucible.abronze").crucibleSetup(9, new ItemStack(ModItems.ingot_arsenic_bronze.get()))
                .inputs(new MaterialStack(Mats.MAT_COPPER, n * 8), new MaterialStack(Mats.MAT_ARSENIC, n), new MaterialStack(Mats.MAT_FLUX, n * 3))
                .outputs(new MaterialStack(Mats.MAT_ABRONZE, i), new MaterialStack(Mats.MAT_SLAG, n * 3)));

        this.register(new CrucibleRecipe("crucible.cmb").crucibleSetup(3, new ItemStack(ModItems.ingot_combine_steel.get()))
                .inputs(new MaterialStack(Mats.MAT_MAGTUNG, n * 6), new MaterialStack(Mats.MAT_MUD, n * 3))
                .outputs(new MaterialStack(Mats.MAT_CMB, i)));

        this.register(new CrucibleRecipe("crucible.magtung").crucibleSetup(3, new ItemStack(ModItems.ingot_magnetized_tungsten.get()))
                .inputs(new MaterialStack(Mats.MAT_TUNGSTEN, i), new MaterialStack(Mats.MAT_SCHRABIDIUM, n * 1))
                .outputs(new MaterialStack(Mats.MAT_MAGTUNG, i)));

        this.register(new CrucibleRecipe("crucible.bscco").crucibleSetup(3, new ItemStack(ModItems.ingot_bscco.get()))
                .inputs(new MaterialStack(Mats.MAT_BISMUTH, n * 2), new MaterialStack(Mats.MAT_STRONTIUM, n * 2), new MaterialStack(Mats.MAT_CALCIUM, n * 2), new MaterialStack(Mats.MAT_COPPER, n * 3))
                .outputs(new MaterialStack(Mats.MAT_BSCCO, i)));

        // 1.7 registerMoldsForNEI() adds display entries, not rows
    }

    /** 1.7 {@code register(recipe)}: the row takes 1.7's position as its order; the id is the name after the dot. */
    private void register(CrucibleRecipe recipe) {
        recipe.order = nextOrder();
        String name = recipe.getInternalName();
        register(name.substring(name.indexOf('.') + 1).toLowerCase(Locale.ROOT), recipe);
    }
}
