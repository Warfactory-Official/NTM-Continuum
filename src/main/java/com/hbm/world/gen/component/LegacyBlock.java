package com.hbm.world.gen.component;

import com.hbm.world.gen.nbt.LegacyStructurePalette;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * A 1.7 {@code Block} reference as the hardcoded structure builders ({@link Component} and its subclasses) used it: a
 * 1.7 registry name ({@code hbm:tile.<name>} or {@code minecraft:<name>}) that is combined with a 1.7 metadata value
 * at every placement. The builders compute that metadata themselves ({@code getDecoMeta}, {@code getStairMeta}, ...),
 * exactly as 1.7 did, and the pair becomes a flattened state through {@link LegacyStructurePalette} - the same table
 * the 1.7 NBT structures go through - plus the few blocks whose 1.7 metadata layout only the component builders use
 * ({@link #resolveOverride}).
 *
 * <p>Resolution needs the block registry, so it happens on first use and is cached per metadata value (a benign race:
 * every thread computes the same state).
 */
public final class LegacyBlock {

    public final String name;
    private final @Nullable BlockState[] cache = new BlockState[16];

    private LegacyBlock(String name) {
        this.name = name;
    }

    /** {@code ModBlocks.<field>}: the 1.7 block registered as {@code hbm:tile.<blockName>}. */
    public static LegacyBlock hbm(String blockName) {
        return new LegacyBlock("hbm:tile." + blockName);
    }

    /** {@code Blocks.<field>}: a 1.7.10 vanilla block by its registry name. */
    public static LegacyBlock vanilla(String name) {
        return new LegacyBlock("minecraft:" + name);
    }

    /** The flattened state for this block with 1.7 metadata {@code meta} (only the low four bits exist). */
    public BlockState state(int meta) {
        int m = meta & 15;
        BlockState state = cache[m];
        if (state == null) {
            state = resolveOverride(name, m);
            if (state == null) {
                state = LegacyStructurePalette.legacyState(name, m);
                if (name.startsWith("hbm:")) state = sixWay(state, m);
            }
            cache[m] = state;
        }
        return state;
    }

    public Block block() {
        return state(0).getBlock();
    }

    public boolean isAir() {
        return name.equals("minecraft:air");
    }

    /** Whether a placed state is this 1.7 block (by flattened block; metadata-split 1.7 blocks compare by variant). */
    public boolean is(BlockState placed, int meta) {
        return placed.getBlock() == state(meta).getBlock();
    }

    /** 1.7 {@code instanceof BlockStairs}. */
    public boolean isStairs() {
        return block() instanceof net.minecraft.world.level.block.StairBlock;
    }

    @Override
    public String toString() {
        return name;
    }

    // -----------------------------------------------------------------------------------------------------------------

    private static final String[] DUD = {"balefire", "conventional", "nuke", "salted"};
    private static final String[] FLOWER = {"foxglove", "tobacco", "nightshade", "weed", "cd0", "cd1"};
    private static final String[] TILE = {"large", "small"};
    /** 1.7 {@code BlockDecoContainer}/{@code BlockDecoModel} rotation bits 2..3 labelled N, S, W, E. */
    private static final Direction[] DECO_MODEL = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    /**
     * 1.7 blocks whose metadata the NBT palette does not map (none of the bundled .nbt files carries them) but the
     * component builders place: the subtype-split blocks, and the cabinet's decoration-model layout.
     */
    private static @Nullable BlockState resolveOverride(String name, int meta) {
        return switch (name) {
            // BlockCrashedBomb is a BlockEnumMulti: EnumUtil.grabEnumSafely(meta), so NTMLab2's rotated meta picks the dud
            case "hbm:tile.crashed_bomb" -> hbmState("crashed_bomb_" + DUD[Math.abs(meta % DUD.length)]);
            case "hbm:tile.plant_flower" -> hbmState("plant_flower_" + FLOWER[Math.abs(meta % FLOWER.length)]);
            case "hbm:tile.vinyl_tile" -> hbmState("vinyl_tile_" + TILE[Math.abs(meta % TILE.length)]);
            // 1.7 capacitor_copper is this port's machine_capacitor_copper; metadata = ForgeDirection
            case "hbm:tile.capacitor_copper" -> meta <= 5
                    ? LegacyStructurePalette.with(hbmState("machine_capacitor_copper"), "facing", Direction.from3DDataValue(meta))
                    : hbmState("machine_capacitor_copper");
            // "0 North, 1 South, 2 West, 3 East - the side the drawers face" in bits 2..3, finish in bits 0..1
            case "hbm:tile.filing_cabinet" -> {
                BlockState s = hbmState("filing_cabinet");
                s = LegacyStructurePalette.with(s, "facing", DECO_MODEL[(meta >> 2) & 3]);
                yield ordinal(s, "type", meta & 3);
            }
            // NTMAnvil faces away from the ForgeDirection in its metadata (as the palette's anvil_lead rule)
            case "hbm:tile.anvil_iron" -> meta >= 2 && meta <= 5
                    ? LegacyStructurePalette.with(hbmState("anvil_iron"), "facing", Direction.from3DDataValue(meta).getOpposite())
                    : hbmState("anvil_iron");
            // 1.7.10 kept a pot's plant in its tile entity (the metadata was the plant item's damage); the builders set none
            case "minecraft:flower_pot" -> net.minecraft.world.level.block.Blocks.FLOWER_POT.defaultBlockState();
            default -> null;
        };
    }

    /**
     * The palette's generic rule sets a {@code facing} from metadata 2..5 only; a six-way 1.7 block (fans, diodes,
     * radio torches, gauges) stores ForgeDirection 0..5, so down and up are taken from the metadata too.
     */
    private static BlockState sixWay(BlockState state, int meta) {
        if (meta > 1) return state;
        Property<?> facing = state.getBlock().getStateDefinition().getProperty("facing");
        if (facing == null || !facing.getPossibleValues().contains(Direction.UP)) return state;
        return LegacyStructurePalette.with(state, "facing", Direction.from3DDataValue(meta));
    }

    /** A bare path is HBM's block; {@code "ns:path"} names an addon's (or vanilla's) block. */
    private static BlockState hbmState(String id) {
        return BuiltInRegistries.BLOCK.getOptional(com.hbm.lib.Library.parseId(id)).map(Block::defaultBlockState).orElse(Blocks.AIR.defaultBlockState());
    }

    private static BlockState ordinal(BlockState state, String name, int ordinal) {
        Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
        if (property == null) return state;
        return nth(state, property, ordinal);
    }

    private static <T extends Comparable<T>> BlockState nth(BlockState state, Property<T> property, int ordinal) {
        java.util.List<T> values = java.util.List.copyOf(property.getPossibleValues());
        return ordinal < 0 || ordinal >= values.size() ? state : state.setValue(property, values.get(ordinal));
    }
}
