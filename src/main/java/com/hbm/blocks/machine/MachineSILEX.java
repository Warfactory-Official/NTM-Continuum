package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntitySILEX;
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
 * Laser Isotope Separation Chamber, "SILEX" (1.12.2 {@code machine_silex}): a 3×3×3 dummyable chamber
 * that dissolves isotope-bearing material and lets a {@code machine_fel}'s beam sort it.
 *
 * <p>It takes <b>no power of its own</b> — all the energy is in the laser. What it does need is
 * peroxide, which arrives through the two ports {@link #fillSpace} adds at {@code y+1} on either side,
 * perpendicular to the machine's facing. Those two extras are the only fluid connection points.
 */
public class MachineSILEX extends BlockDummyable implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineSILEX(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySILEX(pos, state);
    }

    /** Any face exposes the core inventory: insert into slot 0, extract from the 5–10 queue. */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntitySILEX te ? te : null;
    }

    /** Ducts connect only at the two side ports the core reports (the {@code y+1} extras). */
    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        BlockPos pos = new BlockPos(x, y, z);
        return findCoreTE(world, pos) instanceof TileEntitySILEX te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_SILEX.get(), TileEntitySILEX::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);

        // Two "extra" dummies at y+1, one on each side ACROSS the facing axis — the fluid inlets.
        if (dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH) {
            this.makeExtra(world, x + dir.offsetX * o + 1, y + 1, z + dir.offsetZ * o);
            this.makeExtra(world, x + dir.offsetX * o - 1, y + 1, z + dir.offsetZ * o);
        }

        if (dir == ForgeDirection.EAST || dir == ForgeDirection.WEST) {
            this.makeExtra(world, x + dir.offsetX * o, y + 1, z + dir.offsetZ * o + 1);
            this.makeExtra(world, x + dir.offsetX * o, y + 1, z + dir.offsetZ * o - 1);
        }
    }
}
