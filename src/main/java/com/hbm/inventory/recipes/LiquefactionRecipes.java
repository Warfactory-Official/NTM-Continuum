package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModOilItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Liquefactor recipes — item → {@link FluidStack} (stack size ignored, 1 consumed) — 1:1 port of the
 * 1.7 {@code LiquefactionRecipes.registerDefaults} (all 33 entries, History §19ej-3). Ore-dict inputs become
 * {@code c:} item tags; literal item/block inputs become their vanilla items. First-wins, resolved by exact
 * item then by tag, then 1.7's food fallback: any other food gives SALIENT at nutrition × saturation modifier × 20.
 */
public final class LiquefactionRecipes {

    private LiquefactionRecipes() {}

    private static final Map<Item, FluidStack> byItem = new HashMap<>();
    private static final Map<TagKey<Item>, FluidStack> byTag = new java.util.LinkedHashMap<>();
    private static boolean initialized = false;

    public static FluidStack getOutput(ItemStack stack) {
        init();
        if (stack.isEmpty()) return null;
        FluidStack direct = byItem.get(stack.getItem());
        if (direct != null) return direct;
        for (Map.Entry<TagKey<Item>, FluidStack> e : byTag.entrySet()) {
            if (stack.is(e.getKey())) return e.getValue();
        }
        // 1.7 getOutput: any ItemFood gives SALIENT, food value × saturation modifier × 2 (constant) × 10 (quanta)
        net.minecraft.world.food.FoodProperties food = dev.golem.core.world.ItemStacks.foodProperties(stack);
        if (food != null) {
            int salient = (int) (dev.golem.core.world.ItemStacks.foodNutrition(food)
                    * dev.golem.core.world.ItemStacks.foodSaturationModifier(food) * 20F);
            // 1.7 would return an empty 0 mB stack for a zero-saturation food; the port's tanks reject a 0 mB fill
            if (salient > 0) return new FluidStack(Fluids.SALIENT, salient);
        }
        return null;
    }

    /** Read-only views for JEI display. */
    public static Map<Item, FluidStack> getItemRecipes() { init(); return Collections.unmodifiableMap(byItem); }
    public static Map<TagKey<Item>, FluidStack> getTagRecipes() { init(); return Collections.unmodifiableMap(byTag); }

    private static TagKey<Item> c(String path) {
        return TagKey.create(Registries.ITEM, dev.golem.core.Ids.of("c", path));
    }

    private static void item(Item in, int fill, com.hbm.inventory.fluid.FluidType type) {
        if (in != null && in != Items.AIR) byItem.putIfAbsent(in, new FluidStack(type, fill));
    }

    private static void tag(String path, int fill, com.hbm.inventory.fluid.FluidType type) {
        byTag.putIfAbsent(c(path), new FluidStack(type, fill));
    }

    private static synchronized void init() {
        if (initialized) return;
        initialized = true;

        // oil processing
        tag("gems/coal", 100, Fluids.COALOIL);
        tag("dusts/coal", 100, Fluids.COALOIL);
        tag("gems/lignite", 50, Fluids.COALOIL);
        tag("dusts/lignite", 50, Fluids.COALOIL);
        item(ModOilItems.tar(EnumTarType.CRUDE), 75, Fluids.BITUMEN);   // KEY_OIL_TAR
        item(ModOilItems.tar(EnumTarType.CRACK), 100, Fluids.BITUMEN);  // KEY_CRACK_TAR
        item(ModOilItems.tar(EnumTarType.COAL), 50, Fluids.BITUMEN);    // KEY_COAL_TAR
        // KEY_LOG → the vanilla logs tag → MUG
        byTag.putIfAbsent(ItemTags.LOGS, new FluidStack(Fluids.MUG, 100));
        tag("dusts/sodium", 100, Fluids.SODIUM);
        tag("ingots/lead", 100, Fluids.LEAD);
        tag("dusts/lead", 100, Fluids.LEAD);
        tag("storage_blocks/lead", 900, Fluids.LEAD);

        // general utility
        item(Items.NETHERRACK.asItem(), 250, Fluids.LAVA);
        item(Items.COBBLESTONE.asItem(), 250, Fluids.LAVA);
        item(Items.STONE.asItem(), 250, Fluids.LAVA);
        item(Items.OBSIDIAN.asItem(), 500, Fluids.LAVA);
        item(Items.SNOWBALL, 125, Fluids.WATER);
        item(Items.SNOW_BLOCK.asItem(), 500, Fluids.WATER);
        item(Items.ICE.asItem(), 1000, Fluids.WATER);
        item(Items.PACKED_ICE.asItem(), 1000, Fluids.WATER);
        item(Items.ENDER_PEARL, 100, Fluids.ENDERJUICE);
        item(Items.SUGAR, 100, Fluids.ETHANOL);
        item(com.hbm.blocks.ModTerrainBlocks.plant_flower(com.hbm.blocks.generic.BlockNTMFlower.Type.WEED).asItem(), 150, Fluids.ETHANOL);
        item(com.hbm.blocks.ModTerrainBlocks.plant_flower(com.hbm.blocks.generic.BlockNTMFlower.Type.CD0).asItem(), 50, Fluids.ETHANOL);
        item(byName("hbm", "glyphid_gland_empty"), 2000, Fluids.BIOGAS);
        item(Items.SUNFLOWER, 100, Fluids.SUNFLOWEROIL);
        item(Items.WHEAT_SEEDS, 50, Fluids.SEEDSLURRY);
        item(Item.byBlock(net.minecraft.world.level.block.Blocks.SHORT_GRASS), 100, Fluids.SEEDSLURRY); // 1.7 tallgrass:1
        item(Items.FERN, 100, Fluids.SEEDSLURRY);                                                         // 1.7 tallgrass:2
        item(Items.VINE.asItem(), 100, Fluids.SEEDSLURRY);
        // 1.7 Items.fish at any meta (the raw fish only) → FISHOIL; cooked fish falls through to the food fallback
        byTag.putIfAbsent(MixerRecipes.RAW_FISHES, new FluidStack(Fluids.FISHOIL, 100));
        item(byName("hbm", "biomass"), 125, Fluids.BIOGAS);
        item(com.hbm.blocks.ModFallingBlocks.ore_oil_sand.get().asItem(), 100, Fluids.BITUMEN);
    }

    private static Item byName(String ns, String path) {
        return BuiltInRegistries.ITEM.getOptional(dev.golem.core.Ids.of(ns, path)).orElse(null);
    }
}
