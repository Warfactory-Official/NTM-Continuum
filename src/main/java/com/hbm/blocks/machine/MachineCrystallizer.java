package com.hbm.blocks.machine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityMachineCrystallizer;
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

/**
 * Ore Acidizer (1.12.2 {@code machine_crystallizer}): a 3×3×6 dummyable tower that dissolves ores in
 * acid to produce crystals. Power and fluid connect only at the core's 8 side ports ({@code getConPos});
 * the plus-shaped base is widened to the full 3×3 by {@link #fillSpace} corner extras, like the original.
 */
public class MachineCrystallizer extends BlockMultiblockMachine implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineCrystallizer(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCrystallizer(pos, state);
    }

    /** Any face of the multiblock exposes the core's inventory to hoppers/pipes (input 0, outputs 2/4). */
    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineCrystallizer te ? te : null;
    }

    @Override
    public int[] getDimensions() {
        return new int[]{5, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    /** The declared box fills a plus at ground level; 1.7 widens it to the full 3x3 with four corners. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 1));
            }
        }
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, domains);
            }
        }
    }
}
