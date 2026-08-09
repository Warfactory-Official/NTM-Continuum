package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineMiningLaser;
import net.minecraft.core.BlockPos;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Mining Laser (1.12.2 {@code machine_mining_laser}): a ceiling-mounted 3×3×3 dummyable — the core hangs
 * one block below the clicked ceiling face ({@link #getHeightOffset()} = −1) with a 3D plus of dummy
 * cells; {@link #fillSpace} marks the old proxy-arm cells as extras (the port's connector geometry works
 * off {@code getConPos} ports instead of proxy tiles). Power connects at the TOP arm, fluid (crude oil
 * out) at the 4 side arms, and every cell exposes the core's 21 output slots to hoppers.
 */
public class MachineMiningLaser extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineMiningLaser(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineMiningLaser(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 1, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    /** Ceiling mount: the core sits one below the placement position (1.12.2 verbatim). */
    @Override
    public int getHeightOffset() {
        return -1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        // The old block promoted the 4 side arms + the top cell to "extra" cells (its power/item proxy
        // tiles). Kept verbatim for structural parity; connectivity here is port-based (getConPos).
        x += dir.offsetX * o;
        z += dir.offsetZ * o;

        this.makeExtra(world, x + 1, y, z);
        this.makeExtra(world, x - 1, y, z);
        this.makeExtra(world, x, y, z + 1);
        this.makeExtra(world, x, y, z - 1);

        this.makeExtra(world, x, y + 1, z);
    }

    /** Any face of the multiblock exposes the core's inventory to hoppers (extract the 21 output slots). */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineMiningLaser te ? te : null;
    }

    /** Cables connect only at the machine's true power port (the core's {@code getConPos} top arm). */
    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineMiningLaser te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    /** Ducts connect at the same ports (the geometry is fluid-agnostic — reuse the energy helper). */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineMiningLaser te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Server: mining/power/fluid. No client ticker (the beam is interpolated purely in the renderer).
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_MINING_LASER.get(), TileEntityMachineMiningLaser::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

}
