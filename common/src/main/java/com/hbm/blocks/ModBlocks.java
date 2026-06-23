package com.hbm.blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import com.hbm.Constants;
import com.hbm.blocks.generic.HbmStairBlock;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registry.DeferredRegister;
import com.hbm.registry.RegistryObject;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {

    private ModBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);

    public record BlockEntry(RegistryObject<Block> block, HbmTab tab) {}
    public static final List<BlockEntry> ALL = new ArrayList<>();

    private static RegistryObject<Block> block(String name, HbmTab tab, UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<Block> ro = BLOCKS.registerSimpleBlock(name, props);
        ALL.add(new BlockEntry(ro, tab));
        return ro;
    }

    private static RegistryObject<Block> oreXp(String name, HbmTab tab, int xp, UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<Block> ro = BLOCKS.registerBlock(name, p -> new DropExperienceBlock(ConstantInt.of(xp), p), props);
        ALL.add(new BlockEntry(ro, tab));
        return ro;
    }

    private static RegistryObject<Block> slab(String name, HbmTab tab, UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<Block> ro = BLOCKS.registerBlock(name, SlabBlock::new, props);
        ALL.add(new BlockEntry(ro, tab));
        return ro;
    }

    private static RegistryObject<Block> stairs(String name, HbmTab tab, Supplier<Block> base, UnaryOperator<BlockBehaviour.Properties> props) {
        RegistryObject<Block> ro = BLOCKS.registerBlock(name, p -> new HbmStairBlock(base.get().defaultBlockState(), p), props);
        ALL.add(new BlockEntry(ro, tab));
        return ro;
    }

    public static final RegistryObject<Block> fatduck = block("fatduck", HbmTab.BLOCKS, p -> p);
    public static final RegistryObject<Block> crystal_hardened = block("crystal_hardened", HbmTab.BLOCKS, p -> p.strength(15.0F));
    public static final RegistryObject<Block> reinforced_stone = block("reinforced_stone", HbmTab.BLOCKS, p -> p.strength(15.0F, 100.0F));
    public static final RegistryObject<Block> brick_concrete_cracked = block("brick_concrete_cracked", HbmTab.BLOCKS, p -> p.strength(15.0F, 60.0F));
    public static final RegistryObject<Block> brick_concrete_broken = block("brick_concrete_broken", HbmTab.BLOCKS, p -> p.strength(15.0F, 45.0F));
    public static final RegistryObject<Block> brick_light = block("brick_light", HbmTab.BLOCKS, p -> p.strength(15.0F, 20.0F));
    public static final RegistryObject<Block> reinforced_sand = block("reinforced_sand", HbmTab.BLOCKS, p -> p.strength(15.0F, 40.0F));
    public static final RegistryObject<Block> brick_obsidian = block("brick_obsidian", HbmTab.BLOCKS, p -> p.strength(15.0F, 120.0F));
    public static final RegistryObject<Block> cmb_brick = block("cmb_brick", HbmTab.BLOCKS, p -> p.strength(25.0F, 5000.0F));
    public static final RegistryObject<Block> concrete = block("concrete", HbmTab.BLOCKS, p -> p.strength(15.0F, 140.0F));
    public static final RegistryObject<Block> concrete_smooth = block("concrete_smooth", HbmTab.BLOCKS, p -> p.strength(15.0F, 140.0F));
    public static final RegistryObject<Block> concrete_asbestos = block("concrete_asbestos", HbmTab.BLOCKS, p -> p.strength(15.0F, 1500.0F));
    public static final RegistryObject<Block> ore_bedrock_oil = block("ore_bedrock_oil", HbmTab.RESOURCES, p -> p);
    public static final RegistryObject<Block> ore_oil_empty = block("ore_oil_empty", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> stone_gneiss = block("stone_gneiss", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> gneiss_brick = block("gneiss_brick", HbmTab.BLOCKS, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> gneiss_tile = block("gneiss_tile", HbmTab.BLOCKS, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> gneiss_chiseled = block("gneiss_chiseled", HbmTab.BLOCKS, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> meteor_polished = block("meteor_polished", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> meteor_brick = block("meteor_brick", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> meteor_brick_mossy = block("meteor_brick_mossy", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> meteor_brick_cracked = block("meteor_brick_cracked", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> meteor_brick_chiseled = block("meteor_brick_chiseled", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> meteor_battery = block("meteor_battery", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> brick_jungle = block("brick_jungle", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> brick_jungle_cracked = block("brick_jungle_cracked", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> brick_dungeon = block("brick_dungeon", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> brick_dungeon_flat = block("brick_dungeon_flat", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> brick_dungeon_tile = block("brick_dungeon_tile", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> brick_dungeon_circle = block("brick_dungeon_circle", HbmTab.BLOCKS, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> block_niter = block("block_niter", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> block_sulfur = block("block_sulfur", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> block_titanium = block("block_titanium", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_copper = block("block_copper", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_red_copper = block("block_red_copper", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_tungsten = block("block_tungsten", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_aluminium = block("block_aluminium", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_fluorite = block("block_fluorite", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> block_steel = block("block_steel", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_beryllium = block("block_beryllium", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_euphemium = block("block_euphemium", HbmTab.BLOCKS, p -> p.strength(5.0F, 30000.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_dineutronium = block("block_dineutronium", HbmTab.BLOCKS, p -> p.strength(5.0F, 60000.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_combine_steel = block("block_combine_steel", HbmTab.BLOCKS, p -> p.strength(5.0F, 300.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_desh = block("block_desh", HbmTab.BLOCKS, p -> p.strength(5.0F, 300.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_dura_steel = block("block_dura_steel", HbmTab.BLOCKS, p -> p.strength(5.0F, 300.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_saturnite = block("block_saturnite", HbmTab.BLOCKS, p -> p.strength(6.0F, 400.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> tektite = block("tektite", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F).sound(SoundType.STONE));
    public static final RegistryObject<Block> ore_tektite_osmiridium = block("ore_tektite_osmiridium", HbmTab.RESOURCES, p -> p.strength(2.5F, 20.0F).sound(SoundType.STONE));
    public static final RegistryObject<Block> deco_titanium = block("deco_titanium", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_red_copper = block("deco_red_copper", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_tungsten = block("deco_tungsten", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_aluminium = block("deco_aluminium", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_steel = block("deco_steel", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_rusty_steel = block("deco_rusty_steel", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_lead = block("deco_lead", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> deco_beryllium = block("deco_beryllium", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> stone_cracked = block("stone_cracked", HbmTab.RESOURCES, p -> p.strength(5.0F));
    public static final RegistryObject<Block> block_smore = block("block_smore", HbmTab.BLOCKS, p -> p.strength(15.0F, 450.0F));
    public static final RegistryObject<Block> block_starmetal = block("block_starmetal", HbmTab.BLOCKS, p -> p.strength(5.0F, 300.0F).sound(SoundType.METAL));
    public static final RegistryObject<Block> block_cobalt = block("block_cobalt", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> block_foam = block("block_foam", HbmTab.BLOCKS, p -> p.strength(0.5F, 0.0F).sound(SoundType.SNOW));
    public static final RegistryObject<Block> basalt = block("basalt", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> basalt_smooth = block("basalt_smooth", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> basalt_brick = block("basalt_brick", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> basalt_polished = block("basalt_polished", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> basalt_tiles = block("basalt_tiles", HbmTab.BLOCKS, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> seal_frame = block("seal_frame", HbmTab.MACHINES, p -> p.strength(10.0F, 100.0F));
    public static final RegistryObject<Block> machine_transformer = block("machine_transformer", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> machine_transformer_20 = block("machine_transformer_20", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> machine_transformer_dnt = block("machine_transformer_dnt", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> machine_transformer_dnt_20 = block("machine_transformer_dnt_20", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> muffler = block("muffler", HbmTab.BLOCKS, p -> p.strength(0.8F).sound(SoundType.WOOL));
    public static final RegistryObject<Block> struct_launcher = block("struct_launcher", HbmTab.MISSILES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> struct_scaffold = block("struct_scaffold", HbmTab.MISSILES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> fusion_heater = block("fusion_heater", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> fusion_hatch = block("fusion_hatch", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> fusion_core_block = block("fusion_core_block", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> watz_element = block("watz_element", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> watz_cooler = block("watz_cooler", HbmTab.MACHINES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> pink_planks = block("pink_planks", HbmTab.BLOCKS, p -> p.sound(SoundType.WOOD));

    public static final RegistryObject<Block> ore_schrabidium = block("ore_schrabidium", HbmTab.RESOURCES, p -> p.strength(15.0F, 600.0F));
    public static final RegistryObject<Block> ore_thorium = block("ore_thorium", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_titanium = block("ore_titanium", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_sulfur = oreXp("ore_sulfur", HbmTab.RESOURCES, 2, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_niter = oreXp("ore_niter", HbmTab.RESOURCES, 2, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_copper = block("ore_copper", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_tungsten = block("ore_tungsten", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_aluminium = block("ore_aluminium", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_fluorite = oreXp("ore_fluorite", HbmTab.RESOURCES, 2, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_lead = block("ore_lead", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_beryllium = block("ore_beryllium", HbmTab.RESOURCES, p -> p.strength(5.0F, 15.0F));
    public static final RegistryObject<Block> ore_lignite = block("ore_lignite", HbmTab.RESOURCES, p -> p.strength(5.0F, 15.0F));
    public static final RegistryObject<Block> ore_asbestos = oreXp("ore_asbestos", HbmTab.RESOURCES, 6, p -> p.strength(5.0F, 15.0F));
    public static final RegistryObject<Block> ore_rare = block("ore_rare", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_cobalt = oreXp("ore_cobalt", HbmTab.RESOURCES, 15, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_cinnabar = oreXp("ore_cinnabar", HbmTab.RESOURCES, 2, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_coltan = oreXp("ore_coltan", HbmTab.RESOURCES, 20, p -> p.strength(15.0F, 10.0F));
    public static final RegistryObject<Block> ore_australium = block("ore_australium", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_oil = block("ore_oil", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_iron = block("ore_gneiss_iron", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_gold = block("ore_gneiss_gold", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_copper = block("ore_gneiss_copper", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_asbestos = oreXp("ore_gneiss_asbestos", HbmTab.RESOURCES, 2, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_lithium = block("ore_gneiss_lithium", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_schrabidium = block("ore_gneiss_schrabidium", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_rare = block("ore_gneiss_rare", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_gneiss_gas = block("ore_gneiss_gas", HbmTab.RESOURCES, p -> p.strength(1.5F, 10.0F));
    public static final RegistryObject<Block> ore_tikite = block("ore_tikite", HbmTab.RESOURCES, p -> p.strength(5.0F, 10.0F));
    public static final RegistryObject<Block> ore_nether_cobalt = block("ore_nether_cobalt", HbmTab.RESOURCES, p -> p.strength(0.4F, 10.0F));
    public static final RegistryObject<Block> ore_nether_tungsten = block("ore_nether_tungsten", HbmTab.RESOURCES, p -> p.strength(0.4F, 10.0F));
    public static final RegistryObject<Block> ore_nether_sulfur = oreXp("ore_nether_sulfur", HbmTab.RESOURCES, 2, p -> p.strength(0.4F, 10.0F));
    public static final RegistryObject<Block> ore_nether_fire = block("ore_nether_fire", HbmTab.RESOURCES, p -> p.strength(0.4F, 10.0F));
    public static final RegistryObject<Block> ore_nether_plutonium = block("ore_nether_plutonium", HbmTab.RESOURCES, p -> p.strength(0.4F, 10.0F));
    public static final RegistryObject<Block> ore_nether_schrabidium = block("ore_nether_schrabidium", HbmTab.RESOURCES, p -> p.strength(15.0F, 600.0F));
    public static final RegistryObject<Block> block_meteor = block("block_meteor", HbmTab.RESOURCES, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> block_meteor_cobble = oreXp("block_meteor_cobble", HbmTab.RESOURCES, 0, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> block_meteor_broken = oreXp("block_meteor_broken", HbmTab.RESOURCES, 0, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> block_meteor_treasure = block("block_meteor_treasure", HbmTab.RESOURCES, p -> p.strength(15.0F, 360.0F));
    public static final RegistryObject<Block> waste_planks = block("waste_planks", HbmTab.RESOURCES, p -> p.strength(0.5F, 2.5F).sound(SoundType.WOOD));

    public static final RegistryObject<Block> reinforced_stone_slab = slab("reinforced_stone_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 60.0F));
    public static final RegistryObject<Block> brick_concrete_slab = slab("brick_concrete_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> brick_concrete_mossy_slab = slab("brick_concrete_mossy_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> brick_concrete_cracked_slab = slab("brick_concrete_cracked_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 30.0F));
    public static final RegistryObject<Block> brick_concrete_broken_slab = slab("brick_concrete_broken_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 22.0F));
    public static final RegistryObject<Block> reinforced_brick_slab = slab("reinforced_brick_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 150.0F));
    public static final RegistryObject<Block> brick_light_slab = slab("brick_light_slab", null, p -> p.strength(15.0F, 20.0F));
    public static final RegistryObject<Block> brick_compound_slab = slab("brick_compound_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 200.0F));
    public static final RegistryObject<Block> brick_asbestos_slab = slab("brick_asbestos_slab", HbmTab.BLOCKS, p -> p);
    public static final RegistryObject<Block> brick_fire_slab = slab("brick_fire_slab", null, p -> p.strength(15.0F, 35.0F));
    public static final RegistryObject<Block> reinforced_sand_slab = slab("reinforced_sand_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 20.0F));
    public static final RegistryObject<Block> brick_obsidian_slab = slab("brick_obsidian_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 60.0F));
    public static final RegistryObject<Block> cmb_brick_reinforced_slab = slab("cmb_brick_reinforced_slab", HbmTab.BLOCKS, p -> p.strength(25.0F, 25000.0F));
    public static final RegistryObject<Block> concrete_slab = slab("concrete_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_smooth_slab = slab("concrete_smooth_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_white_slab = slab("concrete_white_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_orange_slab = slab("concrete_orange_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_magenta_slab = slab("concrete_magenta_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_light_blue_slab = slab("concrete_light_blue_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_yellow_slab = slab("concrete_yellow_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_lime_slab = slab("concrete_lime_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_pink_slab = slab("concrete_pink_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_gray_slab = slab("concrete_gray_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_silver_slab = slab("concrete_silver_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_cyan_slab = slab("concrete_cyan_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_purple_slab = slab("concrete_purple_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_blue_slab = slab("concrete_blue_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_brown_slab = slab("concrete_brown_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_green_slab = slab("concrete_green_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_red_slab = slab("concrete_red_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_black_slab = slab("concrete_black_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> concrete_asbestos_slab = slab("concrete_asbestos_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 70.0F));
    public static final RegistryObject<Block> ducrete_smooth_slab = slab("ducrete_smooth_slab", HbmTab.BLOCKS, p -> p.strength(20.0F, 250.0F));
    public static final RegistryObject<Block> ducrete_slab = slab("ducrete_slab", HbmTab.BLOCKS, p -> p.strength(20.0F, 250.0F));
    public static final RegistryObject<Block> ducrete_brick_slab = slab("ducrete_brick_slab", HbmTab.BLOCKS, p -> p.strength(15.0F, 375.0F));
    public static final RegistryObject<Block> ducrete_reinforced_slab = slab("ducrete_reinforced_slab", HbmTab.BLOCKS, p -> p.strength(20.0F, 500.0F));
    public static final RegistryObject<Block> tile_lab_slab = slab("tile_lab_slab", HbmTab.BLOCKS, p -> p.strength(1.0F, 10.0F).sound(SoundType.GLASS));
    public static final RegistryObject<Block> tile_lab_cracked_slab = slab("tile_lab_cracked_slab", HbmTab.BLOCKS, p -> p.strength(1.0F, 10.0F).sound(SoundType.GLASS));
    public static final RegistryObject<Block> tile_lab_broken_slab = slab("tile_lab_broken_slab", HbmTab.BLOCKS, p -> p.strength(1.0F, 10.0F).sound(SoundType.GLASS));
    public static final RegistryObject<Block> pink_slab = slab("pink_slab", null, p -> p.sound(SoundType.WOOD));

    public static final RegistryObject<Block> reinforced_stone_stairs = stairs("reinforced_stone_stairs", HbmTab.BLOCKS, reinforced_stone, p -> p.strength(15.0F, 76.0F));
    public static final RegistryObject<Block> brick_concrete_cracked_stairs = stairs("brick_concrete_cracked_stairs", HbmTab.BLOCKS, brick_concrete_cracked, p -> p.strength(15.0F, 40.0F));
    public static final RegistryObject<Block> brick_concrete_broken_stairs = stairs("brick_concrete_broken_stairs", HbmTab.BLOCKS, brick_concrete_broken, p -> p.strength(15.0F, 30.0F));
    public static final RegistryObject<Block> brick_light_stairs = stairs("brick_light_stairs", HbmTab.BLOCKS, brick_light, p -> p.strength(15.0F, 20.0F));
    public static final RegistryObject<Block> reinforced_sand_stairs = stairs("reinforced_sand_stairs", HbmTab.BLOCKS, reinforced_sand, p -> p.strength(15.0F, 32.0F));
    public static final RegistryObject<Block> brick_obsidian_stairs = stairs("brick_obsidian_stairs", HbmTab.BLOCKS, brick_obsidian, p -> p.strength(15.0F, 96.0F));
    public static final RegistryObject<Block> concrete_stairs = stairs("concrete_stairs", HbmTab.BLOCKS, concrete, p -> p.strength(15.0F, 94.0F));
    public static final RegistryObject<Block> concrete_smooth_stairs = stairs("concrete_smooth_stairs", HbmTab.BLOCKS, concrete_smooth, p -> p.strength(15.0F, 94.0F));
    public static final RegistryObject<Block> concrete_asbestos_stairs = stairs("concrete_asbestos_stairs", HbmTab.BLOCKS, concrete_asbestos, p -> p.strength(15.0F, 94.0F));
    public static final RegistryObject<Block> pink_stairs = stairs("pink_stairs", null, pink_planks, p -> p.sound(SoundType.WOOD));

    public static void init() {}
}
