package com.hbm.blocks.machine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.oil.TileEntityMachineCoker;
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

/** Coker block — 1:1 port of the 1.12.2 {@code MachineCoker}: a 3×3×23 heat tower (dims
 * {@code {22,0,1,1,1,1}}, offset 1; a y+1 ring, 4 corner pillars, 4 makeExtra fluid ports). */
public class MachineCoker extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder {

    public MachineCoker(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityMachineCoker(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{22, 0, 1, 1, 1, 1}; }
    @Override public int getOffset() { return 1; }

    @Override
    @Nullable
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        return core != null && level.getBlockEntity(core) instanceof TileEntityMachineCoker te ? te : null;
    }

    /** 1.7: the collar at y+1 and the four corner pillars that carry it. */
    private static final int[] COLLAR = {5, 0, 2, 2, 2, 2};
    private static final int[] PILLAR = {0, 1, 0, 0, 0, 0};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        if (!MultiblockHandlerXR.checkSpace(level, core.above(), COLLAR, clicked, facing)) return false;
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) {
                if (!MultiblockHandlerXR.checkSpace(level, core.offset(dx, 1, dz), PILLAR, clicked, facing)) {
                    return false;
                }
            }
        }
        return true;
    }

    /** Four corner ports at the foot, each open on the two faces its corner turns outward. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        MultiblockHandlerXR.visitBox(core.above(), COLLAR, facing, visitor);
        for (int dx = -2; dx <= 2; dx += 4) {
            for (int dz = -2; dz <= 2; dz += 4) {
                MultiblockHandlerXR.visitBox(core.offset(dx, 1, dz), PILLAR, facing, visitor);
            }
        }
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.cell(core.offset(dx, 0, dz), outwardMask(dx, dz, 1));
            }
        }
    }

    /** 1.7 stamps {@code inventory().fluid()} on the four corners. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;
        for (int dx = -1; dx <= 1; dx += 2) {
            for (int dz = -1; dz <= 1; dz += 2) {
                visitor.passiveCell(core.offset(dx, 0, dz), MASK_ALL, domains);
            }
        }
    }
}
