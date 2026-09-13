package com.hbm.blocks.machine.fusion;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionBoiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Fusion boiler - a plasma-fired drum that turns water straight into superheated steam. Behaviour
 * lives in {@link TileEntityFusionBoiler}.
 */
public class MachineFusionBoiler extends BlockMultiblockMachine implements ILookOverlay, IFluidConnectorBlockMK2 {

    public MachineFusionBoiler(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionBoiler(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{3, 0, 4, 4, 1, 1}; }

    @Override public int getOffset() { return 4; }

    /**
     * The look-at readout: what the reactor is feeding this drum, and how full both tanks are. Without
     * it the boiler is completely opaque - it has no GUI of its own.
     */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFusionBoiler boiler)) return null;

        java.util.List<String> text = new java.util.ArrayList<>();
        text.add("§a-> §r" + String.format(java.util.Locale.US, "%,d", boiler.plasmaEnergy) + " TU");
        com.hbm.inventory.fluid.tank.FluidTankNTM[] tanks = boiler.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            text.add((i == 0 ? "§a-> §r" : "§c<- §r") + tanks[i].getTankType().getDisplayNameString()
                    + ": " + String.format(java.util.Locale.US, "%,d", tanks[i].getFill())
                    + "/" + String.format(java.util.Locale.US, "%,d", tanks[i].getMaxFill()) + " mB");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /**
     * Four water/steam ports, paired either side of the drum at both ends. 1.7's getConPos names the
     * neighbour two out along the rotation axis, so each cell turns exactly the face it sits on.
     */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.cell(core.offset(-dx + rx, 0, -dz + rz), MASK_WEST);
        visitor.cell(core.offset(-dx - rx, 0, -dz - rz), MASK_EAST);
        visitor.cell(core.offset(dx * 2 + rx, 0, dz * 2 + rz), MASK_WEST);
        visitor.cell(core.offset(dx * 2 - rx, 0, dz * 2 - rz), MASK_EAST);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();

        visitor.passiveCell(core.offset(-dx + rx, 0, -dz + rz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-dx - rx, 0, -dz - rz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(dx * 2 + rx, 0, dz * 2 + rz), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(dx * 2 - rx, 0, dz * 2 - rz), MASK_ALL, PASSIVE_FLUID_IN);
    }
}
