package com.hbm.recipes.tables;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.ModDecoCrateBlocks;
import com.hbm.blocks.ModFallingBlocks;
import com.hbm.blocks.ModFalloutBlocks;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.ModSteelBlocks;
import com.hbm.blocks.ModTerrainBlocks;
import com.hbm.blocks.generic.BlockBobble.BobbleType;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.inventory.recipes.ShredderRecipes.Row;
import com.hbm.inventory.recipes.loader.GenericRecipeCodec.ItemOut;
import com.hbm.items.machine.ItemDrive.EnumDriveType;
import com.hbm.items.ItemEnums.EnumChunkType;
import com.hbm.items.ModBedrockOreItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.ModIntermediateItems;
import com.hbm.items.ModItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.items.special.ItemBedrockOreGraded.Grade;
import dev.golem.core.Ids;
import dev.golem.core.world.RecipeSource;
import com.mojang.datafixers.util.Either;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * 1.7 {@code ShredderRecipes.registerDefaults}, the explicit rows. 1.7's ore-dictionary loops ({@code logWood},
 * {@code plankWood}, {@code treeSapling}, {@code dustLapis}), the wool and stained clay metas and {@code registerPost}
 * are not files: {@code ShredderRecipes} sweeps the matching tags at run time. 1.7 metadata stacks are the port's split
 * items. A row's id is its input item: the path, or {@code minecraft_<path>} for vanilla. Like 1.7's
 * {@code setRecipe}, the first row of an input wins.
 */
public final class ShredderTable extends RecipeSource<Row> {

    private final Set<String> inputs = new HashSet<>();

    public ShredderTable() {
        super(ShredderRecipes.TABLE.table);
    }

    @Override
    protected void registerDefaults() {

        /* Primary recipes */
        setRecipe(ModItems.scrap.get(), new ItemStack(ModItems.dust.get()));
        setRecipe(ModItems.dust.get(), new ItemStack(ModItems.dust.get()));
        setRecipe(ModItems.dust_tiny.get(), new ItemStack(ModItems.dust_tiny.get()));
        setRecipe(Items.GLOWSTONE, new ItemStack(Items.GLOWSTONE_DUST, 4));
        setRecipe(Items.QUARTZ_BLOCK, new ItemStack(ModItems.powder_quartz.get(), 4));
        setRecipe(Items.CHISELED_QUARTZ_BLOCK, new ItemStack(ModItems.powder_quartz.get(), 4));
        setRecipe(Items.QUARTZ_PILLAR, new ItemStack(ModItems.powder_quartz.get(), 4));
        setRecipe(Items.QUARTZ_STAIRS, new ItemStack(ModItems.powder_quartz.get(), 3));
        setRecipe(Items.QUARTZ_SLAB, new ItemStack(ModItems.powder_quartz.get(), 2));     // stone_slab:7
        setRecipe(Items.QUARTZ, new ItemStack(ModItems.powder_quartz.get()));
        setRecipe(Items.NETHER_QUARTZ_ORE, new ItemStack(ModItems.powder_quartz.get(), 2));
        setRecipe(ModBlocks.ore_nether_fire.get(), new ItemStack(ModFuelItems.powder_fire.get(), 6));
        setRecipe(Items.PACKED_ICE, new ItemStack(ModItems.powder_ice.get(), 1));
        setRecipe(ModBlocks.brick_light.get(), new ItemStack(Items.CLAY_BALL, 4));
        setRecipe(ModBlocks.concrete.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModBlocks.concrete_smooth.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModHazardBlocks.brick_concrete.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModHazardBlocks.brick_concrete_mossy.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModBlocks.brick_concrete_cracked.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModBlocks.brick_concrete_broken.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModBlocks.brick_obsidian.get(), new ItemStack(ModFallingBlocks.gravel_obsidian.get(), 1));
        setRecipe(Items.OBSIDIAN, new ItemStack(ModFallingBlocks.gravel_obsidian.get(), 1));
        setRecipe(Items.STONE, new ItemStack(Items.GRAVEL, 1));
        setRecipe(ModBlocks.ore_oil_empty.get(), new ItemStack(Items.GRAVEL, 1));
        setRecipe(Items.COBBLESTONE, new ItemStack(Items.GRAVEL, 1));
        setRecipe(Items.STONE_BRICKS, new ItemStack(Items.GRAVEL, 1));
        setRecipe(Items.GRAVEL, new ItemStack(Items.SAND, 1));
        setRecipe(Items.BRICKS, new ItemStack(Items.CLAY_BALL, 4));
        setRecipe(Items.BRICK_STAIRS, new ItemStack(Items.CLAY_BALL, 3));
        setRecipe(Items.FLOWER_POT, new ItemStack(Items.CLAY_BALL, 3));
        setRecipe(Items.BRICK, new ItemStack(Items.CLAY_BALL, 1));
        setRecipe(Items.SANDSTONE, new ItemStack(Items.SAND, 4));
        setRecipe(Items.SANDSTONE_STAIRS, new ItemStack(Items.SAND, 6));
        setRecipe(Items.CLAY, new ItemStack(Items.CLAY_BALL, 4));
        setRecipe(Items.TERRACOTTA, new ItemStack(Items.CLAY_BALL, 4));
        setRecipe(Items.TNT, new ItemStack(Items.GUNPOWDER, 5)); // 4 with GregTech 6, which no target has
        setRecipe(item("stone_resource_limestone"), new ItemStack(ModItems.powder_limestone.get(), 4));
        setRecipe(ModBlocks.stone_gneiss.get(), new ItemStack(ModItems.powder_lithium_tiny.get(), 1));
        setRecipe(ModItems.fragment_neodymium.get(), new ItemStack(ModItems.powder_neodymium_tiny.get(), 1));
        setRecipe(ModItems.fragment_cobalt.get(), new ItemStack(ModItems.powder_cobalt_tiny.get(), 1));
        setRecipe(ModItems.fragment_niobium.get(), new ItemStack(ModItems.powder_niobium_tiny.get(), 1));
        setRecipe(ModItems.fragment_cerium.get(), new ItemStack(ModItems.powder_cerium_tiny.get(), 1));
        setRecipe(ModItems.fragment_lanthanium.get(), new ItemStack(ModItems.powder_lanthanium_tiny.get(), 1));
        setRecipe(ModItems.fragment_actinium.get(), new ItemStack(ModItems.powder_actinium_tiny.get(), 1));
        setRecipe(ModItems.fragment_boron.get(), new ItemStack(ModItems.powder_boron_tiny.get(), 1));
        setRecipe(ModItems.fragment_meteorite.get(), new ItemStack(ModItems.powder_meteorite_tiny.get(), 1));
        setRecipe(ModBlocks.block_meteor.get(), new ItemStack(ModItems.powder_meteorite.get(), 10));
        setRecipe(Items.ENCHANTED_BOOK, new ItemStack(ModItems.powder_magic.get(), 1));
        setRecipe(ModBlocks.meteor_polished.get(), new ItemStack(ModItems.powder_meteorite.get(), 1));
        setRecipe(ModBlocks.meteor_brick.get(), new ItemStack(ModItems.powder_meteorite.get(), 1));
        setRecipe(ModBlocks.meteor_brick_mossy.get(), new ItemStack(ModItems.powder_meteorite.get(), 1));
        setRecipe(ModBlocks.meteor_brick_cracked.get(), new ItemStack(ModItems.powder_meteorite.get(), 1));
        setRecipe(ModBlocks.meteor_brick_chiseled.get(), new ItemStack(ModItems.powder_meteorite.get(), 1));
        setRecipe(ModDecoContent.meteor_pillar.get(), new ItemStack(ModItems.powder_meteorite.get(), 1));
        setRecipe(ModBlocks.ore_rare.get(), new ItemStack(ModItems.powder_desh_mix.get(), 1));
        setRecipe(Items.DIAMOND_ORE, new ItemStack(ModFallingBlocks.gravel_diamond.get(), 2));
        setRecipe(ModFalloutBlocks.ore_sellafield_diamond.get(), new ItemStack(ModFallingBlocks.gravel_diamond.get(), 2));
        setRecipe(ModDecoBlocks.boxcar.get(), new ItemStack(ModItems.powder_steel.get(), 32));
        setRecipe(ModItems.ingot_schrabidate.get(), new ItemStack(ModItems.powder_schrabidate.get(), 1));
        setRecipe(ModHazardBlocks.block_schrabidate.get(), new ItemStack(ModItems.powder_schrabidate.get(), 9));
        setRecipe(ModFuelItems.coal_infernal.get(), new ItemStack(ModFuelItems.powder_coal.get(), 2));
        setRecipe(Items.FERMENTED_SPIDER_EYE, new ItemStack(ModItems.powder_poison.get(), 3));
        setRecipe(Items.POISONOUS_POTATO, new ItemStack(ModItems.powder_poison.get(), 1));
        setRecipe(ModBlocks.ore_tektite_osmiridium.get(), new ItemStack(ModItems.powder_tektite.get(), 1));
        setRecipe(Items.DIRT, new ItemStack(ModItems.dust.get(), 1));
        setRecipe(Items.SUGAR_CANE, new ItemStack(Items.SUGAR, 3));
        setRecipe(Items.APPLE, new ItemStack(Items.SUGAR, 1));
        setRecipe(Items.CARROT, new ItemStack(Items.SUGAR, 1));
        setRecipe(ModItems.can_empty.get(), new ItemStack(ModItems.powder_aluminium.get(), 2));
        setRecipe(ModIntermediateItems.chunk(EnumChunkType.RARE), new ItemStack(ModItems.powder_desh_mix.get()));
        setRecipe(Items.SAND, new ItemStack(ModItems.dust.get(), 2));
        setRecipe(ModBlocks.block_slag.get(), new ItemStack(ModFuelItems.powder_cement.get(), 4));
        setRecipe(ModBlocks.ore_aluminium.get(), ModIntermediateItems.chunk(EnumChunkType.CRYOLITE, 2));

        setRecipe(ModIntermediateItems.drive(EnumDriveType.FLASH_BROKEN), new ItemStack(ModItems.powder_polymer.get(), 2));
        setRecipe(ModIntermediateItems.drive(EnumDriveType.DISK_BROKEN), new ItemStack(ModItems.powder_polymer.get(), 4));

        // (the logWood, plankWood, treeSapling and dustLapis loops are tag sweeps in ShredderRecipes)

        // port: 1.7's EnumBedrockOre loop (ore_bedrock, ore_centrifuged, ... ore_seared -> ore_enriched) has no rows;
        // that legacy bedrock ore chain is not in the port

        // 1.7 Items.skull, metas 0 to 4
        for (Item skull : new Item[] {Items.SKELETON_SKULL, Items.WITHER_SKELETON_SKULL, Items.ZOMBIE_HEAD, Items.PLAYER_HEAD, Items.CREEPER_HEAD}) {
            setRecipe(skull, new ItemStack(ModFuelItems.biomass.get(), 4));
        }

        /* Crystal processing */
        setRecipe(ModFuelItems.ingot_schraranium.get(), new ItemStack(ModItems.nugget_schrabidium.get(), 2));
        setRecipe(ModFuelItems.crystal_coal.get(), new ItemStack(ModFuelItems.powder_coal.get(), 3));
        setRecipe(ModItems.crystal_iron.get(), new ItemStack(ModItems.powder_iron.get(), 3));
        setRecipe(ModItems.crystal_gold.get(), new ItemStack(ModItems.powder_gold.get(), 3));
        setRecipe(ModItems.crystal_redstone.get(), new ItemStack(Items.REDSTONE, 8));
        setRecipe(ModItems.crystal_lapis.get(), new ItemStack(ModItems.powder_lapis.get(), 8));
        setRecipe(ModItems.crystal_diamond.get(), new ItemStack(ModItems.powder_diamond.get(), 3));
        setRecipe(ModItems.crystal_uranium.get(), new ItemStack(ModItems.powder_uranium.get(), 3));
        setRecipe(ModItems.crystal_plutonium.get(), new ItemStack(ModItems.powder_plutonium.get(), 3));
        setRecipe(ModItems.crystal_thorium.get(), new ItemStack(ModItems.powder_thorium.get(), 3));
        setRecipe(ModItems.crystal_titanium.get(), new ItemStack(ModItems.powder_titanium.get(), 3));
        setRecipe(ModItems.crystal_sulfur.get(), new ItemStack(ModItems.sulfur.get(), 8));
        setRecipe(ModItems.crystal_niter.get(), new ItemStack(ModItems.niter.get(), 8));
        setRecipe(ModItems.crystal_copper.get(), new ItemStack(ModItems.powder_copper.get(), 3));
        setRecipe(ModItems.crystal_tungsten.get(), new ItemStack(ModItems.powder_tungsten.get(), 3));
        setRecipe(ModItems.crystal_aluminium.get(), new ItemStack(ModItems.powder_aluminium.get(), 3));
        setRecipe(ModItems.crystal_fluorite.get(), new ItemStack(ModItems.fluorite.get(), 8));
        setRecipe(ModItems.crystal_beryllium.get(), new ItemStack(ModItems.powder_beryllium.get(), 3));
        setRecipe(ModItems.crystal_lead.get(), new ItemStack(ModItems.powder_lead.get(), 3));
        setRecipe(ModItems.crystal_schraranium.get(), new ItemStack(ModItems.nugget_schrabidium.get(), 3));
        setRecipe(ModItems.crystal_schrabidium.get(), new ItemStack(ModItems.powder_schrabidium.get(), 3));
        setRecipe(ModItems.crystal_rare.get(), new ItemStack(ModItems.powder_desh_mix.get(), 2));
        setRecipe(ModItems.crystal_phosphorus.get(), new ItemStack(ModFuelItems.powder_fire.get(), 8));
        setRecipe(ModItems.crystal_trixite.get(), new ItemStack(ModItems.powder_plutonium.get(), 6));
        setRecipe(ModItems.crystal_lithium.get(), new ItemStack(ModItems.powder_lithium.get(), 3));
        setRecipe(ModItems.crystal_starmetal.get(), new ItemStack(ModItems.powder_dura_steel.get(), 6));
        setRecipe(ModItems.crystal_cobalt.get(), new ItemStack(ModItems.powder_cobalt.get(), 3));

        /* Misc recycling */
        setRecipe(ModDecoContent.steel_poles.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 2));
        setRecipe(ModDecoBlocks.steel_roof.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 9));
        setRecipe(ModDecoBlocks.steel_wall.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 9));
        setRecipe(ModDecoBlocks.steel_corner.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 18));
        setRecipe(ModDecoBlocks.steel_beam.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 3));
        // 1.7 any meta: metas 0..3 (steel, red, white, yellow) are one block item per colour
        for (String scaffold : new String[] {"steel_scaffold", "steel_scaffold_red", "steel_scaffold_white", "steel_scaffold_yellow"})
            setRecipe(item(scaffold), new ItemStack(ModItems.powder_steel_tiny.get(), 4));
        setRecipe(ModItems.coil_copper.get(), new ItemStack(ModItems.powder_red_copper.get(), 1));
        setRecipe(ModItems.coil_copper_torus.get(), new ItemStack(ModItems.powder_red_copper.get(), 2));
        setRecipe(ModItems.coil_gold.get(), new ItemStack(ModItems.powder_gold.get(), 1));
        setRecipe(ModItems.coil_gold_torus.get(), new ItemStack(ModItems.powder_gold.get(), 2));
        setRecipe(ModItems.coil_tungsten.get(), new ItemStack(ModItems.powder_tungsten.get(), 1));
        setRecipe(ModItems.coil_magnetized_tungsten.get(), new ItemStack(ModItems.powder_magnetized_tungsten.get(), 1));
        setRecipe(ModStorageBlocks.crate_iron.get(), new ItemStack(ModItems.powder_iron.get(), 8));
        setRecipe(ModStorageBlocks.crate_steel.get(), new ItemStack(ModItems.powder_steel.get(), 8));
        setRecipe(ModDecoCrateBlocks.crate_tungsten.get(), new ItemStack(ModItems.powder_tungsten.get(), 36));
        setRecipe(Items.ANVIL, new ItemStack(ModItems.powder_iron.get(), 31));
        setRecipe(ModDecoContent.dungeon_chain.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 1)); // ModBlocks.chain
        setRecipe(ModSteelBlocks.steel_grate.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 3));
        setRecipe(ModItems.pipes_steel.get(), new ItemStack(ModItems.powder_steel.get(), 27));
        // ModItems.bedrock_ore at any meta: every grade of every type (1.7 ItemBedrockOreNew)
        for (Grade grade : Grade.values()) {
            for (BedrockOreType type : BedrockOreType.VALUES) {
                setRecipe(ModBedrockOreItems.get(grade, type), new ItemStack(Items.GRAVEL));
            }
        }

        /* Sellafite scrapping */
        setRecipe(ModFalloutBlocks.sellafield_slaked.get(), new ItemStack(Items.GRAVEL));
        setRecipe(ModTerrainBlocks.sellafield(0), new ItemStack(ModItems.scrap_nuclear.get(), 1));
        setRecipe(ModTerrainBlocks.sellafield(1), new ItemStack(ModItems.scrap_nuclear.get(), 2));
        setRecipe(ModTerrainBlocks.sellafield(2), new ItemStack(ModItems.scrap_nuclear.get(), 3));
        setRecipe(ModTerrainBlocks.sellafield(3), new ItemStack(ModItems.scrap_nuclear.get(), 5));
        setRecipe(ModTerrainBlocks.sellafield(4), new ItemStack(ModItems.scrap_nuclear.get(), 7));
        setRecipe(ModTerrainBlocks.sellafield(5), new ItemStack(ModItems.scrap_nuclear.get(), 15));

        /* Fracking debris scrapping */
        setRecipe(ModFallingBlocks.dirt_dead.get(), new ItemStack(ModItems.scrap_oil.get(), 1));
        setRecipe(ModFallingBlocks.dirt_oily.get(), new ItemStack(ModItems.scrap_oil.get(), 1));
        setRecipe(ModFallingBlocks.sand_dirty.get(), new ItemStack(ModItems.scrap_oil.get(), 1));
        setRecipe(ModFallingBlocks.sand_dirty_red.get(), new ItemStack(ModItems.scrap_oil.get(), 1));
        setRecipe(ModBlocks.stone_cracked.get(), new ItemStack(ModItems.scrap_oil.get(), 1));
        setRecipe(ModTerrainBlocks.stone_porous.get(), new ItemStack(ModItems.scrap_oil.get(), 1));

        /* Deco pipe recycling */
        setRecipe(ModDecoContent.deco_pipe.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_green.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_green_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_red.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_marked.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rim.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rim_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rim_green.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rim_green_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rim_red.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_rim_marked.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_quad.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_quad_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_quad_green.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_quad_green_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_quad_red.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_quad_marked.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_framed.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_framed_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_framed_green.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_framed_green_rusted.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_framed_red.get(), new ItemStack(ModItems.powder_steel.get(), 1));
        setRecipe(ModDecoContent.deco_pipe_framed_marked.get(), new ItemStack(ModItems.powder_steel.get(), 1));

        /* Wool and clay scrapping */
        // (the 16 stained clay and wool metas are the terracotta and wool tag sweeps in ShredderRecipes)

        /* Shredding bobbleheads */
        // the bobblehead is one item: the row is the plain figure's (NONE), DecoCrateMachineRecipes.bobbleScrap the others
        BobbleType type = BobbleType.values()[0];
        setRecipe(ModDecoCrateBlocks.bobblehead.get(), new ItemStack(item("scrap_plastic_" + type.scrap.toLowerCase(Locale.US)), 1));

        /* Debris shredding */
        setRecipe(ModItems.debris_concrete.get(), new ItemStack(ModItems.scrap_nuclear.get(), 2));
        setRecipe(ModItems.debris_shrapnel.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 5));
        setRecipe(ModItems.debris_exchanger.get(), new ItemStack(ModItems.powder_steel.get(), 3));
        setRecipe(ModItems.debris_element.get(), new ItemStack(ModItems.scrap_nuclear.get(), 4));
        setRecipe(ModItems.debris_metal.get(), new ItemStack(ModItems.powder_steel_tiny.get(), 3));
        setRecipe(ModItems.debris_graphite.get(), new ItemStack(ModFuelItems.powder_coal.get(), 1));

        // (GC and AR compat: neither mod exists on any target)

        // port: modern counterparts of rows above, so they do not fall through to the tag sweep
        setRecipe(Items.DEEPSLATE_DIAMOND_ORE, new ItemStack(ModFallingBlocks.gravel_diamond.get(), 2));
        setRecipe(Items.DEEPSLATE, new ItemStack(Items.GRAVEL));
        setRecipe(Items.COBBLED_DEEPSLATE, new ItemStack(Items.GRAVEL));
    }

    /** 1.7 {@code ShredderRecipes.setRecipe}: the first row of an input wins. */
    private void setRecipe(ItemLike in, ItemStack out) {
        Item item = in.asItem();
        String id = id(item);
        if (!inputs.add(id)) return;
        register(id, new Row(Either.right(item), ItemOut.of(out)));
    }

    private static String id(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id.getNamespace().equals("minecraft") ? "minecraft_" + id.getPath() : id.getPath();
    }

    /** An HBM item by id, for split 1.7 metadata stacks without a field of their own. */
    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(Ids.of("hbm", path))
                .orElseThrow(() -> new IllegalStateException("no hbm:" + path));
    }
}
