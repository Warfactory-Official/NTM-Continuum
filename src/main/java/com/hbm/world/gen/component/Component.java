package com.hbm.world.gen.component;

import com.hbm.blocks.generic.BlockBobble;
import com.hbm.blocks.generic.Waterlogging;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.config.StructureConfig;
import com.hbm.itempool.ItemPool;
import com.hbm.items.special.ItemBookLore;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.deco.TileEntityBobble;
import com.hbm.tileentity.machine.TileEntityLockableBase;
import com.hbm.util.LootGenerator;
import com.hbm.world.gen.component.LegacyBlocks.Mod;
import com.hbm.world.gen.component.LegacyBlocks.Vanilla;
import com.hbm.world.gen.nbt.LegacyStructurePalette;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 {@code com.hbm.world.gen.component.Component}: the base of NTM's hardcoded structure pieces (the features
 * houses/labs/offices, the missile silo, the bunker rooms), together with the parts of 1.7's vanilla
 * {@code StructureComponent} those builders call ({@code placeBlockAtCurrentPosition}, {@code getMetadataWithOffset},
 * {@code generateStructureChestContents}, the {@code BlockSelector}, {@code ItemDoor.placeDoorBlock}).
 *
 * <p><b>1.7 coordinates, not vanilla's.</b> A piece keeps 1.7's {@code coordBaseMode} and 1.7's "fixed" offset
 * methods ({@link #getXWithOffset}: mode 2 counts x from {@code maxX}, where vanilla mirrors), and the builders compute
 * their own 1.7 metadata; {@link LegacyBlock} turns (block, metadata) into a flattened state at the last moment. The
 * modern {@code StructurePiece} orientation/mirror is not used at all.
 *
 * <p><b>Chunk-by-chunk building.</b> Vanilla calls {@link #postProcess} once per chunk with that chunk's box, like
 * 1.7's {@code addComponentParts}; every write is clipped to the box (1.7's box checks, plus the few 1.7 helpers that
 * wrote without one), so each chunk builds exactly its own share - a far-chunk write is an error in modern world
 * generation. The builder runs with a per-call context (the box and the placed positions for the final neighbour-shape
 * pass), never piece state, because neighbouring chunks may be built on different threads.
 *
 * <p><b>Height.</b> 1.7 averaged the terrain under a piece whenever a chunk built it ({@link #setAverageHeight}). The
 * port does it once, when the start is assembled, from the generator's own terrain ({@link #assembleHeight}); see the
 * method for how that matches 1.7.
 */
public abstract class Component extends StructurePiece {

    /** Average height (Presumably stands for height position) */
    protected int hpos = -1;

    /** 1.7 {@code StructureComponent.coordBaseMode}: 0 south, 1 west, 2 north, 3 east. */
    protected int coordBaseMode;

    protected Component(StructurePieceType type, int componentType) {
        super(type, componentType, new BoundingBox(0, 0, 0, 0, 0, 0));
    }

    protected Component(StructurePieceType type, RandomSource rand, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        super(type, 0, new BoundingBox(minX, minY, minZ, minX, minY, minZ));
        this.coordBaseMode = rand.nextInt(4);

        switch (this.coordBaseMode) {
            case 1, 3 -> this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxZ, minY + maxY, minZ + maxX);
            default -> this.boundingBox = new BoundingBox(minX, minY, minZ, minX + maxX, minY + maxY, minZ + maxZ);
        }
    }

    /** Load; subclasses call {@link #readNBT} from their own constructor, after their field initialisers ran. */
    protected Component(StructurePieceType type, CompoundTag tag) {
        super(type, tag);
        this.coordBaseMode = Tags.getIntOr(tag, "CBM", 0);
    }

    @Override
    protected final void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CBM", coordBaseMode);
        writeNBT(tag);
    }

    /** 1.7 {@code func_143012_a}: Set to NBT */
    protected void writeNBT(CompoundTag nbt) {
        nbt.putInt("HPos", this.hpos);
    }

    /** 1.7 {@code func_143011_b}: Get from NBT */
    protected void readNBT(CompoundTag nbt) {
        this.hpos = Tags.getIntOr(nbt, "HPos", -1);
    }

    public int getCoordMode() {
        return this.coordBaseMode;
    }

    /** 1.7 {@code StructureComponent.getComponentType}. */
    public int getComponentType() {
        return genDepth;
    }

    // height, once per start

    /**
     * Decides the piece's height when its start is assembled. The default is 1.7's {@link #setAverageHeight}: the mean
     * of {@code max(getTopSolidOrLiquidBlock, averageGroundLevel)} over the columns of the piece inside the populating
     * box, the box then moved so that its floor sits there. The features pieces are anchored at the start chunk's
     * {@code +8} corner, where 1.7's populate box starts, and none is wider than 16, so 1.7 averaged over the whole
     * footprint from one populating chunk; the port does the same from the generator's terrain
     * ({@code OCEAN_FLOOR_WG}: the highest movement-blocking block + 1, liquids and leaves ignored, which is what
     * {@code getTopSolidOrLiquidBlock} returned). 1.7's {@code averageGroundLevel} is 64 in a normal world, 4 in a
     * superflat one: the generator's sea level + 1.
     */
    public void assembleHeight(Structure.GenerationContext context) {
        int ground = groundLevel(context);
        int total = 0;
        int iterations = 0;

        for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
            for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
                total += Math.max(topSolid(context, x, z), ground);
                iterations++;
            }
        }

        if (iterations == 0) return;

        this.hpos = total / iterations; //finds mean of every block in bounding box
        this.boundingBox.move(0, this.hpos - this.boundingBox.minY(), 0);
    }

    /** Tests and tools: puts the piece's box floor at {@code y} and marks the height decided (no terrain rule; -1 is 1.7's unset). */
    public void fixHeight(int y) {
        this.boundingBox.move(0, y - this.boundingBox.minY(), 0);
        this.hpos = y;
    }

    /** 1.7 {@code getTopSolidOrLiquidBlock} from the generator (before any feature). */
    protected static int topSolid(Structure.GenerationContext context, int x, int z) {
        return context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, context.heightAccessor(), context.randomState());
    }

    /** 1.7 {@code WorldProvider.getAverageGroundLevel}: 64 (sea level 63 + 1); a superflat world's surface. */
    protected static int groundLevel(Structure.GenerationContext context) {
        return context.chunkGenerator().getSeaLevel() + 1;
    }

    /** 1.7 {@code getAverageHeight} over {@code area} (all of it) from the generator; -1 for an empty area. */
    protected static int getAverageHeight(Structure.GenerationContext context, BoundingBox area) {
        int ground = groundLevel(context);
        int total = 0;
        int iterations = 0;

        for (int z = area.minZ(); z <= area.maxZ(); z++) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                total += Math.max(topSolid(context, x, z), ground);
                iterations++;
            }
        }

        if (iterations == 0) return -1;

        return total / iterations;
    }

    /**
     * 1.7 re-averaged here on every populating chunk; the height is decided at assembly now ({@link #assembleHeight}),
     * so this only computes one for a piece that has none (a piece built directly, as the GameTests do).
     */
    protected boolean setAverageHeight(WorldGenLevel world, BoundingBox box, int y) {
        if (this.hpos != -1) return true;

        int total = 0;
        int iterations = 0;
        int ground = world.getSeaLevel() + 1;

        for (int z = this.boundingBox.minZ(); z <= this.boundingBox.maxZ(); z++) {
            for (int x = this.boundingBox.minX(); x <= this.boundingBox.maxX(); x++) {
                if (box.isInside(x, y, z)) {
                    total += Math.max(topSolid(world, x, z), ground);
                    iterations++;
                }
            }
        }

        if (iterations == 0)
            return false;

        this.hpos = total / iterations; //finds mean of every block in bounding box
        this.boundingBox.move(0, this.hpos - this.boundingBox.minY(), 0);
        return true;
    }

    /** 1.7 {@code Component.getAverageHeight} in the world being built; -1 when no column of {@code area} is in {@code box}. */
    protected static int getAverageHeight(WorldGenLevel world, BoundingBox area, BoundingBox box, int y) {
        int total = 0;
        int iterations = 0;
        int ground = world.getSeaLevel() + 1;

        for (int z = area.minZ(); z <= area.maxZ(); z++) {
            for (int x = area.minX(); x <= area.maxX(); x++) {
                if (box.isInside(x, y, z)) {
                    total += Math.max(topSolid(world, x, z), ground);
                    iterations++;
                }
            }
        }

        if (iterations == 0)
            return -1;

        return total / iterations;
    }

    /**
     * 1.7 {@code World.getTopSolidOrLiquidBlock}: above the highest block whose material blocks movement and is not
     * leaves (water does not block movement, so liquids are skipped too). The {@code OCEAN_FLOOR} heightmap is the
     * movement-blocking part, but modern leaves block movement, so a canopy that reached the chunk first (a
     * neighbour's tree) is stepped through here as 1.7 did. Returns -1 like 1.7 when the column is empty.
     */
    public static int topSolid(LevelAccessor world, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int min = world.getMinY();
        for (int y = world.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) - 1; y >= min; y--) {
            BlockState state = world.getBlockState(pos.set(x, y, z));
            if (state.blocksMotion() && !state.is(BlockTags.LEAVES)) return y + 1;
        }
        return -1;
    }

    // building

    /** 1.7 {@code addComponentParts}: builds the part of the piece inside {@code box}. */
    public abstract boolean addComponentParts(WorldGenLevel world, RandomSource rand, BoundingBox box);

    /** The box and placed positions of the build running on this thread. */
    private static final ThreadLocal<Build> BUILD = new ThreadLocal<>();

    private static final class Build {
        final BoundingBox box;
        final List<BlockPos> placed = new ArrayList<>();

        Build(BoundingBox box) {
            this.box = box;
        }
    }

    @Override
    public final void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                                  BoundingBox chunkBB, ChunkPos chunkPos, BlockPos pivot) {
        build(level, random, chunkBB);
    }

    /** Builds the part inside {@code box}, then lets the placed blocks take their neighbour shapes. */
    public final void build(WorldGenLevel level, RandomSource random, BoundingBox box) {
        Build previous = BUILD.get();
        Build build = new Build(box);
        BUILD.set(build);
        try {
            addComponentParts(level, random, box);
            // 1.7 computed fence/pane/stair connections at render time; a flattened world stores them in the state
            for (BlockPos pos : build.placed) {
                BlockState state = level.getBlockState(pos);
                BlockState shaped = Block.updateFromNeighbourShapes(state, level, pos);
                if (shaped != state) level.setBlock(pos, shaped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        } finally {
            if (previous == null) BUILD.remove(); else BUILD.set(previous);
        }
    }

    /** Whether a world position may be written by the build running on this thread (its chunk box). */
    protected static boolean writable(int x, int y, int z) {
        Build build = BUILD.get();
        return build == null || build.box.isInside(x, y, z);
    }

    /** 1.7 {@code world.setBlock(x, y, z, block, meta, 2)}, clipped to the chunk being built. */
    protected static void setBlock(WorldGenLevel world, int x, int y, int z, LegacyBlock block, int meta) {
        setState(world, x, y, z, block.state(meta));
    }

    protected static void setState(WorldGenLevel world, int x, int y, int z, BlockState state) {
        if (!writable(x, y, z)) return;
        BlockPos pos = new BlockPos(x, y, z);
        if (world.isOutsideBuildHeight(pos)) return;
        world.setBlock(pos, Waterlogging.inWorld(state, world, pos), Block.UPDATE_CLIENTS);
        Build build = BUILD.get();
        if (build != null) build.placed.add(pos);
    }

    /** 1.7 {@code world.getBlock(x, y, z)}. */
    protected static BlockState getState(WorldGenLevel world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z));
    }

    protected static @Nullable BlockEntity getTileEntity(WorldGenLevel world, int x, int y, int z) {
        if (!writable(x, y, z)) return null;
        return world.getBlockEntity(new BlockPos(x, y, z));
    }

    /** 1.7 {@code Block.getMaterial().isReplaceable()}. */
    protected static boolean isReplaceable(BlockState state) {
        return state.canBeReplaced();
    }

    // 1.7 StructureComponent

    /** getXWithOffset & getZWithOffset Methods that are actually fixed **/
    //Turns out, this entire time every single minecraft structure is mirrored instead of rotated when facing East and North
    //Also turns out, it's a scarily easy fix that they somehow didn't see *entirely*
    public int getXWithOffset(int x, int z) {
        switch (this.coordBaseMode) {
            case 0:
                return this.boundingBox.minX() + x;
            case 1:
                return this.boundingBox.maxX() - z;
            case 2:
                return this.boundingBox.maxX() - x;
            case 3:
                return this.boundingBox.minX() + z;
            default:
                return x;
        }
    }

    public int getYWithOffset(int y) {
        return this.coordBaseMode == -1 ? y : y + this.boundingBox.minY();
    }

    public int getZWithOffset(int x, int z) {
        switch (this.coordBaseMode) {
            case 0:
                return this.boundingBox.minZ() + z;
            case 1:
                return this.boundingBox.minZ() + x;
            case 2:
                return this.boundingBox.maxZ() - z;
            case 3:
                return this.boundingBox.maxZ() - x;
            default:
                return z;
        }
    }

    /** 1.7 {@code StructureComponent.findIntersecting}. */
    public static @Nullable StructurePiece findIntersecting(List<StructurePiece> components, BoundingBox box) {
        for (StructurePiece component : components) {
            if (component.getBoundingBox() != null && component.getBoundingBox().intersects(box)) return component;
        }
        return null;
    }

    /** 1.7 {@code StructureComponent.getMetadataWithOffset}, using vanilla's rotation table for its own blocks. */
    protected int getMetadataWithOffset(LegacyBlock block, int meta) {
        if (block == Vanilla.rail) {
            if (this.coordBaseMode == 1 || this.coordBaseMode == 3) {
                if (meta == 1) return 0;
                return 1;
            }
        } else if (block != Vanilla.wooden_door && block != Vanilla.iron_door) {
            if (block != Vanilla.stone_stairs && block != Vanilla.oak_stairs && block != Vanilla.nether_brick_stairs
                    && block != Vanilla.stone_brick_stairs && block != Vanilla.sandstone_stairs) {
                if (block == Vanilla.ladder) {
                    if (this.coordBaseMode == 0) {
                        if (meta == 2) return 3;
                        if (meta == 3) return 2;
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 2) return 4;
                        if (meta == 3) return 5;
                        if (meta == 4) return 2;
                        if (meta == 5) return 3;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 2) return 5;
                        if (meta == 3) return 4;
                        if (meta == 4) return 2;
                        if (meta == 5) return 3;
                    }
                } else if (block == Vanilla.stone_button) {
                    if (this.coordBaseMode == 0) {
                        if (meta == 3) return 4;
                        if (meta == 4) return 3;
                    } else if (this.coordBaseMode == 1) {
                        if (meta == 3) return 1;
                        if (meta == 4) return 2;
                        if (meta == 2) return 3;
                        if (meta == 1) return 4;
                    } else if (this.coordBaseMode == 3) {
                        if (meta == 3) return 2;
                        if (meta == 4) return 1;
                        if (meta == 2) return 3;
                        if (meta == 1) return 4;
                    }
                } else if (block != Vanilla.tripwire_hook && !isDirectional(block)) {
                    if (block == Vanilla.piston || block == Vanilla.sticky_piston || block == Vanilla.lever || block == Vanilla.dispenser) {
                        if (this.coordBaseMode == 0) {
                            if (meta == 2 || meta == 3) return meta ^ 1; // Facing.oppositeSide
                        } else if (this.coordBaseMode == 1) {
                            if (meta == 2) return 4;
                            if (meta == 3) return 5;
                            if (meta == 4) return 2;
                            if (meta == 5) return 3;
                        } else if (this.coordBaseMode == 3) {
                            if (meta == 2) return 5;
                            if (meta == 3) return 4;
                            if (meta == 4) return 2;
                            if (meta == 5) return 3;
                        }
                    }
                } else if (this.coordBaseMode == 0) {
                    if (meta == 0 || meta == 2) return ROTATE_OPPOSITE[meta];
                } else if (this.coordBaseMode == 1) {
                    if (meta == 2) return 1;
                    if (meta == 0) return 3;
                    if (meta == 1) return 2;
                    if (meta == 3) return 0;
                } else if (this.coordBaseMode == 3) {
                    if (meta == 2) return 3;
                    if (meta == 0) return 1;
                    if (meta == 1) return 2;
                    if (meta == 3) return 0;
                }
            } else if (this.coordBaseMode == 0) {
                if (meta == 2) return 3;
                if (meta == 3) return 2;
            } else if (this.coordBaseMode == 1) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 0;
                if (meta == 3) return 1;
            } else if (this.coordBaseMode == 2) {
                if (meta == 2) return 3;
                if (meta == 3) return 2;
            } else if (this.coordBaseMode == 3) {
                if (meta == 0) return 2;
                if (meta == 1) return 3;
                if (meta == 2) return 1;
                if (meta == 3) return 0;
            }
        } else if (this.coordBaseMode == 0) {
            if (meta == 0) return 2;
            if (meta == 2) return 0;
        } else {
            if (this.coordBaseMode == 1) return meta + 1 & 3;
            if (this.coordBaseMode == 3) return meta + 3 & 3;
        }

        return meta;
    }

    /** 1.7 {@code net.minecraft.util.Direction.rotateOpposite}. */
    private static final int[] ROTATE_OPPOSITE = {2, 3, 0, 1};

    /** 1.7 {@code instanceof BlockDirectional}: beds, gates, pumpkins, repeaters, comparators, cocoa, end portal frames. */
    private static boolean isDirectional(LegacyBlock block) {
        return block == Vanilla.bed || block == Vanilla.fence_gate || block == Vanilla.pumpkin || block == Vanilla.lit_pumpkin
                || block == Vanilla.unpowered_repeater || block == Vanilla.powered_repeater || block == Vanilla.unpowered_comparator
                || block == Vanilla.powered_comparator || block == Vanilla.cocoa || block == Vanilla.end_portal_frame;
    }

    /** current Position depends on currently set Coordinates mode, is computed here */
    protected void placeBlockAtCurrentPosition(WorldGenLevel world, LegacyBlock block, int meta, int x, int y, int z, BoundingBox box) {
        int posX = this.getXWithOffset(x, z);
        int posY = this.getYWithOffset(y);
        int posZ = this.getZWithOffset(x, z);

        if (box.isInside(posX, posY, posZ)) {
            setBlock(world, posX, posY, posZ, block, meta);
        }
    }

    protected BlockState getBlockAtCurrentPosition(WorldGenLevel world, int x, int y, int z, BoundingBox box) {
        int posX = this.getXWithOffset(x, z);
        int posY = this.getYWithOffset(y);
        int posZ = this.getZWithOffset(x, z);
        return !box.isInside(posX, posY, posZ) ? Vanilla.air.state(0) : getState(world, posX, posY, posZ);
    }

    /** 1.7 {@code StructureComponent.generateStructureChestContents}: a chest with meta 0 and its loot. */
    protected boolean generateStructureChestContents(WorldGenLevel world, BoundingBox box, RandomSource rand, int x, int y, int z,
                                                     List<ItemPool.Entry> content, int amount) {
        int posX = this.getXWithOffset(x, z);
        int posY = this.getYWithOffset(y);
        int posZ = this.getZWithOffset(x, z);

        if (box.isInside(posX, posY, posZ) && !Vanilla.chest.is(getState(world, posX, posY, posZ), 0)) {
            setBlock(world, posX, posY, posZ, Vanilla.chest, 0);
            Container chest = inventoryAt(world, posX, posY, posZ);

            if (chest != null) {
                generateChestContents(rand, content, chest, amount);
            }

            return true;
        } else {
            return false;
        }
    }

    /** The inventory at a world position, or null (1.7's {@code (TileEntityChest) world.getTileEntity(...)}). */
    protected static @Nullable Container inventoryAt(WorldGenLevel world, int x, int y, int z) {
        return getTileEntity(world, x, y, z) instanceof Container container ? container : null;
    }

    /**
     * 1.7 {@code WeightedRandomChestContent.generateChestContents}: {@code count} weighted draws, each stack put into a
     * random slot (overwriting); a draw larger than a stack splits into single items. A null inventory is skipped
     * (1.7 threw).
     */
    protected static void generateChestContents(RandomSource rand, List<ItemPool.Entry> content, @Nullable Container inventory, int count) {
        if (inventory == null) return;
        int size = inventory.getContainerSize();
        if (size <= 0) return;
        for (int j = 0; j < count; ++j) {
            ItemStack drawn = ItemPool.getStack(content, rand);
            if (drawn.isEmpty()) continue;
            if (drawn.getCount() <= drawn.getMaxStackSize()) {
                inventory.setItem(rand.nextInt(size), drawn);
            } else {
                for (int i = 0; i < drawn.getCount(); i++) {
                    inventory.setItem(rand.nextInt(size), ItemStacks.copyWithCount(drawn, 1));
                }
            }
        }
        if (inventory instanceof BlockEntity be) be.setChanged();
    }

    /** 1.7 {@code ItemDoor.placeDoorBlock}: both halves, hinge from the neighbours, clipped to the chunk being built. */
    protected static void placeDoorBlock(WorldGenLevel world, int x, int y, int z, int meta, LegacyBlock door) {
        int b0 = 0;
        int b1 = 0;

        if (meta == 0) b1 = 1;
        if (meta == 1) b0 = -1;
        if (meta == 2) b1 = -1;
        if (meta == 3) b0 = 1;

        int i1 = (isNormalCube(world, x - b0, y, z - b1) ? 1 : 0) + (isNormalCube(world, x - b0, y + 1, z - b1) ? 1 : 0);
        int j1 = (isNormalCube(world, x + b0, y, z + b1) ? 1 : 0) + (isNormalCube(world, x + b0, y + 1, z + b1) ? 1 : 0);
        boolean flag = door.is(getState(world, x - b0, y, z - b1), meta) || door.is(getState(world, x - b0, y + 1, z - b1), meta);
        boolean flag1 = door.is(getState(world, x + b0, y, z + b1), meta) || door.is(getState(world, x + b0, y + 1, z + b1), meta);
        boolean flag2 = false;

        if (flag && !flag1) {
            flag2 = true;
        } else if (j1 > i1) {
            flag2 = true;
        }

        placeDoorHalves(world, x, y, z, door, meta, 8 | (flag2 ? 1 : 0));
    }

    /** A 1.7 door from both halves' metadata (facing/open below, hinge above), as 1.7 kept them. */
    protected static void placeDoorHalves(WorldGenLevel world, int x, int y, int z, LegacyBlock door, int metaBottom, int metaTop) {
        BlockState base = door.state(metaBottom).getBlock().defaultBlockState();
        setState(world, x, y, z, LegacyStructurePalette.door(base, metaBottom, metaTop));
        setState(world, x, y + 1, z, LegacyStructurePalette.door(base, metaTop, metaBottom));
    }

    /** 1.7 {@code Block.isNormalCube}: a full opaque cube that does not conduct... i.e. a redstone conductor. */
    private static boolean isNormalCube(WorldGenLevel world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        return world.getBlockState(pos).isRedstoneConductor(world, pos);
    }

    // 1.7 Component: metadata for decoration methods

    /**
     * Gets metadata for rotatable pillars.
     * @param metadata (First two digits are equal to block metadata, other two are equal to orientation
     * @return metadata adjusted for random orientation
     */
    protected int getPillarMeta(int metadata) {
        if (this.coordBaseMode % 2 != 0 && this.coordBaseMode != -1)
            metadata = metadata ^ 12;

        return metadata;
    }

    /**
     * Gets metadata for rotatable DecoBlock
     * honestly i don't remember how i did this and i'm scared to optimize it because i fail to see any reasonable patterns like the pillar
     * should work for hoppers, just flip dir for N/S and W/E
     * @param metadata (2 for facing South, 3 for facing North, 4 for facing East, 5 for facing West
     */
    protected int getDecoMeta(int metadata) {
        // 1.7 has no break after each inner switch: an unmatched metadata falls into the next rotation's table
        switch (this.coordBaseMode) {
            case 0: //South
                switch (metadata) {
                    case 2: return 2;
                    case 3: return 3;
                    case 4: return 4;
                    case 5: return 5;
                }
            case 1: //West
                switch (metadata) {
                    case 2: return 5;
                    case 3: return 4;
                    case 4: return 2;
                    case 5: return 3;
                }
            case 2: //North
                switch (metadata) {
                    case 2: return 3;
                    case 3: return 2;
                    case 4: return 5;
                    case 5: return 4;
                }
            case 3: //East
                switch (metadata) {
                    case 2: return 4;
                    case 3: return 5;
                    case 4: return 3;
                    case 5: return 2;
                }
        }
        return 0;
    }

    /**
     * Get orientation-offset metadata for BlockDecoModel; also suitable for trapdoors
     * @param metadata (0 for facing North, 1 for facing South, 2 for facing West, 3 for facing East)
     */
    protected int getDecoModelMeta(int metadata) {
        //N: 0b00, S: 0b01, W: 0b10, E: 0b11

        switch (this.coordBaseMode) {
            default: //South
                break;
            case 1: //West
                if ((metadata & 3) < 2) //N & S can just have bits toggled
                    metadata = metadata ^ 3;
                else //W & E can just have first bit set to 0
                    metadata = metadata ^ 2;
                break;
            case 2: //North
                metadata = metadata ^ 1; //N, W, E & S can just have first bit toggled
                break;
            case 3: //East
                if ((metadata & 3) < 2)//N & S can just have second bit set to 1
                    metadata = metadata ^ 2;
                else //W & E can just have bits toggled
                    metadata = metadata ^ 3;
                break;
        }
        //genuinely like. why did i do that
        return metadata << 2; //To accommodate for BlockDecoModel's shift in the rotation bits; otherwise, simply bit-shift right and or any non-rotation meta after
    }

    //works for crts, toasters, and anything that follows mc's cardinal dirs. S: 0, W: 1, N: 2, E: 3
    protected int getCRTMeta(int meta) {
        return (meta + this.coordBaseMode) % 4;
    }

    /**
     * Gets orientation-adjusted meta for stairs.
     * 0 = West, 1 = East, 2 = North, 3 = South
     */
    protected int getStairMeta(int metadata) {
        switch (this.coordBaseMode) {
            default: //South
                break;
            case 1: //West
                if ((metadata & 3) < 2) //Flip second bit for E/W
                    metadata = metadata ^ 2;
                else
                    metadata = metadata ^ 3; //Flip both bits for N/S
                break;
            case 2: //North
                metadata = metadata ^ 1; //Flip first bit
                break;
            case 3: //East
                if ((metadata & 3) < 2) //Flip both bits for E/W
                    metadata = metadata ^ 3;
                else //Flip second bit for N/S
                    metadata = metadata ^ 2;
                break;
        }

        return metadata;
    }

    /*
     * Assuming door is on opposite side of block from direction: East: 0, South: 1, West: 2, North: 3
     * The 4th bit (8) marks the top block, whose 1st bit is the right hinge; below it, bits 1 & 2 are the facing and
     * the 3rd bit (4) is open.
     */
    protected void placeDoor(WorldGenLevel world, BoundingBox box, LegacyBlock door, int dirMeta, boolean opensRight, boolean isOpen, int featureX, int featureY, int featureZ) { //isOpen for randomly opened doors
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return;

        switch (this.coordBaseMode) {
            default: //South
                break;
            case 1: //West
                dirMeta = (dirMeta + 1) % 4; break;
            case 2: //North
                dirMeta ^= 2; break; //Flip second bit
            case 3: //East
                dirMeta = (dirMeta + 3) % 4; break; //fuck you modulo
        }

        //hee hoo
        int metaTop = opensRight ? 0b1001 : 0b1000;
        int metaBottom = dirMeta | (isOpen ? 0b100 : 0);

        BlockPos below = new BlockPos(posX, posY - 1, posZ);
        // 1.7 World.doesBlockHaveSolidTopSurface
        if (world.getBlockState(below).isFaceSturdy(world, below, Direction.UP)) {
            placeDoorHalves(world, posX, posY, posZ, door, metaBottom, metaTop);
        }
    }

    /** 1 for west face, 2 for east face, 3 for north, 4 for south*/
    protected void placeLever(WorldGenLevel world, BoundingBox box, int dirMeta, boolean on, int featureX, int featureY, int featureZ) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return;

        if (dirMeta <= 0 || dirMeta >= 7) { //levers suck ass
            switch (this.coordBaseMode) {
                case 1: case 3: //west / east
                    dirMeta ^= 0b111;
            }
        } else if (dirMeta >= 5) {
            switch (this.coordBaseMode) {
                case 1: case 3: //west / east
                    dirMeta = (dirMeta + 1) % 2 + 5;
            }
        } else {
            dirMeta = getButtonMeta(dirMeta);
        }

        setBlock(world, posX, posY, posZ, Vanilla.lever, on ? dirMeta | 8 : dirMeta);
    }

    /** pain. works for side-facing levers as well */
    protected int getButtonMeta(int dirMeta) {
        switch (this.coordBaseMode) { //are you ready for the pain?
            case 1: //West
                if (dirMeta <= 2) return dirMeta + 2;
                else if (dirMeta < 4) return dirMeta - 1;
                else return dirMeta - 3;// this shit sucks ass
            case 2: //North
                return dirMeta + (dirMeta % 2 == 0 ? -1 : 1);
            case 3: //East
                if (dirMeta <= 1) return dirMeta + 3;
                else if (dirMeta <= 2) return dirMeta + 1;
                else return dirMeta - 2;
            default: //South
                return dirMeta;
        }
    }

    /**N:0 W:1 S:2 E:3 */
    protected void placeBed(WorldGenLevel world, BoundingBox box, int meta, int featureX, int featureY, int featureZ) {
        int xOffset = 0;
        int zOffset = 0;

        switch (meta & 3) {
            default:
                zOffset = 1; break;
            case 1:
                xOffset = -1; break;
            case 2:
                zOffset = -1; break;
            case 3:
                xOffset = 1; break;
        }

        switch (this.coordBaseMode) {
            default: //S
                break;
            case 1: //W
                meta = (meta + 1) % 4; break;
            case 2: //N
                meta ^= 2; break;
            case 3: //E
                meta = (meta - 1) % 4; break; // 1.7: (0 - 1) % 4 is -1, which setBlock stored as metadata 15
        }

        placeBlockAtCurrentPosition(world, Vanilla.bed, meta, featureX, featureY, featureZ, box);
        placeBlockAtCurrentPosition(world, Vanilla.bed, meta + 8, featureX + xOffset, featureY, featureZ + zOffset, box);
    }

    /**Tripwire Hook: S:0 W:1 N:2 E:3 */
    protected int getTripwireMeta(int metadata) {
        switch (this.coordBaseMode) {
            default:
                return metadata;
            case 1:
                return (metadata + 1) % 4;
            case 2:
                return metadata ^ 2;
            case 3:
                return (metadata - 1) % 4;
        }
    }

    // 1.7 Component: loot

    protected boolean generateInvContents(WorldGenLevel world, BoundingBox box, RandomSource rand, LegacyBlock block, int featureX, int featureY, int featureZ, List<ItemPool.Entry> content, int amount) {
        return generateInvContents(world, box, rand, block, 0, featureX, featureY, featureZ, content, amount);
    }

    /**
     * Places {@code block} with loot. 1.7 answered {@code true} for a position outside the populating box ("replacement
     * for hasPlacedLoot checks"); the port answers {@code false} there, so a {@code hasPlacedLoot} flag only turns once
     * the chunk holding the container has built it (1.7's populate boxes happened to contain these positions).
     */
    protected boolean generateInvContents(WorldGenLevel world, BoundingBox box, RandomSource rand, LegacyBlock block, int meta, int featureX, int featureY, int featureZ, List<ItemPool.Entry> content, int amount) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return false;
        if (block.is(getState(world, posX, posY, posZ), meta)) //replacement for hasPlacedLoot checks
            return true;

        this.placeBlockAtCurrentPosition(world, block, meta, featureX, featureY, featureZ, box);
        Container inventory = inventoryAt(world, posX, posY, posZ);

        if (inventory != null) {
            amount = (int) Math.floor(amount * StructureConfig.lootAmountFactor);
            generateChestContents(rand, content, inventory, amount < 1 ? 1 : amount);
            return true;
        }

        return false;
    }

    protected boolean generateLockableContents(WorldGenLevel world, BoundingBox box, RandomSource rand, LegacyBlock block, int featureX, int featureY, int featureZ,
                                               List<ItemPool.Entry> content, int amount, double mod) {
        return generateLockableContents(world, box, rand, block, 0, featureX, featureY, featureZ, content, amount, mod);
    }

    protected boolean generateLockableContents(WorldGenLevel world, BoundingBox box, RandomSource rand, LegacyBlock block, int meta, int featureX, int featureY, int featureZ,
                                               List<ItemPool.Entry> content, int amount, double mod) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ) || block.is(getState(world, posX, posY, posZ), meta)) //replacement for hasPlacedLoot checks
            return false;

        this.placeBlockAtCurrentPosition(world, block, meta, featureX, featureY, featureZ, box);
        BlockEntity tile = getTileEntity(world, posX, posY, posZ);

        if (tile instanceof TileEntityLockableBase lock && tile instanceof Container inventory) {
            amount = (int) Math.floor(amount * StructureConfig.lootAmountFactor);
            generateChestContents(rand, content, inventory, amount < 1 ? 1 : amount);

            lock.setPins(rand.nextInt(999) + 1);
            lock.setMod(mod);
            lock.lock();
            return true;
        }

        return false;
    }

    protected void generateLoreBook(WorldGenLevel world, BoundingBox box, int featureX, int featureY, int featureZ, int slot, @Nullable ItemStack stack) { //kept for compat
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        if (!box.isInside(posX, posY, posZ)) return;

        Container inventory = inventoryAt(world, posX, posY, posZ);

        if (inventory != null && stack != null && slot < inventory.getContainerSize()) {
            inventory.setItem(slot, stack);
        }
    }

    /**
     * Places random bobblehead with a randomized orientation at specified location
     */
    protected void placeRandomBobble(WorldGenLevel world, BoundingBox box, RandomSource rand, int featureX, int featureY, int featureZ) {
        int posX = this.getXWithOffset(featureX, featureZ);
        int posY = this.getYWithOffset(featureY);
        int posZ = this.getZWithOffset(featureX, featureZ);

        placeBlockAtCurrentPosition(world, Mod.bobblehead, rand.nextInt(16), featureX, featureY, featureZ, box);
        randomBobble(world, rand, posX, posY, posZ);
    }

    /** 1.7 {@code bobble.type = BobbleType.values()[rand.nextInt(BobbleType.values().length - 1) + 1]} (never NONE). */
    protected static void randomBobble(WorldGenLevel world, RandomSource rand, int posX, int posY, int posZ) {
        if (getTileEntity(world, posX, posY, posZ) instanceof TileEntityBobble bobble) {
            bobble.setTypeOrdinal(rand.nextInt(BlockBobble.BobbleType.VALUES.length - 1) + 1);
            bobble.setChanged();
        }
    }

    /** 1.7 {@code HbmChestContents.generateOfficeBook}. */
    protected static @Nullable ItemStack generateOfficeBook(RandomSource rand) { //TODO rework this lore in general
        String key;
        int pages;
        switch (rand.nextInt(5)) {
            case 0: key = "resignation_note"; pages = 3; break;
            case 1: key = "memo_stocks"; pages = 1; break;
            case 2: key = "memo_schrab_gsa"; pages = 2; break;
            case 3: key = "memo_schrab_rd"; pages = 4; break;
            case 4: key = "memo_schrab_nuke"; pages = 3; break;
            default: return null;
        }

        return ItemBookLore.createBook(key, pages, 0x6BC8FF, 0x0A0A0A);
    }

    /** 1.7 {@code HbmChestContents.generateLabBook}. */
    protected static @Nullable ItemStack generateLabBook(RandomSource rand) {
        String key;
        int pages;

        switch (rand.nextInt(5)) {
            case 0: key = "bf_bomb_1"; pages = 4; break;
            case 1: key = "bf_bomb_2"; pages = 6; break;
            case 2: key = "bf_bomb_3"; pages = 6; break;
            case 3: key = "bf_bomb_4"; pages = 5; break;
            case 4: key = "bf_bomb_5"; pages = 9; break;
            default: return null;
        }

        return ItemBookLore.createBook(key, pages, 0x1E1E1E, 0x46EA44);
    }

    /** 1.7 {@code LootGenerator.lootMedicine} on a loot pile the builder just placed; clipped to the chunk being built. */
    protected static void lootMedicine(WorldGenLevel world, int x, int y, int z) {
        if (writable(x, y, z)) LootGenerator.lootMedicine(world, new BlockPos(x, y, z));
    }

    /** 1.7 {@code LootGenerator.lootMakeshiftGun}; clipped to the chunk being built. */
    protected static void lootMakeshiftGun(WorldGenLevel world, int x, int y, int z) {
        if (writable(x, y, z)) LootGenerator.lootMakeshiftGun(world, new BlockPos(x, y, z));
    }

    /** 1.7 {@code LootGenerator.lootCapStash}; clipped to the chunk being built. */
    protected static void lootCapStash(WorldGenLevel world, int x, int y, int z) {
        if (writable(x, y, z)) LootGenerator.lootCapStash(world, new BlockPos(x, y, z));
    }

    /** 1.7 {@code LootGenerator.lootBookLore}; clipped to the chunk being built. */
    protected static void lootBookLore(WorldGenLevel world, int x, int y, int z, @Nullable ItemStack book) {
        if (book != null && writable(x, y, z)) LootGenerator.lootBookLore(world, new BlockPos(x, y, z), book);
    }

    // 1.7 Component: block placement utilities

    /**
     * Places blocks underneath location until reaching a solid block; good for foundations
     */
    protected void placeFoundationUnderneath(WorldGenLevel world, LegacyBlock placeBlock, int meta, int minX, int minZ, int maxX, int maxZ, int featureY, BoundingBox box) {

        for (int featureX = minX; featureX <= maxX; featureX++) {
            for (int featureZ = minZ; featureZ <= maxZ; featureZ++) {
                int posX = this.getXWithOffset(featureX, featureZ);
                int posY = this.getYWithOffset(featureY);
                int posZ = this.getZWithOffset(featureX, featureZ);

                if (box.isInside(posX, posY, posZ)) {
                    BlockState block = getState(world, posX, posY, posZ);
                    int brake = 0;

                    // 1.7: air, a non-solid material, or foliage/leaves; stops at y 1 or after 16 blocks
                    while ((block.isAir() || !block.isSolid() || block.is(BlockTags.LEAVES))
                            && posY > world.getMinY() + 1 && brake <= 15) {
                        setBlock(world, posX, posY, posZ, placeBlock, meta);
                        block = getState(world, posX, --posY, posZ);

                        brake++;
                    }
                }
            }
        }
    }

    /**
     * Places specified blocks on top of pre-existing blocks in a given area, up to a certain height. Does NOT place blocks on top of liquids.
     * Useful for stuff like fences and walls most likely.
     */
    protected void placeBlocksOnTop(WorldGenLevel world, BoundingBox box, LegacyBlock block, int minX, int minZ, int maxX, int maxZ, int height) {

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                int posX = this.getXWithOffset(x, z);
                int posZ = this.getZWithOffset(x, z);
                // 1.7 had no box check here; only the chunk being built may be read and written
                if (!box.isInside(posX, box.minY(), posZ)) continue;
                int topHeight = topSolid(world, posX, posZ);

                if (getState(world, posX, topHeight, posZ).getFluidState().isEmpty()) {

                    for (int i = 0; i < height; i++) {
                        int posY = topHeight + i;

                        setBlock(world, posX, posY, posZ, block, 0);
                    }
                }
            }
        }
    }

    /** Methods that are actually optimized, including ones that cut out replaceBlock and onlyReplace functionality when it's redundant. */
    protected void fillWithAir(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        setBlock(world, posX, posY, posZ, Vanilla.air, 0);
                    }
                }
            }
        }
    }

    protected void fillWithBlocks(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block, LegacyBlock replaceBlock, boolean onlyReplace) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (!onlyReplace || !getState(world, posX, posY, posZ).isAir()) {
                            if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ)
                                setBlock(world, posX, posY, posZ, replaceBlock, 0);
                            else
                                setBlock(world, posX, posY, posZ, block, 0);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithBlocks(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        setBlock(world, posX, posY, posZ, block, 0);
                    }
                }
            }
        }
    }

    protected void fillWithMetadataBlocks(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block, int meta, LegacyBlock replaceBlock, int replaceMeta, boolean onlyReplace) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (!onlyReplace || !getState(world, posX, posY, posZ).isAir()) {
                            if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ)
                                setBlock(world, posX, posY, posZ, replaceBlock, replaceMeta);
                            else
                                setBlock(world, posX, posY, posZ, block, meta);
                        }
                    }
                }
            }
        }
    }

    protected void fillWithMetadataBlocks(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block, int meta) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        setBlock(world, posX, posY, posZ, block, meta);
                    }
                }
            }
        }
    }

    protected void fillWithRandomizedBlocks(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean onlyReplace, RandomSource rand, BlockSelector selector) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (!onlyReplace || !getState(world, posX, posY, posZ).isAir()) {
                            setState(world, posX, posY, posZ, selector.pick(rand, posX, posY, posZ, x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ, 0));
                        }
                    }
                }
            }
        }
    }

    //TODO replace the shitty block selector with something else. probably a lambda that returns a metablock for convenience
    protected void fillWithRandomizedBlocks(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, RandomSource rand, BlockSelector selector) { //so i don't have to replace shit

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);
                        //keep this functionality in mind!
                        setState(world, posX, posY, posZ, selector.pick(rand, posX, posY, posZ, false, 0)); //for most structures it's redundant since nothing is just hollow cubes, but vanilla structures rely on this. use the method above in that case.
                    }
                }
            }
        }
    }

    //stairs and shit
    protected void fillWithRandomizedBlocksMeta(WorldGenLevel world, BoundingBox box, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, RandomSource rand, BlockSelector selector, int meta) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);
                        //keep this functionality in mind!
                        setState(world, posX, posY, posZ, selector.pick(rand, posX, posY, posZ, false, meta)); //Make sure the metadata is initialized in the ctor!
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(WorldGenLevel world, BoundingBox box, RandomSource rand, float randLimit, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block, LegacyBlock replaceBlock, boolean onlyReplace) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (rand.nextFloat() <= randLimit && (!onlyReplace || !getState(world, posX, posY, posZ).isAir())) {
                            if (x != minX && x != maxX && y != minY && y != maxY && z != minZ && z != maxZ)
                                setBlock(world, posX, posY, posZ, replaceBlock, 0);
                            else
                                setBlock(world, posX, posY, posZ, block, 0);
                        }
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(WorldGenLevel world, BoundingBox box, RandomSource rand, float randLimit, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (rand.nextFloat() <= randLimit)
                            setBlock(world, posX, posY, posZ, block, 0);
                    }
                }
            }
        }
    }

    protected void randomlyFillWithBlocks(WorldGenLevel world, BoundingBox box, RandomSource rand, float randLimit, int minX, int minY, int minZ, int maxX, int maxY, int maxZ, LegacyBlock block, int meta) {

        if (getYWithOffset(minY) < box.minY() || getYWithOffset(maxY) > box.maxY())
            return;

        for (int x = minX; x <= maxX; x++) {

            for (int z = minZ; z <= maxZ; z++) {
                int posX = getXWithOffset(x, z);
                int posZ = getZWithOffset(x, z);

                if (posX >= box.minX() && posX <= box.maxX() && posZ >= box.minZ() && posZ <= box.maxZ()) {
                    for (int y = minY; y <= maxY; y++) {
                        int posY = getYWithOffset(y);

                        if (rand.nextFloat() <= randLimit)
                            setBlock(world, posX, posY, posZ, block, meta);
                    }
                }
            }
        }
    }

    protected ForgeDirection getDirection(ForgeDirection dir) {
        switch (coordBaseMode) {
            default: //South
                return dir;
            case 1: //West
                return dir.getRotation(ForgeDirection.UP);
            case 2: //North
                return dir.getOpposite();
            case 3: //East
                return dir.getRotation(ForgeDirection.DOWN);
        }
    }

    /**
     * Sets the core block for a BlockDummyable multiblock. For BlockDummyables, 'dir' <b>always</b> faces the player,
     * being the opposite of the player's direction. This is already taken into account.
     */
    protected void placeCore(WorldGenLevel world, BoundingBox box, LegacyBlock block, @Nullable ForgeDirection dir, int x, int y, int z) {
        int posX = getXWithOffset(x, z);
        int posZ = getZWithOffset(x, z);
        int posY = getYWithOffset(y);

        if (!box.isInside(posX, posY, posZ)) return;

        if (dir == null)
            dir = ForgeDirection.NORTH;

        dir = getDirection(dir.getOpposite());
        setBlock(world, posX, posY, posZ, block, dir.ordinal() + 10); // BlockDummyable.offset
    }

    /**
     * 1.7 {@code fillSpace}: the multiblock's cells around a core, clipped to the chunk being built. 1.7 wrote the
     * dummy cells itself from the rotated {@code dim}; here the multiblock writes its own cells
     * ({@link BlockMultiblockCore#fillSpace}), which follow the same dimensions and facing, restricted to the box.
     */
    protected void fillSpace(WorldGenLevel world, BoundingBox box, int x, int y, int z, int[] dim, LegacyBlock block, @Nullable ForgeDirection dir) {

        if (getYWithOffset(y - dim[1]) < box.minY() || getYWithOffset(y + dim[0]) > box.maxY()) //the BlockDummyable will be fucked regardless if it goes beyond either limit
            return;

        if (dir == null)
            dir = ForgeDirection.NORTH;

        final ForgeDirection facing = getDirection(dir.getOpposite());

        if (!(block.block() instanceof BlockMultiblockCore core)) return;

        int posX = getXWithOffset(x, z);
        int posZ = getZWithOffset(x, z);
        int posY = getYWithOffset(y);
        Build build = BUILD.get();
        BlockMultiblockCore.withoutTeardown(() -> core.fillSpace(world, new BlockPos(posX, posY, posZ), facing.toDirection(),
                build == null ? null : build.box));
    }

    /**
     * 1.7 {@code makeExtra}: flagged the cells a machine connects through. This port's multiblocks declare those
     * cells in their own mask tables, so there is nothing to write.
     */
    public void makeExtra(WorldGenLevel world, BoundingBox box, LegacyBlock block, int x, int y, int z) {}

    // Block Selectors

    /**
     * 1.7 {@code StructureComponent.BlockSelector}: a (block, metadata) pick per position. Selectors are shared (1.7
     * kept several in static fields), and chunks build on several threads, so the pick-and-read is one synchronised
     * step ({@link #pick}).
     */
    public abstract static class BlockSelector {
        protected LegacyBlock block = Vanilla.air;
        protected int meta;

        public abstract void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior);

        public LegacyBlock getBlock() {
            return this.block;
        }

        public int getMeta() {
            return this.meta;
        }

        /** {@code selectBlocks}, then the selected block with {@code extraMeta | selected metadata}. */
        public synchronized BlockState pick(RandomSource rand, int posX, int posY, int posZ, boolean notInterior, int extraMeta) {
            selectBlocks(rand, posX, posY, posZ, notInterior);
            return getBlock().state(extraMeta | getMeta());
        }
    }

    /**
     * 1.7 {@code NBTStructure.transformBlock} for a {@code JigsawPiece.blockTable}: the selector's pick when
     * {@code original} (the piece's own block) has an entry, keyed by the port block's id path; otherwise null.
     */
    public static @Nullable BlockState blockTablePick(Map<String, BlockSelector> blockTable, Block original, RandomSource rand) {
        var id = BuiltInRegistries.BLOCK.getKey(original);
        if (!"hbm".equals(id.getNamespace())) return null;
        BlockSelector selector = blockTable.get(id.getPath());
        return selector == null ? null : selector.pick(rand, 0, 0, 0, false, 0); // fuck the vanilla shit idc
    }

    static class Sandstone extends BlockSelector {

        Sandstone() { }

        /** Selects blocks */
        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance > 0.6F) {
                this.block = Vanilla.sandstone;
            } else if (chance < 0.5F) {
                this.block = Mod.reinforced_sand;
            } else {
                this.block = Vanilla.sand;
            }
        }
    }

    static class ConcreteBricks extends BlockSelector {

        ConcreteBricks() { }

        /** Selects blocks */
        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.4F) {
                this.block = Mod.brick_concrete;
            } else if (chance < 0.7F) {
                this.block = Mod.brick_concrete_mossy;
            } else if (chance < 0.9F) {
                this.block = Mod.brick_concrete_cracked;
            } else {
                this.block = Mod.brick_concrete_broken;
            }
        }
    }

    static class ConcreteBricksStairs extends BlockSelector {

        ConcreteBricksStairs() {
            this.meta = 0;
        }

        /** Selects blocks */
        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.4F) {
                this.block = Mod.brick_concrete_stairs;
            } else if (chance < 0.7F) {
                this.block = Mod.brick_concrete_mossy_stairs;
            } else if (chance < 0.9F) {
                this.block = Mod.brick_concrete_cracked_stairs;
            } else {
                this.block = Mod.brick_concrete_broken_stairs;
            }
        }
    }

    static class ConcreteBricksSlabs extends BlockSelector {

        ConcreteBricksSlabs() {
            this.block = Mod.concrete_brick_slab;
            this.meta = 0;
        }

        /** Selects blocks; 1.7 ORs into the metadata without clearing it, so it drifts to 3 (kept) */
        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance >= 0.4F && chance < 0.7F) {
                this.meta |= 1;
            } else if (chance < 0.9F) {
                this.meta |= 2;
            } else {
                this.meta |= 3;
            }
        }
    }

    //ag
    static class LabTiles extends BlockSelector {

        LabTiles() { }

        /** Selects blocks */
        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.5F) {
                this.block = Mod.tile_lab;
            } else if (chance < 0.9F) {
                this.block = Mod.tile_lab_cracked;
            } else {
                this.block = Mod.tile_lab_broken;
            }
        }
    }

    static class SuperConcrete extends BlockSelector {

        SuperConcrete() {
            this.block = Mod.concrete_super;
        }

        /** Selects blocks */
        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            this.meta = rand.nextInt(6) + 10;
        }
    }

    public static class MeteorBricks extends BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.4F) {
                this.block = Mod.meteor_brick;
            } else if (chance < 0.7F) {
                this.block = Mod.meteor_brick_mossy;
            } else {
                this.block = Mod.meteor_brick_cracked;
            }
        }

    }

    public static class SupplyCrates extends BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.6F) {
                this.block = Vanilla.air;
            } else if (chance < 0.8F) {
                this.block = Mod.crate_ammo;
            } else if (chance < 0.9F) {
                this.block = Mod.crate_can;
            } else {
                this.block = Mod.crate;
            }
        }

    }

    public static class CrabSpawners extends BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.8F) {
                this.block = Mod.meteor_brick;
            } else {
                this.block = Mod.meteor_spawner;
            }
        }

    }

    public static class GreenOoze extends BlockSelector {

        @Override
        public void selectBlocks(RandomSource rand, int posX, int posY, int posZ, boolean notInterior) {
            float chance = rand.nextFloat();

            if (chance < 0.8F) {
                this.block = Mod.toxic_block;
            } else {
                this.block = Mod.meteor_polished;
            }
        }

    }
}
