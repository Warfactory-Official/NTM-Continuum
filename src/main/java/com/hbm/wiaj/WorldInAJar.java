package com.hbm.wiaj;

import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.blocks.machine.FoundryOutlet;
import com.hbm.blocks.machine.FoundryTank;
import com.hbm.blocks.network.BlockCable;
import com.hbm.blocks.network.BlockFluidDuct;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.tileentity.network.TileEntityFluidDuctNT;
import com.hbm.wiaj.cannery.Dummies;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/**
 * The scripted jar: a small cube of blocks plus the "tile entities" a script parks in it.
 *
 * <p>1.7 handed this to {@code RenderBlocks} as an {@code IBlockAccess}, and every ISBRH that looked at its
 * neighbours (cables, ducts, foundry channels) found the jar's dummy connectors and drew its arms. The
 * blocks live in Golem Core's {@link dev.golem.core.render.WorldInAJar}, which the block tesselator
 * reads as a level. A modern cable or channel does not look at its neighbours while drawing: it bakes them
 * into its block state when a neighbour changes. So whenever a block or tile in the jar changes, the
 * connection state of that cell and its six neighbours is recomputed here with the same rules the 1.7
 * renderers applied: the one thing a level would have done that a jar has to do itself.
 *
 * <p>Tiles are plain objects, as they effectively were upstream: a {@link Dummies.JarDummyConnector}, or a
 * block entity that is never added to a level and exists only so its renderer has something to read.
 *
 * <p>Light is always full, as upstream ({@code lightlevel = 15}); biome tints are plains, upstream's
 * {@code getBiomeGenForCoords}.
 */
public class WorldInAJar {

    /** Plains grass, foliage and water, the colours 1.7 took from {@code BiomeGenBase.plains}. */
    private static final int PLAINS_GRASS = 0xFF91BD59;
    private static final int PLAINS_FOLIAGE = 0xFF77AB2F;
    private static final int PLAINS_WATER = 0xFF3F76E4;

    public final int sizeX;
    public final int sizeY;
    public final int sizeZ;

    public int lightlevel = 15;

    private dev.golem.core.render.WorldInAJar blocks;
    private Object[] tiles;
    private final Map<Integer, BlockEntity> standIns = new HashMap<>();

    public WorldInAJar(int x, int y, int z) {
        this.sizeX = x;
        this.sizeY = y;
        this.sizeZ = z;
        nuke();
    }

    public void nuke() {
        this.blocks = new dev.golem.core.render.WorldInAJar(sizeX, sizeY, sizeZ);
        this.blocks.tint(PLAINS_GRASS, PLAINS_FOLIAGE, PLAINS_WATER);
        this.tiles = new Object[sizeX * sizeY * sizeZ];
        this.standIns.clear();
    }

    /** The block cells, for the block renderer. */
    public dev.golem.core.render.WorldInAJar blocks() {
        return blocks;
    }

    public BlockState getBlock(int x, int y, int z) {
        return blocks.getBlockState(x, y, z);
    }

    public void setBlock(int x, int y, int z, BlockState state) {
        if (outside(x, y, z)) return;
        blocks.setBlock(x, y, z, state);
        refreshAround(x, y, z);
    }

    public @Nullable Object getTileEntity(int x, int y, int z) {
        if (outside(x, y, z)) return null;
        return tiles[index(x, y, z)];
    }

    public void setTileEntity(int x, int y, int z, @Nullable Object tile) {
        if (outside(x, y, z)) return;
        tiles[index(x, y, z)] = tile;
        refreshAround(x, y, z);
    }

    /**
     * A block entity for a block that the port draws with its block-entity renderer rather than a block
     * model, such as a barrel, standing in the jar without a tile of its own. 1.7 drew such blocks (ISBRHs)
     * in the block pass from the block alone; this lends the renderer the block entity it reads. Null for
     * blocks with a model, or without a block entity.
     */
    public @Nullable BlockEntity standIn(int x, int y, int z) {
        BlockState state = getBlock(x, y, z);
        if (state.isAir() || state.getRenderShape() == RenderShape.MODEL || !(state.getBlock() instanceof EntityBlock entityBlock)) {
            return null;
        }
        int index = index(x, y, z);
        BlockEntity tile = standIns.get(index);
        if (tile == null || tile.getBlockState().getBlock() != state.getBlock()) {
            tile = entityBlock.newBlockEntity(BlockPos.ZERO, state);
            if (tile == null) return null;
            standIns.put(index, tile);
        }
        return tile;
    }

    /** Visits every cell holding a tile. */
    public void forEachTile(TileVisitor visitor) {
        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    Object tile = tiles[index(x, y, z)];
                    if (tile != null) visitor.visit(x, y, z, tile);
                }
            }
        }
    }

    @FunctionalInterface
    public interface TileVisitor {
        void visit(int x, int y, int z, Object tile);
    }

    private boolean outside(int x, int y, int z) {
        return x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ;
    }

    private int index(int x, int y, int z) {
        return (x * sizeY + y) * sizeZ + z;
    }

    /// CONNECTIONS ///

    private void refreshAround(int x, int y, int z) {
        refresh(x, y, z);
        for (Direction dir : Direction.values()) {
            refresh(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
        }
    }

    private void refresh(int x, int y, int z) {
        if (outside(x, y, z)) return;
        BlockState state = blocks.getBlockState(x, y, z);
        BlockState connected = state;

        if (state.getBlock() instanceof BlockCable) {
            // 1.7 RenderBlockCable: an arm wherever the neighbour's tile is an energy connector.
            for (Direction dir : Direction.values()) {
                connected = connected.setValue(BlockCable.PROPERTY_BY_DIRECTION.get(dir),
                        isConnector(getTileEntity(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ()), null));
            }
        } else if (state.getBlock() instanceof BlockFluidDuct) {
            // 1.7 Library.canConnectFluid: a connector, or another pipe carrying the same fluid.
            FluidType type = getTileEntity(x, y, z) instanceof TileEntityFluidDuctNT duct ? duct.getFluidType() : null;
            for (Direction dir : Direction.values()) {
                Object neighbour = getTileEntity(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
                connected = connected.setValue(BlockFluidDuct.PROPERTY_BY_DIRECTION.get(dir), isConnector(neighbour, type));
            }
        } else if (state.getBlock() instanceof FoundryChannel channel) {
            BlockPos pos = new BlockPos(x, y, z);
            int meta = 0;
            if (channel.canConnectTo(blocks, pos, Direction.EAST)) meta |= 1;
            if (channel.canConnectTo(blocks, pos, Direction.WEST)) meta |= 2;
            if (channel.canConnectTo(blocks, pos, Direction.SOUTH)) meta |= 4;
            if (channel.canConnectTo(blocks, pos, Direction.NORTH)) meta |= 8;
            connected = connected.setValue(FoundryChannel.META, meta);
        } else if (state.getBlock() instanceof FoundryTank) {
            for (Direction dir : Direction.values()) {
                BlockState neighbour = getBlock(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
                connected = connected.setValue(FoundryTank.TANK_BY_DIRECTION.get(dir), neighbour.getBlock() instanceof FoundryTank);
                BooleanProperty outlet = FoundryTank.OUTLET_BY_DIRECTION.get(dir);
                if (outlet != null) {
                    connected = connected.setValue(outlet, neighbour.getBlock() instanceof FoundryOutlet
                            && neighbour.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                            && neighbour.getValue(BlockStateProperties.HORIZONTAL_FACING) == dir);
                }
            }
        }

        if (connected != state) blocks.setBlock(x, y, z, connected);
    }

    /**
     * Whether {@code tile} would accept a connection. {@code fluid} is the pipe's type when asking for a
     * fluid connection: a dummy accepts anything, another pipe only the fluid it carries.
     */
    private static boolean isConnector(@Nullable Object tile, @Nullable FluidType fluid) {
        if (tile instanceof Dummies.JarDummyConnector) return true;
        if (fluid != null && tile instanceof TileEntityFluidDuctNT duct) return duct.getFluidType() == fluid;
        return false;
    }

    /** Air, for scripts that clear a cell. */
    public static BlockState air() {
        return Blocks.AIR.defaultBlockState();
    }
}
