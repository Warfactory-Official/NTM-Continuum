package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.MultiblockHandlerXR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModAutogenItems;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityWatz;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * One segment of the Watz power plant — a cross whose arms reach three blocks out on each horizontal
 * axis, three blocks tall. Segments stack every three blocks; the bottom one runs the simulation for
 * the whole column (see {@link TileEntityWatz}).
 *
 * <p>The footprint is not a box, so it is built from five overlapping {@link MultiblockHandlerXR}
 * passes — the base dimensions plus four with negative extents that carve the arms — exactly as
 * upstream. Nine cells are promoted to ports: the four arm tips at core level and at +2, plus the
 * centre-top cell.
 *
 * <p>Unlike most machines this block has no item: it only comes into being by building the shape
 * around a {@code struct_watz_core} and letting it assemble.
 */
public class Watz extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    /** Suppresses the component drop while the reactor is tearing itself apart in a meltdown. */
    public static boolean drop = true;

    public Watz(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWatz(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 3, 3, 1, 1}; }

    @Override public int getOffset() { return 3; }

    /** The four extra passes, with the negative extents that turn the box into a cross. */
    private static final int[][] ARMS = {
            {2, 0, 2, 2, 2, -2},
            {2, 0, 2, 2, -2, 2},
            {2, 0, 1, 1, 3, -3},
            {2, 0, 1, 1, -3, 3},
    };

    @Override
    public boolean checkRequirement(Level level, int x, int y, int z, ForgeDirection dir, int o) {
        if (!super.checkRequirement(level, x, y, z, dir, o)) return false;
        int cx = x + dir.offsetX * o, cy = y + dir.offsetY * o, cz = z + dir.offsetZ * o;
        for (int[] arm : ARMS) {
            if (!MultiblockHandlerXR.checkSpace(level, cx, cy, cz, arm, x, y, z, dir)) return false;
        }
        return true;
    }

    @Override
    public void formStructure(Level level, BlockPos core, ForgeDirection dir) {
        int cx = core.getX(), cy = core.getY(), cz = core.getZ();
        MultiblockHandlerXR.fillSpace(level, cx, cy, cz, getDimensions(), this, dir);
        for (int[] arm : ARMS) MultiblockHandlerXR.fillSpace(level, cx, cy, cz, arm, this, dir);

        // Upstream declares the ports in world axes rather than rotated ones; the Watz is symmetric
        // about both horizontal axes, so facing makes no difference to where they land.
        makeExtra(level, cx + 2, cy, cz);
        makeExtra(level, cx - 2, cy, cz);
        makeExtra(level, cx, cy, cz + 2);
        makeExtra(level, cx, cy, cz - 2);
        makeExtra(level, cx + 2, cy + 2, cz);
        makeExtra(level, cx - 2, cy + 2, cz);
        makeExtra(level, cx, cy + 2, cz + 2);
        makeExtra(level, cx, cy + 2, cz - 2);
        makeExtra(level, cx, cy + 2, cz);
    }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(META) < extra) return null;
        return findCoreTE(level, pos) instanceof TileEntityWatz watz ? watz : null;
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection direction) {
        BlockPos cell = new BlockPos(x, y, z);
        BlockState state = world.getBlockState(cell);
        if (state.getBlock() != this || state.getValue(META) < extra || state.getValue(META) >= offset + 2) return false;
        BlockPos outside = cell.offset(direction.offsetX, direction.offsetY, direction.offsetZ);
        return findCoreTE(world, cell) instanceof TileEntityWatz watz
                && watz.getConPos().stream().anyMatch(port -> port.getPos().equals(outside) && port.getDir() == direction);
    }

    /**
     * Upstream's hand-rolled drop table: breaking the core returns the whole build. It is code rather
     * than a loot table because the reactor is assembled in world and has no block item of its own.
     */
    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide() && !BlockDummyable.safeRem && state.getValue(META) >= 12 && drop) {
            drop(level, pos, new ItemStack(ModMachineBlocks.watz_casing_tooled.get(), 48));
            for (int i = 0; i < 3; i++) {
                drop(level, pos, new ItemStack(ModAutogenItems.get(MaterialShapes.BOLT, Mats.MAT_DURA), 64));
            }
            drop(level, pos, new ItemStack(com.hbm.blocks.ModBlocks.watz_element.get(), 36));
            drop(level, pos, new ItemStack(com.hbm.blocks.ModBlocks.watz_cooler.get(), 26));
            drop(level, pos, new ItemStack(ModMachineBlocks.struct_watz_core.get(), 1));
        }
        super.onBlockRemoved(state, level, pos, movedByPiston);
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.WATZ.get(), TileEntityWatz::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
