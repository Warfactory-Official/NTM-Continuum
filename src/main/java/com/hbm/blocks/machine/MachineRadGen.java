package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineRadGen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 12-channel radiation generator and its three extra dummy cells. */
public class MachineRadGen extends BlockMultiblockMachine implements IEnergyConnectorBlock, WorldlyContainerHolder {

    public MachineRadGen(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineRadGen(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{2, 0, 3, 2, 1, 1};
    }

    @Override
    public int getOffset() {
        return 2;
    }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineRadGen generator
                ? generator : null;
    }

    /** One port, at the back of the hall: 1.7 provides at {@code core - facing*4} facing away. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.relative(facing, -3), MASK_NORTH);
    }

    /**
     * 1.7 stamps three item proxies. Their power arm is inert -- the proxy gates it on the core being a
     * receiver, and this machine only ever provides.
     */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        visitor.passiveCell(core.relative(facing, -3), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.relative(rot), MASK_ALL, PASSIVE_ITEMS);
        visitor.passiveCell(core.relative(rot, -1), MASK_ALL, PASSIVE_ITEMS);
    }
}
