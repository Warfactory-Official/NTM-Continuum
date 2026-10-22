package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.AL;
import static com.hbm.inventory.OreDictManager.BIGMT;
import static com.hbm.inventory.OreDictManager.CO;
import static com.hbm.inventory.OreDictManager.CU;
import static com.hbm.inventory.OreDictManager.GOLD;
import static com.hbm.inventory.OreDictManager.IRON;

import com.hbm.blocks.RawOres;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModAutogenItems;
import com.hbm.items.ModItems;
import com.hbm.inventory.recipes.loader.GenericTableSource;
import dev.golem.core.Ids;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** 1.7 {@code BlastFurnaceRecipesNT.registerDefaults}. */
public final class BlastFurnaceTable extends GenericTableSource<GenericRecipe> {

    /** 1.7 {@code KEY_SAND}, the "sand" ore name: {@code c:sands} (sand and red sand, not suspicious sand). */
    private static final TagKey<Item> KEY_SAND = TagKey.create(Registries.ITEM, Ids.of("c", "sands"));

    private static final NTMMaterial REDSTONE = Mats.MAT_REDSTONE;
    /** 1.7 {@code LIMESTONE.dust()} ("dustLimestone"). */
    private static final TagKey<Item> LIMESTONE_DUST = TagKey.create(Registries.ITEM, Ids.of("c", "dusts/limestone"));

    public BlastFurnaceTable() {
        super(BlastFurnaceRecipesNT.INSTANCE);
    }

    @Override
    protected void registerDefaults() {

        // 20s per ingot of steel produced
        this.register(new GenericRecipe("blast.steelFromIngot").setDuration(800)
                .inputItems(IRON.ingot().count(2), new TagStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slag(1)));
        this.register(new GenericRecipe("blast.steelFromDust").setDuration(800)
                .inputItems(IRON.dust().count(2), new TagStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slag(1)));
        this.register(new GenericRecipe("blast.steelFromOre").setDuration(800)
                .inputItems(IRON.ore(), new TagStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slag(2)));
        this.register(new GenericRecipe("blast.steelWithFlux").setDuration(1_200)
                .inputItems(IRON.ore(), new ComparableStack(ModItems.powder_flux.get()))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 3), slag(2)));
        // port: ntm-next's raw ores, each output at two thirds of the ore row's
        this.register(new GenericRecipe("blast.steelFromOre_raw").setDuration(800)
                .inputItems(RawOres.stack(IRON), new TagStack(KEY_SAND))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 2), slag(2)).outputChances(RawOres.SHARE, RawOres.SHARE));
        this.register(new GenericRecipe("blast.steelWithFlux_raw").setDuration(1_200)
                .inputItems(RawOres.stack(IRON), new ComparableStack(ModItems.powder_flux.get()))
                .outputItems(new ItemStack(ModItems.ingot_steel.get(), 3), slag(2)).outputChances(RawOres.SHARE, RawOres.SHARE));

        this.register(new GenericRecipe("blast.mingrade").setDuration(400)
                .inputItems(CU.ingot(), REDSTONE.dust())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        this.register(new GenericRecipe("blast.mingradeDust").setDuration(400)
                .inputItems(CU.dust(), REDSTONE.dust())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        this.register(new GenericRecipe("blast.mingradeIngot").setDuration(400)
                .inputItems(CU.ingot(), REDSTONE.ingot())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        this.register(new GenericRecipe("blast.mingradeCursed").setDuration(400)
                .inputItems(CU.dust(), REDSTONE.ingot())
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 2)));
        this.register(new GenericRecipe("blast.mingradeOre").setDuration(1_200)
                .inputItems(CU.ore(), REDSTONE.dust().count(6))
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 6), slag(1)));
        // port: ntm-next's raw copper
        this.register(new GenericRecipe("blast.mingradeOre_raw").setDuration(1_200)
                .inputItems(RawOres.stack(CU), REDSTONE.dust().count(6))
                .outputItems(new ItemStack(ModItems.ingot_red_copper.get(), 6), slag(1)).outputChances(RawOres.SHARE, RawOres.SHARE));

        this.register(new GenericRecipe("blast.meteorSword").setDuration(1_200)
                .inputItems(CO.ingot(), new ComparableStack(ModAbilityToolItems.meteorite_sword_hardened.get(), 1))
                .outputItems(new ItemStack(ModAbilityToolItems.meteorite_sword_alloyed.get(), 1)));

        this.register(new GenericRecipe("blast.meteor").setDuration(600)
                .inputItems(CO.ingot(), new ComparableStack(ModItems.powder_meteorite.get(), 1))
                .outputItems(new ItemStack(ModAbilityToolItems.ingot_meteorite.get(), 1)));
        this.register(new GenericRecipe("blast.starmetal").setDuration(600)
                .inputItems(BIGMT.ingot(), new ComparableStack(ModAbilityToolItems.ingot_meteorite.get(), 1))
                .outputItems(new ItemStack(ModItems.ingot_starmetal.get(), 1)));

        this.register(new GenericRecipe("blast.paa").setDuration(600)
                .inputItems(GOLD.ingot(), new ComparableStack(ModItems.plate_mixed.get(), 1))
                .outputItems(new ItemStack(ModItems.plate_paa.get(), 1)));

        this.register(new GenericRecipe("blast.firebrick").setDuration(800)
                .inputItems(AL.dust(), new ComparableStack(Items.CLAY_BALL, 7))
                .outputItems(new ItemStack(ModItems.ingot_firebrick.get(), 8)));
        this.register(new GenericRecipe("blast.firebrickLimestone").setDuration(800)
                .inputItems(new TagStack(LIMESTONE_DUST), new ComparableStack(Items.CLAY_BALL, 6))
                .outputItems(new ItemStack(ModItems.ingot_firebrick.get(), 8)));
    }

    /** 1.7 {@code new ItemStack(ModItems.ingot_raw, count, Mats.MAT_SLAG.id)}: the autogen slag ingot. */
    private static ItemStack slag(int count) {
        return new ItemStack(ModAutogenItems.get(MaterialShapes.INGOT, Mats.MAT_SLAG), count);
    }
}
