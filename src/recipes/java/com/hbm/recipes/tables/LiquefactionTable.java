package com.hbm.recipes.tables;

import static com.hbm.inventory.OreDictManager.*;

import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockNTMFlower;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.TagStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.LiquefactionRecipes;
import com.hbm.inventory.recipes.LiquefactionRecipes.Row;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.items.ItemEnums.EnumTarType;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModItems;
import com.hbm.items.ModOilItems;
import dev.golem.core.world.RecipeSource;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 {@code LiquefactionRecipes.registerDefaults}. 1.7's ore names are {@code c:} tags, its tar keys the split
 * {@code oil_tar_*} items, {@code KEY_LOG} the vanilla logs tag and {@code Items.fish} at any meta the raw-fish tag.
 * The id of a row is its input: {@code tag_<ns>_<path>} for a tag, the path for a vanilla item, {@code <ns>_<path>}
 * otherwise. 1.7's food fallback (SALIENT) is code in {@code LiquefactionRecipes}, not a row.
 */
public final class LiquefactionTable extends RecipeSource<Row> {

    // 1.7 DictFrame COAL and LIGNITE
    private static final NTMMaterial COAL = Mats.MAT_COAL;
    private static final NTMMaterial LIGNITE = Mats.MAT_LIGNITE;

    public LiquefactionTable() {
        super(LiquefactionRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        //oil processing
        put(COAL.gem(),                                         new FluidStack(100, Fluids.COALOIL));
        put(COAL.dust(),                                        new FluidStack(100, Fluids.COALOIL));
        put(LIGNITE.gem(),                                      new FluidStack(50, Fluids.COALOIL));
        put(LIGNITE.dust(),                                     new FluidStack(50, Fluids.COALOIL));
        put(new ComparableStack(ModOilItems.tar(EnumTarType.CRUDE)),    new FluidStack(75, Fluids.BITUMEN));    // KEY_OIL_TAR
        put(new ComparableStack(ModOilItems.tar(EnumTarType.CRACK)),    new FluidStack(100, Fluids.BITUMEN));   // KEY_CRACK_TAR
        put(new ComparableStack(ModOilItems.tar(EnumTarType.COAL)),     new FluidStack(50, Fluids.BITUMEN));    // KEY_COAL_TAR
        put(new TagStack(ItemTags.LOGS),                        new FluidStack(100, Fluids.MUG));               // KEY_LOG
        put(NA.dust(),                                          new FluidStack(100, Fluids.SODIUM));
        put(PB.ingot(),                                         new FluidStack(100, Fluids.LEAD));
        put(PB.dust(),                                          new FluidStack(100, Fluids.LEAD));
        put(PB.block(),                                         new FluidStack(900, Fluids.LEAD));
        //general utility recipes because why not
        put(new ComparableStack(Items.NETHERRACK),              new FluidStack(250, Fluids.LAVA));
        put(new ComparableStack(Items.COBBLESTONE),             new FluidStack(250, Fluids.LAVA));
        put(new ComparableStack(Items.STONE),                   new FluidStack(250, Fluids.LAVA));
        put(new ComparableStack(Items.OBSIDIAN),                new FluidStack(500, Fluids.LAVA));
        put(new ComparableStack(Items.SNOWBALL),                new FluidStack(125, Fluids.WATER));
        put(new ComparableStack(Items.SNOW_BLOCK),              new FluidStack(500, Fluids.WATER));
        put(new ComparableStack(Items.ICE),                     new FluidStack(1000, Fluids.WATER));
        put(new ComparableStack(Items.PACKED_ICE),              new FluidStack(1000, Fluids.WATER));
        put(new ComparableStack(Items.ENDER_PEARL),             new FluidStack(100, Fluids.ENDERJUICE));
        put(new ComparableStack(ModFallingBlocks.ore_oil_sand.get().asItem()), new FluidStack(100, Fluids.BITUMEN));

        put(new ComparableStack(Items.SUGAR),                   new FluidStack(100, Fluids.ETHANOL));
        put(new ComparableStack(ModTerrainBlocks.plant_flower(BlockNTMFlower.Type.WEED).asItem()), new FluidStack(150, Fluids.ETHANOL));   // plant_flower:3
        put(new ComparableStack(ModTerrainBlocks.plant_flower(BlockNTMFlower.Type.CD0).asItem()), new FluidStack(50, Fluids.ETHANOL));     // plant_flower:4
        put(new ComparableStack(ModFuelItems.biomass.get()),        new FluidStack(125, Fluids.BIOGAS));
        put(new ComparableStack(ModItems.glyphid_gland_empty.get()), new FluidStack(2000, Fluids.BIOGAS));
        put(new TagStack(MixerRecipes.RAW_FISHES),              new FluidStack(100, Fluids.FISHOIL));           // Items.fish, any meta
        put(new ComparableStack(Items.SUNFLOWER),               new FluidStack(100, Fluids.SUNFLOWEROIL));      // double_plant:0

        put(new ComparableStack(Items.WHEAT_SEEDS),             new FluidStack(50, Fluids.SEEDSLURRY));
        put(new ComparableStack(Item.byBlock(Blocks.SHORT_GRASS)), new FluidStack(100, Fluids.SEEDSLURRY));     // tallgrass:1
        put(new ComparableStack(Items.FERN),                    new FluidStack(100, Fluids.SEEDSLURRY));        // tallgrass:2
        put(new ComparableStack(Items.VINE),                    new FluidStack(100, Fluids.SEEDSLURRY));
    }

    /** 1.7 {@code recipes.put(input, output)}. */
    private void put(AStack input, FluidStack output) {
        if (input instanceof TagStack tag) {
            register(id(tag.tag), new Row(Either.left(tag.tag), output));
        } else if (input instanceof ComparableStack stack) {
            register(id(stack.item), new Row(Either.right(stack.item), output));
        } else {
            throw new IllegalArgumentException("not an item or tag input: " + input);
        }
    }

    private static String id(TagKey<Item> tag) {
        Identifier location = tag.location();
        return "tag_" + location.getNamespace() + "_" + location.getPath().replace('/', '_');
    }

    private static String id(Item item) {
        Identifier location = BuiltInRegistries.ITEM.getKey(item);
        return location.getNamespace().equals("minecraft") ? location.getPath() : location.getNamespace() + "_" + location.getPath();
    }
}
