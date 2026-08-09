package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineVacuumDistill;
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

/** Vacuum Distiller block — 3×3×9 {@link BlockDummyable} (dims {@code {8,0,1,1,1,1}}, offset 1) with the
 * 4 base corners marked as extras (the fluid ports). Mirrors {@link MachineRefinery}. */
public class MachineVacuumDistill extends BlockDummyable implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineVacuumDistill(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineVacuumDistill(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{8, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    protected void fillSpace(Level world, int x, int y, int z, ForgeDirection dir, int o) {
        super.fillSpace(world, x, y, z, dir, o);
        this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ + 1);
        this.makeExtra(world, x - dir.offsetX + 1, y, z - dir.offsetZ - 1);
        this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ + 1);
        this.makeExtra(world, x - dir.offsetX - 1, y, z - dir.offsetZ - 1);
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = findCore(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineVacuumDistill te ? te : null;
    }

    @Override
    public boolean canConnect(BlockGetter world, BlockPos pos, ForgeDirection dir) {
        return findCoreTE(world, pos) instanceof TileEntityMachineVacuumDistill te
                && IEnergyConnectorBlock.isPort(te.getConPos(), pos, dir);
    }

    @Override
    public boolean canConnect(FluidType type, BlockGetter world, int x, int y, int z, ForgeDirection dir) {
        return findCoreTE(world, new BlockPos(x, y, z)) instanceof TileEntityMachineVacuumDistill te
                && IEnergyConnectorBlock.isPort(te.getConPos(), new BlockPos(x, y, z), dir);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_VACUUM_DISTILL.get(), TileEntityMachineVacuumDistill::serverTick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }
}
