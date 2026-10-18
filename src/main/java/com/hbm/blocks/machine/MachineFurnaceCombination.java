package com.hbm.blocks.machine;

import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityFurnaceCombination;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The combination furnace: a 2x2x2 dummyable heat furnace with fluid output ports. */
public class MachineFurnaceCombination extends BlockMultiblockMachine
        implements IFluidConnectorBlockMK2, WorldlyContainerHolder, ITooltipProvider {

    public MachineFurnaceCombination(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFurnaceCombination(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{1, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityFurnaceCombination furnace
                ? furnace : null;
    }

    /** 1.7's comboven ports: every perimeter cell turns its own outward faces, at both courses. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dy = 0; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dy == 0 && dx == 0 && dz == 0) continue;
                    int mask = outwardMask(dx, dz, 1);
                    visitor.cell(core.offset(dx, dy, dz), mask);
                }
            }
        }
    }
}
