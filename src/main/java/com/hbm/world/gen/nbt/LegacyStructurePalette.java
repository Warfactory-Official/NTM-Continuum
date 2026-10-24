package com.hbm.world.gen.nbt;

import com.hbm.Constants;
import com.hbm.blocks.ModDecoContent;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.machine.rbmk.RBMKBase;
import dev.golem.core.Ids;
import dev.golem.core.data.Tags;
import dev.golem.core.world.LegacyBlockStates;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

/**
 * The 1.7 structure palette ({@code Name} = 1.7 registry name, {@code Properties.meta} = 4-bit metadata) mapped onto
 * this port's flattened block states. The mapping table, in order of precedence:
 *
 * <ol>
 *   <li><b>{@code minecraft:*}</b>: the 1.7.10 numeric id from {@link #VANILLA_IDS}, then vanilla's own
 *       flattening map and data fixer through {@link LegacyBlockStates#upgrade} (so the result is right on every
 *       node, e.g. {@code tallgrass:1} is {@code grass} on 1.20.1 and {@code short_grass} on 26.1). Doors are
 *       rebuilt from both halves ({@link #door}), as vanilla's chunk fixer does.</li>
 *   <li><b>{@code hbm:tile.*}</b> with a {@link #RULES} entry: the 1.7 subtype split into its flattened id (multi
 *       slabs, CRTs, toasters, dead plants, wood structures) and the per-class metadata layout (see each rule).</li>
 *   <li><b>{@code hbm:tile.*}</b> without one: {@code hbm:<name>} with generic property rules - stairs use vanilla
 *       stair metadata, {@code BlockDummyable} multiblocks place their core only (meta &gt;= 12) and skip the cells,
 *       a horizontal {@code facing} takes 1.7's ForgeDirection 2..5, {@code axis} takes the pillar bits,
 *       {@code rotation}/{@code height}/{@code age}/{@code damage}/{@code shade} take the metadata, {@code layers}
 *       metadata + 1, {@code color} the dye index and an enum {@code type} its ordinal.</li>
 *   <li>Anything else becomes air (1.7's own {@code Block.getBlockFromName} fallback) and is logged once.</li>
 * </ol>
 *
 * <p>Rotation stays in metadata space ({@link #transform}): 1.7 rotated the stored metadata by the 1.7 class of the
 * block ({@link INBTBlockTransformable}), and only then did the block reach the world; blocks whose 1.7 class had
 * no rule (e.g. {@code machine_electric_furnace}) were not rotated at all, and still are not.
 */
public final class LegacyStructurePalette {

    private LegacyStructurePalette() {}

    private static final ThreadLocal<Boolean> SPOTLIGHTS_WORKING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * Runs {@code build} (an {@link NBTStructure} build on this thread) with 1.7's {@code Spotlight.disableOnGeneration}
     * set to false: spotlights keep their block and are placed unbroken instead of broken and switched off (hbm-space
     * built its "martian" base that way). Per thread, so builds on worldgen threads keep the default.
     */
    public static void withSpotlightsWorking(Runnable build) {
        boolean previous = SPOTLIGHTS_WORKING.get();
        SPOTLIGHTS_WORKING.set(Boolean.TRUE);
        try {
            build.run();
        } finally {
            SPOTLIGHTS_WORKING.set(previous);
        }
    }

    public enum Kind {
        /** Place {@link Resolved#state()} as-is. */
        STATE,
        /** Unresolved legacy block: runtime retains the authored air fallback; conversion must report it. */
        UNKNOWN,
        /** A multiblock core: place the state, then let the multiblock fill its own cells. */
        CORE,
        /** A multiblock cell the core fills: place nothing. */
        CELL,
        /** Half of a two-block door; the final state needs the other half's metadata ({@link #door}). */
        DOOR
    }

    public record Resolved(BlockState state, Kind kind) {
        static Resolved of(BlockState state) { return new Resolved(state, Kind.STATE); }
    }

    /** A 1.7 block reference after rotation: registry name and metadata. */
    public record Legacy(String name, int meta) {}

    // 1.7.10 vanilla numeric block ids (the pre-flattening key BlockStateData is indexed by)

    private static final String[] VANILLA_1_7 = {
            "air", "stone", "grass", "dirt", "cobblestone", "planks", "sapling", "bedrock", "flowing_water", "water",
            "flowing_lava", "lava", "sand", "gravel", "gold_ore", "iron_ore", "coal_ore", "log", "leaves", "sponge",
            "glass", "lapis_ore", "lapis_block", "dispenser", "sandstone", "noteblock", "bed", "golden_rail",
            "detector_rail", "sticky_piston", "web", "tallgrass", "deadbush", "piston", "piston_head", "wool",
            "piston_extension", "yellow_flower", "red_flower", "brown_mushroom", "red_mushroom", "gold_block",
            "iron_block", "double_stone_slab", "stone_slab", "brick_block", "tnt", "bookshelf", "mossy_cobblestone",
            "obsidian", "torch", "fire", "mob_spawner", "oak_stairs", "chest", "redstone_wire", "diamond_ore",
            "diamond_block", "crafting_table", "wheat", "farmland", "furnace", "lit_furnace", "standing_sign",
            "wooden_door", "ladder", "rail", "stone_stairs", "wall_sign", "lever", "stone_pressure_plate", "iron_door",
            "wooden_pressure_plate", "redstone_ore", "lit_redstone_ore", "unlit_redstone_torch", "redstone_torch",
            "stone_button", "snow_layer", "ice", "snow", "cactus", "clay", "reeds", "jukebox", "fence", "pumpkin",
            "netherrack", "soul_sand", "glowstone", "portal", "lit_pumpkin", "cake", "unpowered_repeater",
            "powered_repeater", "stained_glass", "trapdoor", "monster_egg", "stonebrick", "brown_mushroom_block",
            "red_mushroom_block", "iron_bars", "glass_pane", "melon_block", "pumpkin_stem", "melon_stem", "vine",
            "fence_gate", "brick_stairs", "stone_brick_stairs", "mycelium", "waterlily", "nether_brick",
            "nether_brick_fence", "nether_brick_stairs", "nether_wart", "enchanting_table", "brewing_stand", "cauldron",
            "end_portal", "end_portal_frame", "end_stone", "dragon_egg", "redstone_lamp", "lit_redstone_lamp",
            "double_wooden_slab", "wooden_slab", "cocoa", "sandstone_stairs", "emerald_ore", "ender_chest",
            "tripwire_hook", "tripwire", "emerald_block", "spruce_stairs", "birch_stairs", "jungle_stairs",
            "command_block", "beacon", "cobblestone_wall", "flower_pot", "carrots", "potatoes", "wooden_button",
            "skull", "anvil", "trapped_chest", "light_weighted_pressure_plate", "heavy_weighted_pressure_plate",
            "unpowered_comparator", "powered_comparator", "daylight_detector", "redstone_block", "quartz_ore",
            "hopper", "quartz_block", "quartz_stairs", "activator_rail", "dropper", "stained_hardened_clay",
            "stained_glass_pane", "leaves2", "log2", "acacia_stairs", "dark_oak_stairs", null, null, null, null, null,
            "hay_block", "carpet", "hardened_clay", "coal_block", "packed_ice", "double_plant"
    };

    private static final Map<String, Integer> VANILLA_IDS = new HashMap<>();

    static {
        for (int i = 0; i < VANILLA_1_7.length; i++) {
            if (VANILLA_1_7[i] != null) VANILLA_IDS.put("minecraft:" + VANILLA_1_7[i], i);
        }
    }

    // rotation rules (1.7 class -> INBTBlockTransformable helper)

    enum Rot { NONE, DECO, DECO_MODEL, DECO_MODEL_SHIFTED, STAIRS, TRAPDOOR, PILLAR, DIRECTIONAL, TORCH, DOOR, LEVER,
        VINE, BOBBLE, CHAIN, DUMMYABLE, FLOODLIGHT, SPOTLIGHT }

    private static final Map<String, Rot> VANILLA_ROT = new HashMap<>();

    static {
        for (String s : List.of("oak_stairs", "stone_stairs", "brick_stairs", "stone_brick_stairs", "nether_brick_stairs",
                "sandstone_stairs", "spruce_stairs", "birch_stairs", "jungle_stairs", "quartz_stairs", "acacia_stairs",
                "dark_oak_stairs")) VANILLA_ROT.put("minecraft:" + s, Rot.STAIRS);
        for (String s : List.of("log", "log2", "hay_block")) VANILLA_ROT.put("minecraft:" + s, Rot.PILLAR);
        for (String s : List.of("bed", "fence_gate", "pumpkin", "lit_pumpkin", "unpowered_repeater", "powered_repeater",
                "unpowered_comparator", "powered_comparator", "cocoa", "end_portal_frame", "tripwire_hook"))
            VANILLA_ROT.put("minecraft:" + s, Rot.DIRECTIONAL);
        for (String s : List.of("torch", "redstone_torch", "unlit_redstone_torch", "stone_button", "wooden_button"))
            VANILLA_ROT.put("minecraft:" + s, Rot.TORCH);
        for (String s : List.of("wooden_door", "iron_door")) VANILLA_ROT.put("minecraft:" + s, Rot.DOOR);
        VANILLA_ROT.put("minecraft:lever", Rot.LEVER);
        for (String s : List.of("wall_sign", "standing_sign", "ladder")) VANILLA_ROT.put("minecraft:" + s, Rot.DECO);
        VANILLA_ROT.put("minecraft:vine", Rot.VINE);
        VANILLA_ROT.put("minecraft:trapdoor", Rot.TRAPDOOR);
    }

    // hbm rules

    @FunctionalInterface
    interface Mapper {
        BlockState map(BlockState base, int meta);
    }

    record Rule(Rot rot, IntFunction<String> id, Mapper mapper, boolean door) {}

    private static final Map<String, Rule> RULES = new HashMap<>();

    private static void rule(String name, Rot rot, Mapper mapper) {
        RULES.put(name, new Rule(rot, null, mapper, false));
    }

    private static void rule(String name, Rot rot, IntFunction<String> id, Mapper mapper) {
        RULES.put(name, new Rule(rot, id, mapper, false));
    }

    /** 1.7 ForgeDirection ordinal (0 down, 1 up, 2 north, 3 south, 4 west, 5 east) = Direction 3D data value. */
    private static Direction forge(int ordinal) {
        return Direction.from3DDataValue(ordinal);
    }

    /** Horizontal {@code facing} = ForgeDirection(meta) for metas 2..5. */
    private static final Mapper FACING = (base, meta) -> meta >= 2 && meta <= 5 ? with(base, "facing", forge(meta)) : base;
    /** Horizontal {@code facing} = the opposite of ForgeDirection(meta): blocks whose port facing is the look direction. */
    private static final Mapper FACING_OPPOSITE = (base, meta) ->
            meta >= 2 && meta <= 5 ? with(base, "facing", forge(meta).getOpposite()) : base;
    /** Six-way {@code facing} = ForgeDirection(meta). */
    private static final Mapper FACING6 = (base, meta) -> meta <= 5 ? with(base, "facing", forge(meta)) : base;
    /** 1.7 {@code BlockRotatedPillar}: bits 2..3, 0 = y, 4 = x, 8 = z. */
    private static final Mapper AXIS = (base, meta) -> switch (meta & 12) {
        case 4 -> with(base, "axis", Direction.Axis.X);
        case 8 -> with(base, "axis", Direction.Axis.Z);
        default -> with(base, "axis", Direction.Axis.Y);
    };
    /** Copies whatever the 1.7 vanilla analog (the 1.7 superclass) maps this metadata to. */
    private static Mapper analog(int vanillaId) {
        return (base, meta) -> copyShared(LegacyBlockStates.upgrade(vanillaId, meta), base);
    }
    /** 1.7 {@code IBlockSideRotation}/deco-model look quadrant: meta % 4 = floor(yaw * 4 / 360 + 0.5) & 3. */
    private static final Direction[] LOOK = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    private static final Mapper LOOK_QUADRANT = (base, meta) -> with(base, "facing", LOOK[meta & 3]);

    private static IntFunction<String> variants(String prefix, String... names) {
        return meta -> meta >= 0 && meta < names.length ? prefix + names[meta] : null;
    }

    private static void slab(String name, boolean doubled, String... variants) {
        rule(name, Rot.NONE, meta -> {
            int v = meta & 7;
            return v < variants.length ? variants[v] + "_slab" : null;
        }, (base, meta) -> with(base, "type", doubled ? "double" : (meta & 8) != 0 ? "top" : "bottom"));
    }

    static {
        // 1.7 DecoBlock: meta = ForgeDirection of the look direction; port facing = the same direction
        for (String s : List.of("steel_beam", "steel_wall", "steel_corner", "steel_roof")) rule(s, Rot.DECO, FACING);
        // 1.7 DecoPoleSatelliteReceiver/DecoTapeRecorder (INBTBlockTransformable) and DecoSteelPoles/DecoPoleTop
        // (not): meta = ForgeDirection opposite to the look; DecoModelBlock's facing is the look direction
        rule("pole_satellite_receiver", Rot.DECO, FACING_OPPOSITE);
        rule("tape_recorder", Rot.DECO, FACING_OPPOSITE);
        rule("steel_poles", Rot.NONE, FACING_OPPOSITE);
        rule("pole_top", Rot.NONE, FACING_OPPOSITE);
        rule("charger", Rot.DECO, FACING);
        // BlockMachineBase subclasses rotate; the BlockContainer machines did not implement INBTBlockTransformable
        rule("machine_diesel", Rot.DECO, FACING);
        rule("machine_microwave", Rot.DECO, FACING);
        for (String s : List.of("machine_electric_furnace_off", "machine_boiler_off", "geiger", "radiorec",
                "skeleton_holder", "crate_iron")) rule(s, Rot.NONE, FACING);
        rule("anvil_lead", Rot.NONE, FACING_OPPOSITE); // NTMAnvil faces away from the ForgeDirection in its meta
        rule("wood_barrier", Rot.NONE, FACING_OPPOSITE); // meta = the clicked side; the port faces away from it
        rule("red_connector", Rot.NONE, FACING6);
        rule("fluid_duct_gauge", Rot.DECO, (base, meta) -> meta <= 5 ? with(base, "gauge", forge(meta)) : base);
        // BlockLadder / BlockTrapDoor / BlockRailBase subclasses: vanilla's own metadata layout
        rule("ladder_steel", Rot.DECO, analog(65));
        rule("ladder_tungsten", Rot.DECO, analog(65));
        rule("trapdoor_steel", Rot.TRAPDOOR, analog(96));
        rule("rail_narrow", Rot.NONE, analog(66));
        for (String s : List.of("door_bunker", "door_metal", "door_office")) {
            RULES.put(s, new Rule(Rot.DOOR, null, (base, meta) -> base, true));
        }
        // BlockPipe and BlockRotatablePillar
        for (String s : List.of("deco_pipe", "deco_pipe_framed", "deco_pipe_framed_green_rusted", "deco_pipe_framed_red",
                "deco_pipe_framed_rusted", "deco_pipe_marked", "deco_pipe_quad", "deco_pipe_quad_marked", "deco_pipe_quad_red",
                "deco_pipe_quad_rusted", "deco_pipe_red", "deco_pipe_rim", "deco_pipe_rim_green", "deco_pipe_rim_marked",
                "deco_pipe_rim_rusted", "deco_pipe_rusted", "deco_pipe_green", "deco_pipe_green_rusted",
                "deco_pipe_framed_green", "deco_pipe_framed_marked", "deco_pipe_quad_green", "deco_pipe_quad_green_rusted",
                "deco_pipe_rim_red", "deco_pipe_rim_green_rusted")) rule(s, Rot.PILLAR, AXIS);
        rule("concrete_pillar", Rot.PILLAR, AXIS);
        rule("meteor_pillar", Rot.PILLAR, AXIS);
        rule("bobblehead", Rot.BOBBLE, (base, meta) -> with(base, "rotation", meta));
        // BlockChain: 0 hangs (from above), 2..5 hangs on the wall of that ForgeDirection; "end" is a shape update
        rule("dungeon_chain", Rot.CHAIN, (base, meta) -> meta >= 2 && meta <= 5
                ? with(with(base, "wall", true), "facing", forge(meta)) : with(base, "wall", false));
        // Floodlight: meta % 6 = the clicked side, +6 = flipped
        rule("floodlight", Rot.FLOODLIGHT, (base, meta) -> with(with(base, "facing", forge(meta % 6)), "flip", meta >= 6));
        // Spotlight: side << 1 | broken
        Mapper spot = (base, meta) -> {
            int dir = meta >> 1;
            BlockState s = dir <= 5 ? with(base, "facing", forge(dir)) : base;
            return with(s, "broken", (meta & 1) != 0);
        };
        for (String s : List.of("spotlight_incandescent", "spotlight_incandescent_off", "spotlight_fluoro",
                "spotlight_fluoro_off", "spotlight_halogen", "spotlight_halogen_off")) rule(s, Rot.SPOTLIGHT, spot);
        // BlockScaffold: variant in bits 0..1, meta >> 2: 0 horizontal N/S, 1 vertical N/S, 2 horizontal E/W, 3 vertical E/W
        String[] orient = {"horizontal_north_south", "vertical_north_south", "horizontal_east_west", "vertical_east_west"};
        rule("steel_scaffold", Rot.NONE, (base, meta) -> with(ordinal(base, "variant", meta & 3), "orient", orient[(meta >> 2) & 3]));
        // BlockGrate: meta = height step
        rule("steel_grate", Rot.NONE, (base, meta) -> with(base, "height", meta));
        rule("steel_grate_wide", Rot.NONE, (base, meta) -> with(base, "height", meta));
        // subtype-split 1.7 blocks
        String[] crts = {"clean", "broken", "blinking", "bsod"};
        rule("deco_crt", Rot.DECO_MODEL, meta -> "deco_crt_" + crts[(Math.abs(meta) % 16) / 4], LOOK_QUADRANT);
        String[] toasters = {"iron", "steel", "wood"};
        rule("deco_toaster", Rot.DECO_MODEL, meta -> "deco_toaster_" + toasters[(Math.abs(meta) % 12) / 4], LOOK_QUADRANT);
        // BlockDecoModel: rot in bits 2..3 labelled N/S/W/E for look directions S/N/E/W; the port stores the look
        Direction[] computerLook = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};
        rule("deco_computer", Rot.DECO_MODEL_SHIFTED, meta -> (meta & 3) == 0 ? "deco_computer_ibm_300pl" : null,
                (base, meta) -> with(base, "facing", computerLook[(meta >> 2) & 3]));
        // BlockMetalFence: meta 1 is the post variant; FluidDuctStandard: 0 plain, 1 silver, 2 colored
        rule("fence_metal", Rot.NONE, meta -> (meta & 1) == 1 ? "fence_metal_post" : "fence_metal", (base, meta) -> base);
        rule("fluid_duct_neo", Rot.NONE, variants("fluid_duct_neo", "", "_silver", "_colored"), (base, meta) -> base);
        rule("plant_dead", Rot.NONE, variants("plant_dead_", "generic", "grass", "flower", "bigflower", "fern"), (base, meta) -> base);
        rule("wood_structure", Rot.NONE, variants("wood_structure_", "roof", "scaffold", "ceiling"), (base, meta) -> base);
        rule("platemetal", Rot.NONE, variants("platemetal_", ModDecoContent.PLATEMETAL_COLORS), (base, meta) -> base);
        for (String spike : new String[]{"stalagmite", "stalactite"}) {
            rule(spike, Rot.NONE, variants(spike + "_", "sulfur", "asbestos", "ice", "snow", "glyphid1", "glyphid2", "glyphid3"), (base, meta) -> base);
        }
        rule("crashed_bomb", Rot.NONE, variants("crashed_bomb_", "balefire", "conventional", "nuke", "salted"), (base, meta) -> base);
        // BlockMultiSlab: variant in bits 0..2, top half in bit 3; the doubles use the same variants
        String[] concrete = {"concrete_smooth", "concrete", "concrete_asbestos", "ducrete_smooth", "ducrete", "asphalt"};
        String[] concreteBrick = {"brick_concrete", "brick_concrete_mossy", "brick_concrete_cracked", "brick_concrete_broken", "brick_ducrete"};
        String[] brick = {"reinforced_stone", "reinforced_brick", "brick_obsidian", "brick_light", "brick_compound", "brick_asbestos", "brick_fire"};
        slab("concrete_slab", false, concrete);
        slab("concrete_double_slab", true, concrete);
        slab("concrete_brick_slab", false, concreteBrick);
        slab("concrete_brick_double_slab", true, concreteBrick);
        slab("brick_slab", false, brick);
        slab("brick_double_slab", true, brick);
    }

    /** Names whose 1.7 block did not resolve; each is logged once. */
    private static final Set<String> WARNED = new HashSet<>();
    private static final Map<Long, Resolved> CACHE = new HashMap<>();
    private static final Map<String, Integer> NAME_KEYS = new HashMap<>();

    // entry points

    /**
     * 1.7's per-block {@code transformMeta}/{@code transformBlock} as {@link NBTStructure#build} applied them: the
     * metadata rotation for {@code coordBaseMode}, and spotlights switched to their off variant with the broken bit
     * set (1.7 {@code Spotlight.disableOnGeneration}, true), which happens at every build, rotated or not.
     */
    public static Legacy transform(String name, int meta, int coordBaseMode) {
        Rot rot = rotationOf(name);
        if (rot == Rot.SPOTLIGHT) {
            if (SPOTLIGHTS_WORKING.get()) {
                return new Legacy(name, INBTBlockTransformable.transformMetaDeco(meta >> 1, coordBaseMode) << 1);
            }
            String n = name;
            if (n.equals("hbm:tile.spotlight_incandescent")) n = "hbm:tile.spotlight_incandescent_off";
            if (n.equals("hbm:tile.spotlight_fluoro")) n = "hbm:tile.spotlight_fluoro_off";
            if (n.equals("hbm:tile.spotlight_halogen")) n = "hbm:tile.spotlight_halogen_off";
            return new Legacy(n, (INBTBlockTransformable.transformMetaDeco(meta >> 1, coordBaseMode) << 1) + 1);
        }
        return new Legacy(name, switch (rot) {
            case BOBBLE -> (meta + coordBaseMode * 4) % 16;
            case DUMMYABLE -> INBTBlockTransformable.transformMetaDummyable(meta, coordBaseMode);
            case DECO_MODEL -> INBTBlockTransformable.transformMetaDecoModel(meta, coordBaseMode);
            case DECO_MODEL_SHIFTED -> INBTBlockTransformable.transformMetaDecoModelShifted(meta, coordBaseMode);
            case CHAIN -> coordBaseMode == 0 || meta == 0 || meta == 1 ? meta : INBTBlockTransformable.transformMetaDeco(meta, coordBaseMode);
            case FLOODLIGHT -> INBTBlockTransformable.transformMetaFloodlight(meta, coordBaseMode);
            case DECO -> INBTBlockTransformable.transformMetaDeco(meta, coordBaseMode);
            case DOOR -> INBTBlockTransformable.transformMetaDoor(meta, coordBaseMode);
            case PILLAR -> INBTBlockTransformable.transformMetaPillar(meta, coordBaseMode);
            case STAIRS -> INBTBlockTransformable.transformMetaStairs(meta, coordBaseMode);
            case TRAPDOOR -> INBTBlockTransformable.transformMetaTrapdoor(meta, coordBaseMode);
            case DIRECTIONAL -> INBTBlockTransformable.transformMetaDirectional(meta, coordBaseMode);
            case TORCH -> INBTBlockTransformable.transformMetaTorch(meta, coordBaseMode);
            case LEVER -> INBTBlockTransformable.transformMetaLever(meta, coordBaseMode);
            // 1.7 applied the vanilla vine rule only for coordBaseMode != 0 (the early return precedes it)
            case VINE -> coordBaseMode == 0 ? meta : INBTBlockTransformable.transformMetaVine(meta, coordBaseMode);
            default -> meta;
        });
    }

    private static Rot rotationOf(String name) {
        Rot vanilla = VANILLA_ROT.get(name);
        if (vanilla != null) return vanilla;
        String hbm = hbmName(name);
        if (hbm == null) {
            // an addon's block: no 1.7 rule names it, so only the class-based rotations apply
            Block block = foreignBlock(name);
            if (block instanceof BlockMultiblockCore) return Rot.DUMMYABLE;
            if (block instanceof StairBlock) return Rot.STAIRS;
            return Rot.NONE;
        }
        Rule rule = RULES.get(hbm);
        if (rule != null) return rule.rot();
        if (hbm.startsWith("wand_jigsaw") || hbm.startsWith("wand_tandem")) return Rot.DECO;
        Block block = hbmBlock(hbm);
        if (block instanceof BlockMultiblockCore) return Rot.DUMMYABLE;
        if (block instanceof StairBlock) return Rot.STAIRS;
        return Rot.NONE;
    }

    /** The flattened state for a 1.7 (name, metadata) pair, after {@link #transform}. Never null. */
    public static Resolved resolve(String name, int meta) {
        int nameKey = NAME_KEYS.computeIfAbsent(name, n -> NAME_KEYS.size());
        long key = ((long) nameKey << 8) | (meta & 0xFF);
        Resolved cached = CACHE.get(key);
        if (cached != null) return cached;
        Resolved resolved = resolveUncached(name, meta);
        CACHE.put(key, resolved);
        return resolved;
    }

    private static Resolved resolveUncached(String name, int meta) {
        meta &= 15;
        if (name.startsWith("minecraft:")) {
            Integer id = VANILLA_IDS.get(name);
            if (id == null) return unknown(name);
            BlockState state = LegacyBlockStates.upgrade(id, meta);
            if (id == 64 || id == 71) return new Resolved(state.getBlock().defaultBlockState(), Kind.DOOR);
            return Resolved.of(state);
        }

        String hbm = hbmName(name);
        if (hbm == null) {
            // 1.7 registry names of other mods: the 1.7 port would load them only with that mod installed. An addon
            // that ships palettes (NTM: Space's planet structures) names its own blocks as "ns:path" (a "tile." prefix
            // is dropped like HBM's); they get the generic rules, there is no 1.7 table for them.
            Block foreign = foreignBlock(name);
            return foreign == null ? unknown(name) : fromBlock(foreign, meta);
        }

        // 1.7 NBTStructure.substitutions ("BOB PATCH"): ore_coal_oil became plain coal ore, metadata kept
        if (hbm.equals("ore_coal_oil")) return Resolved.of(Blocks.COAL_ORE.defaultBlockState());
        // port: HBM copper ore and storage block fold onto vanilla copper
        if (hbm.equals("ore_copper")) return Resolved.of(Blocks.COPPER_ORE.defaultBlockState());
        if (hbm.equals("block_copper")) return Resolved.of(Blocks.COPPER_BLOCK.defaultBlockState());

        Rule rule = RULES.get(hbm);
        String id = rule != null && rule.id() != null ? rule.id().apply(meta) : hbm;
        Block block = id == null ? null : hbmBlock(id);
        if (block == null) return unknown(name + (id != null && !id.equals(hbm) ? " -> hbm:" + id : ""));

        BlockState base = block.defaultBlockState();
        if (rule != null && rule.door()) return new Resolved(base, Kind.DOOR);
        if (rule != null) return Resolved.of(rule.mapper().map(base, meta));
        return fromBlock(block, meta);
    }

    /** The rule-less mapping: multiblock core/cell, stairs, otherwise the generic per-property rule. */
    private static Resolved fromBlock(Block block, int meta) {
        BlockState base = block.defaultBlockState();
        if (block instanceof RBMKBase) {
            if (meta < 12) return new Resolved(Blocks.AIR.defaultBlockState(), Kind.CELL);
            var lid = meta == 13 ? RBMKBase.Lid.GLASS : meta == 15 ? RBMKBase.Lid.CONCRETE : RBMKBase.Lid.NONE;
            return new Resolved(base.setValue(BlockMultiblockCore.FACING, RBMKBase.DIR_NO_LID)
                    .setValue(RBMKBase.LID, lid), Kind.CORE);
        }
        if (block instanceof BlockMultiblockCore) {
            // 1.7 BlockDummyable: the core carries ForgeDirection + 10; every other meta is a cell the core fills
            if (meta >= 12) return new Resolved(with(base, "facing", forge(meta - 10)), Kind.CORE);
            return new Resolved(Blocks.AIR.defaultBlockState(), Kind.CELL);
        }
        if (block instanceof StairBlock) return Resolved.of(copyShared(LegacyBlockStates.upgrade(53, meta), base));
        return Resolved.of(generic(base, meta));
    }

    private static Resolved unknown(String name) {
        if (WARNED.add(name)) Constants.LOG.warn("[NBTStructure] no port block for 1.7 structure block {}, placing air", name);
        return new Resolved(Blocks.AIR.defaultBlockState(), Kind.UNKNOWN);
    }

    /**
     * A two-block door from its own metadata and the other half's (1.7 kept facing/open in the lower half and
     * hinge/powered in the upper). {@code otherMeta} is -1 when the other half is missing.
     */
    public static BlockState door(BlockState base, int ownMeta, int otherMeta) {
        boolean upper = (ownMeta & 8) != 0;
        int lower = upper ? (otherMeta >= 0 && (otherMeta & 8) == 0 ? otherMeta : 0) : ownMeta;
        int top = upper ? ownMeta : (otherMeta >= 0 && (otherMeta & 8) != 0 ? otherMeta : 8);
        Direction[] facing = {Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
        BlockState s = with(base, "facing", facing[lower & 3]);
        s = with(s, "open", (lower & 4) != 0);
        s = with(s, "hinge", (top & 1) != 0 ? "right" : "left");
        s = with(s, "powered", (top & 2) != 0);
        return with(s, "half", upper ? "upper" : "lower");
    }

    /** A single 1.7 (name, metadata) reference, e.g. a placeholder's replacement block, as one state. */
    public static BlockState legacyState(String name, int meta) {
        Resolved r = resolve(name, meta);
        return r.kind() == Kind.DOOR ? door(r.state(), meta, -1) : r.state();
    }

    /** Whether a palette name is the 1.7 (or ported) jigsaw placeholder. */
    public static boolean isJigsaw(String name) {
        return name.equals("hbm:tile.wand_jigsaw") || name.equals("hbm:wand_jigsaw");
    }

    // modern (port-saved) palette entries

    /** A state as a vanilla structure palette entry: {@code {Name, Properties{...}}}. */
    public static CompoundTag writeState(BlockState state) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Name", String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        CompoundTag props = new CompoundTag();
        for (Property<?> property : state.getProperties()) props.putString(property.getName(), valueName(state, property));
        if (!props.isEmpty()) tag.put("Properties", props);
        return tag;
    }

    /** The inverse of {@link #writeState}; an unknown block is air (logged once), an unknown property is ignored. */
    public static BlockState readState(CompoundTag tag) {
        String name = Tags.getStringOr(tag, "Name", "minecraft:air");
        var id = Ids.parse(name);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            unknown(name);
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state = block.defaultBlockState();
        if (tag.get("Properties") instanceof CompoundTag props) {
            for (String key : Tags.keys(props)) state = with(state, key, Tags.getStringOr(props, key, ""));
        }
        return state;
    }

    /** {@code namespace:id[prop=value,...]}, the block-state text form, for block-entity fields. */
    public static String stateString(BlockState state) {
        StringBuilder text = new StringBuilder(String.valueOf(BuiltInRegistries.BLOCK.getKey(state.getBlock())));
        if (!state.getProperties().isEmpty()) {
            text.append('[');
            boolean first = true;
            for (Property<?> property : state.getProperties()) {
                if (!first) text.append(',');
                first = false;
                text.append(property.getName()).append('=').append(valueName(state, property));
            }
            text.append(']');
        }
        return text.toString();
    }

    /** The inverse of {@link #stateString}; unknown blocks are air, unknown properties are ignored. */
    public static BlockState parseState(String text) {
        int bracket = text.indexOf('[');
        String name = bracket < 0 ? text : text.substring(0, bracket);
        var id = name.isEmpty() ? null : Ids.parse(name);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) return Blocks.AIR.defaultBlockState();
        BlockState state = block.defaultBlockState();
        if (bracket >= 0 && text.endsWith("]")) {
            for (String pair : text.substring(bracket + 1, text.length() - 1).split(",")) {
                int eq = pair.indexOf('=');
                if (eq > 0) state = with(state, pair.substring(0, eq), pair.substring(eq + 1));
            }
        }
        return state;
    }

    // property helpers

    private static @org.jspecify.annotations.Nullable String hbmName(String name) {
        if (name.startsWith("hbm:tile.")) return name.substring("hbm:tile.".length());
        if (name.startsWith("hbm:")) return name.substring("hbm:".length());
        return null;
    }

    /** A registered block of a namespace other than {@code minecraft} and {@code hbm}, or null. */
    private static @org.jspecify.annotations.Nullable Block foreignBlock(String name) {
        int colon = name.indexOf(':');
        if (colon <= 0) return null;
        String namespace = name.substring(0, colon);
        String path = name.substring(colon + 1);
        if (path.startsWith("tile.")) path = path.substring("tile.".length());
        if (namespace.equals("minecraft") || namespace.equals("hbm") || !validId(namespace) || !validId(path)) return null;
        return BuiltInRegistries.BLOCK.getOptional(Ids.of(namespace, path)).orElse(null);
    }

    private static boolean validId(String part) {
        return part.chars().allMatch(ch -> ch == '_' || ch == '-' || ch == '.' || ch == '/' || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9'));
    }

    private static @org.jspecify.annotations.Nullable Block hbmBlock(String id) {
        // 1.7 .nbt palettes can name blocks that were never registered ("hbm:#undef" in the meteor dungeon pieces);
        // 1.7's Block.getBlockFromName gave null (air) for those, an invalid id must not crash world generation (WG-C)
        if (!id.chars().allMatch(ch -> ch == '_' || ch == '-' || ch == '.' || ch == '/' || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9'))) return null;
        return BuiltInRegistries.BLOCK.getOptional(Ids.of("hbm", id)).orElse(null);
    }

    private static BlockState generic(BlockState base, int meta) {
        BlockState s = base;
        for (Property<?> property : base.getProperties()) {
            switch (property.getName()) {
                case "facing" -> s = FACING.map(s, meta);
                case "axis" -> s = AXIS.map(s, meta);
                case "rotation", "height", "age", "damage", "shade" -> s = with(s, property.getName(), meta);
                case "layers" -> s = with(s, "layers", meta + 1);
                case "color" -> s = with(s, "color", DyeColor.byId(meta).getSerializedName());
                case "type", "variant" -> s = ordinal(s, property.getName(), meta);
                default -> { }
            }
        }
        return s;
    }

    /** Sets {@code name} to {@code value} (a String, number, boolean or enum) when the state has it and accepts it. */
    public static BlockState with(BlockState state, String name, Object value) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) return state;
        String text = value instanceof StringRepresentable rep ? rep.getSerializedName()
                : String.valueOf(value).toLowerCase(Locale.ROOT);
        return set(state, property, text);
    }

    private static <T extends Comparable<T>> BlockState set(BlockState state, Property<T> property, String text) {
        return property.getValue(text).map(v -> state.setValue(property, v)).orElse(state);
    }

    /** Sets an enum-like property to its {@code ordinal}-th possible value (declaration order). */
    private static BlockState ordinal(BlockState state, String name, int ordinal) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) return state;
        return nth(state, property, ordinal);
    }

    private static <T extends Comparable<T>> BlockState nth(BlockState state, Property<T> property, int ordinal) {
        List<T> values = List.copyOf(property.getPossibleValues());
        if (ordinal < 0 || ordinal >= values.size()) return state;
        return state.setValue(property, values.get(ordinal));
    }

    /** Copies every property {@code from} shares by name with {@code to}. */
    private static BlockState copyShared(BlockState from, BlockState to) {
        BlockState s = to;
        for (Property<?> property : from.getProperties()) s = with(s, property.getName(), valueName(from, property));
        return s;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }
}
