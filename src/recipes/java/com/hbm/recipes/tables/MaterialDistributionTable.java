package com.hbm.recipes.tables;

import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.material.MaterialDistribution;
import com.hbm.inventory.material.MaterialDistribution.Row;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModCasingItems;
import com.hbm.items.ModIngredientItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModMachineItems;
import com.hbm.items.machine.ModStampItems;
import dev.golem.core.world.RecipeSource;
import com.mojang.datafixers.util.Either;
import dev.golem.core.Ids;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static com.hbm.inventory.material.MaterialShapes.*;
import static com.hbm.inventory.material.Mats.*;

/**
 * 1.7 {@code MatDistribution.registerDefaults}. 1.7's meta items are the port's split items, its ore dictionary names
 * the port's {@code c:} tags ({@code stone} is {@code c:stones}, {@code oreIron} is {@code c:ores/iron}).
 */
public final class MaterialDistributionTable extends RecipeSource<Row> {

    public MaterialDistributionTable() {
        super(MaterialDistribution.TABLE.table);
    }

    @Override
    protected void registerDefaults() {
        //vanilla crap
        registerOre(tag("stones"), MAT_STONE, q(BLOCK, 1));
        registerOre(tag("cobblestones"), MAT_STONE, q(BLOCK, 1));
        registerEntry(Items.OBSIDIAN, MAT_OBSIDIAN, q(BLOCK, 1));
        registerEntry(Items.RAIL, MAT_IRON, q(INGOT, 6, 16));
        registerEntry(Items.POWERED_RAIL, MAT_GOLD, q(INGOT, 6, 6), MAT_REDSTONE, q(DUST, 1, 6));
        registerEntry(Items.DETECTOR_RAIL, MAT_IRON, q(INGOT, 6, 6), MAT_REDSTONE, q(DUST, 1, 6));
        registerEntry(Items.MINECART, MAT_IRON, q(INGOT, 5));

        //castables
        registerEntry(ModItems.blade_titanium.get(),            MAT_TITANIUM,       q(INGOT, 3));
        registerEntry(ModItems.blade_tungsten.get(),            MAT_TUNGSTEN,       q(INGOT, 3));
        registerEntry(ModMachineItems.blades_steel.get(),       MAT_STEEL,          q(INGOT, 4));
        registerEntry(ModMachineItems.blades_titanium.get(),    MAT_TITANIUM,       q(INGOT, 4));
        registerEntry(ModStampItems.stamp_stone_flat.get(),     MAT_STONE,          q(INGOT, 3));
        registerEntry(ModStampItems.stamp_iron_flat.get(),      MAT_IRON,           q(INGOT, 3));
        registerEntry(ModStampItems.stamp_steel_flat.get(),     MAT_STEEL,          q(INGOT, 3));
        registerEntry(ModStampItems.stamp_titanium_flat.get(),  MAT_TITANIUM,       q(INGOT, 3));
        registerEntry(ModStampItems.stamp_obsidian_flat.get(),  MAT_OBSIDIAN,       q(INGOT, 3));
        registerEntry(ModItems.pipes_steel.get(),               MAT_STEEL,          q(BLOCK, 3));

        registerEntry(ModCasingItems.casing(EnumCasingType.SMALL),          MAT_GUNMETAL,       q(PLATE, 1, 4));
        registerEntry(ModCasingItems.casing(EnumCasingType.SMALL_STEEL),    MAT_WEAPONSTEEL,    q(PLATE, 1, 4));
        registerEntry(ModCasingItems.casing(EnumCasingType.LARGE),          MAT_GUNMETAL,       q(PLATE, 1, 2));
        registerEntry(ModCasingItems.casing(EnumCasingType.LARGE_STEEL),    MAT_WEAPONSTEEL,    q(PLATE, 1, 2));
        // 1.7 registerEntry(Items.minecart, MAT_IRON, INGOT.q(5)) again here: the same row, put a second time
        registerEntry(ModIntermediateItems.chunk(EnumChunkType.CRYOLITE), MAT_ALUMINIUM, q(INGOT, 1), MAT_SODIUM, q(INGOT, 1));
        //actual ores
        // 1.7 skips these four when GregTech 6 is loaded, which never runs alongside the port
        registerOre(MAT_IRON.tag(ORE), MAT_IRON, q(INGOT, 2), MAT_TITANIUM, q(NUGGET, 3), MAT_STONE, quart());
        registerOre(MAT_TITANIUM.tag(ORE), MAT_TITANIUM, q(INGOT, 2), MAT_IRON, q(NUGGET, 3), MAT_STONE, quart());
        registerOre(MAT_TUNGSTEN.tag(ORE), MAT_TUNGSTEN, q(INGOT, 2), MAT_STONE, quart());
        registerOre(MAT_ALUMINIUM.tag(ORE), MAT_ALUMINIUM, q(INGOT, 2), MAT_SODIUM, q(NUGGET, 3), MAT_STONE, quart());

        registerOre(MAT_COAL.tag(ORE), MAT_CARBON, q(GEM, 3), MAT_STONE, quart());
        registerOre(MAT_GOLD.tag(ORE), MAT_GOLD, q(INGOT, 2), MAT_LEAD, q(NUGGET, 3), MAT_STONE, quart());
        registerOre(MAT_URANIUM.tag(ORE), MAT_URANIUM, q(INGOT, 2), MAT_LEAD, q(NUGGET, 3), MAT_STONE, quart());
        // 1.7 loops every thorium ore name (TH232.all(ONLY_ORE)); the port has the one tag
        registerOre(MAT_THORIUM.tag(ORE), MAT_THORIUM, q(INGOT, 2), MAT_URANIUM, q(NUGGET, 3), MAT_STONE, quart());
        registerOre(MAT_COPPER.tag(ORE), MAT_COPPER, q(INGOT, 2), MAT_STONE, quart());
        registerOre(MAT_LEAD.tag(ORE), MAT_LEAD, q(INGOT, 2), MAT_GOLD, q(NUGGET, 1), MAT_STONE, quart());
        registerOre(MAT_BERYLLIUM.tag(ORE), MAT_BERYLLIUM, q(INGOT, 2), MAT_STONE, quart());
        registerOre(MAT_COBALT.tag(ORE), MAT_COBALT, q(INGOT, 1), MAT_STONE, quart());
        registerOre(MAT_REDSTONE.tag(ORE), MAT_REDSTONE, q(INGOT, 4), MAT_STONE, quart());

        registerOre(MAT_HEMATITE.tag(ORE), MAT_HEMATITE, q(INGOT, 1));
        registerOre(MAT_MALACHITE.tag(ORE), MAT_MALACHITE, q(INGOT, 6));

        registerEntry(ModOreBlocks.stone_resource_limestone.get().asItem(), MAT_FLUX, q(DUST, 10));
        registerEntry(ModItems.powder_flux.get(), MAT_FLUX, q(DUST, 1));
        registerEntry(Items.CHARCOAL, MAT_CARBON, q(NUGGET, 3));

        registerEntry(ModIngredientItems.ash(EnumAshType.WOOD), MAT_CARBON, q(NUGGET, 1));
        registerEntry(ModIngredientItems.ash(EnumAshType.COAL), MAT_CARBON, q(NUGGET, 2));
        registerEntry(ModIngredientItems.ash(EnumAshType.MISC), MAT_CARBON, q(NUGGET, 1));
    }

    /** 1.7 {@code registerEntry(item, material, amount, ...)}; the id is the item (its namespace first unless {@code hbm}). */
    private void registerEntry(Item key, Object... matDef) {
        var id = BuiltInRegistries.ITEM.getKey(key);
        String name = id.getNamespace().equals("hbm") ? id.getPath() : id.getNamespace() + "_" + id.getPath();
        register(name, new Row(Either.right(key), stacks(matDef)));
    }

    /** 1.7 {@code registerOre(oreName, material, amount, ...)}; the id is {@code tag_} and the tag path. */
    private void registerOre(TagKey<Item> key, Object... matDef) {
        register("tag_" + key.location().getPath().replace('/', '_'), new Row(Either.left(key), stacks(matDef)));
    }

    private static List<MaterialStack> stacks(Object... matDef) {
        List<MaterialStack> stacks = new ArrayList<>();
        for (int i = 0; i < matDef.length; i += 2) {
            stacks.add(new MaterialStack((NTMMaterial) matDef[i], (int) matDef[i + 1]));
        }
        return stacks;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(Registries.ITEM, Ids.of("c", path));
    }

    /** 1.7 {@code QUART.q(1)}: a quarter block. */
    private static int quart() {
        return BLOCK.quantity / 4;
    }

    /** 1.7 {@code MaterialShapes.q(amount)}. */
    private static int q(MaterialShapes shape, int amount) {
        return shape.quantity * amount;
    }

    /** 1.7 {@code MaterialShapes.q(unitsUsed, itemsProduced)}. */
    private static int q(MaterialShapes shape, int unitsUsed, int itemsProduced) {
        return shape.quantity * unitsUsed / itemsProduced;
    }
}
