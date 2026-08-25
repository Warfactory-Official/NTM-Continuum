package com.hbm.blocks.bomb;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IBomb;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.bomb.TileEntityLaunchPad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Silo Launch Pad: a 3×3 flat dummyable pad (core in the centre, surrounding pad blocks).
 *
 * <p>As an {@link IBomb} it is "detonated" by launching: a detonator aimed at any block of the pad
 * fires the missile at whatever the installed designator points to. Redstone does the same through
 * {@link #onNeighborChanged} — every block of the multiblock reports its own powered state to the core,
 * which counts them so one lever produces one launch rather than nine.
 *
 * <p><b>Cables and ducts attach at the four corner cells only</b>, one block further out — the eight
 * positions in {@link com.hbm.tileentity.bomb.TileEntityLaunchPad#getConPos()}. That is upstream's own
 * geometry: in 1.12.2 the corners were the cells {@code makeExtra} flagged, and only those carried a
 * {@code TileEntityProxyCombo} forwarding power, fluid and inventory to the core. The port has no
 * proxies — a cable asks the neighbouring <em>block</em> whether that face is a port — so the pad has to
 * answer through {@link IEnergyConnectorBlock}/{@link IFluidConnectorBlockMK2}. Without them the pad
 * looks like a plain block to a cable and nothing connects.
 */
public class LaunchPad extends BlockDummyable implements IBomb, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public LaunchPad(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLaunchPad(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.LAUNCH_PAD.get(), TileEntityLaunchPad::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    /** Cables connect only at the pad's true power ports (the core's {@code getConPos} positions). */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityLaunchPad pad
                && IEnergyConnectorBlock.isPort(pad.getConPos(), pos, dir);
    }

    /** Fuel and oxidiser ducts use the same eight ports — the geometry is fluid-agnostic. */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntityLaunchPad pad
                && IEnergyConnectorBlock.isPort(pad.getConPos(), pos, dir);
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, @Nullable Entity detonator) {
        if (level.isClientSide()) return BombReturnCode.UNDEFINED;

        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPad pad) {
            pad.detonator = detonator instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
            return pad.launchFromDesignator();
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    protected void onNeighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                     boolean movedByPiston) {
        super.onNeighborChanged(state, level, pos, neighborBlock, movedByPiston);
        if (level.isClientSide()) return;

        BlockPos core = findCore(level, pos);
        if (core != null && level.getBlockEntity(core) instanceof TileEntityLaunchPad pad) {
            pad.updateRedstonePower(level, pos);
        }
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /**
     * Flags the four corner cells as "extra" after the 3x3 shell is built.
     *
     * <p>Overrides {@code formStructure}, not {@code fillSpace}: {@code fillSpace} is only the placement
     * path, so flagging there leaves a pad built by any other route (struct assembly, the layout
     * GameTests) with unflagged corners. This is the hook both entry points share.
     */
    @Override
    public void formStructure(Level world, BlockPos core, ForgeDirection dir) {
        super.formStructure(world, core, dir);

        int x = core.getX(), y = core.getY(), z = core.getZ();
        this.makeExtra(world, x + 1, y, z + 1);
        this.makeExtra(world, x + 1, y, z - 1);
        this.makeExtra(world, x - 1, y, z + 1);
        this.makeExtra(world, x - 1, y, z - 1);
    }
}
