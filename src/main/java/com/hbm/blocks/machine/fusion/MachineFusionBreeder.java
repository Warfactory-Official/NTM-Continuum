package com.hbm.blocks.machine.fusion;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBreeder;
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

import java.util.List;

/** The fusion breeder's 5 x 3 x 5 dummyable footprint. */
public class MachineFusionBreeder extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, WorldlyContainerHolder, ILookOverlay {

    public MachineFusionBreeder(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionBreeder(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 2, 2, 1, 1}; }
    @Override public int getOffset() { return 2; }

    @Override
    public @Nullable WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        if (state.getValue(CELL) && !MultiblockSurface.carriesPassive(level, pos, PASSIVE_ITEMS)) return null;
        return MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFusionBreeder breeder ? breeder : null;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFusionBreeder breeder)) return null;
        List<String> text = new java.util.ArrayList<>();
        text.add("§a-> §r" + String.format(java.util.Locale.US, "%,.0f", breeder.neutronEnergy) + " flux/t");
        for (int i = 0; i < breeder.tanks.length; i++) {
            text.add((i == 0 ? "§a-> §r" : "§c<- §r") + breeder.tanks[i].getTankType().getDisplayNameString()
                    + ": " + String.format(java.util.Locale.US, "%,d", breeder.tanks[i].getFill())
                    + "/" + String.format(java.util.Locale.US, "%,d", breeder.tanks[i].getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** The deck outlet above the nose, and four flank ports one step inward on either side. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(rx, 0, rz), MASK_WEST);
        visitor.cell(core.offset(-rx, 0, -rz), MASK_EAST);
        visitor.cell(core.offset(dx + rx, 0, dz + rz), MASK_WEST);
        visitor.cell(core.offset(dx - rx, 0, dz - rz), MASK_EAST);
        visitor.cell(core.offset(dx * 2, 2, dz * 2), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_FLUID_IN | PASSIVE_ITEMS;

        visitor.passiveCell(core.offset(rx, 0, rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(-rx, 0, -rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx + rx, 0, dz + rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx - rx, 0, dz - rz), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 2, 2, dz * 2), MASK_ALL, domains);
    }
}
