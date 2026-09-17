package com.hbm.blocks.machine.fusion;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockHandlerXR;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.fusion.TileEntityFusionMHDT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Magnetohydrodynamic turbine - the fusion chain's electrical output, the longest machine in the set
 * at fourteen blocks along its axis. Behaviour lives in {@link TileEntityFusionMHDT}.
 */
public class MachineFusionMHDT extends BlockMultiblockMachine implements ILookOverlay, IEnergyConnectorBlock, IFluidConnectorBlockMK2 {

    public MachineFusionMHDT(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityFusionMHDT(pos, state);
    }

    @Override public int[] getDimensions() { return new int[]{2, 0, 6, 7, 2, 2}; }

    @Override public int getOffset() { return 7; }

    /**
     * The look-at readout. Beyond the throughput it carries the two warnings that explain a turbine
     * producing nothing: plasma below the threshold (half output), and no coolant (no output at all).
     * Both blink, as upstream.
     */
    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityFusionMHDT turbine)) return null;

        boolean hasPlasma = turbine.hasMinimumPlasma();
        boolean isCool = turbine.isCool();
        long power = (long) Math.floor(turbine.plasmaEnergy * TileEntityFusionMHDT.PLASMA_EFFICIENCY);
        if (!hasPlasma) power /= 2;

        java.util.List<String> text = new java.util.ArrayList<>();
        text.add("§a-> " + (hasPlasma ? "§r" : "§6")
                + String.format(java.util.Locale.US, "%,d", turbine.plasmaEnergy) + " TU/t / "
                + String.format(java.util.Locale.US, "%,d", TileEntityFusionMHDT.MINIMUM_PLASMA) + " TU/t");
        text.add("§c<- §r" + String.format(java.util.Locale.US, "%,d", isCool ? power : 0L) + " HE/t");

        com.hbm.inventory.fluid.tank.FluidTankNTM[] tanks = turbine.getAllTanks();
        for (int i = 0; i < tanks.length; i++) {
            text.add((i == 0 ? "§a-> §r" : "§c<- §r") + tanks[i].getTankType().getDisplayNameString()
                    + ": " + String.format(java.util.Locale.US, "%,d", tanks[i].getFill())
                    + "/" + String.format(java.util.Locale.US, "%,d", tanks[i].getMaxFill()) + " mB");
        }

        boolean blink = (System.currentTimeMillis() / 500L) % 2L == 0L;
        if (turbine.plasmaEnergy > 0 && !hasPlasma) {
            text.add("&[" + (blink ? 0xff8000 : 0xffff00) + "&]! LOW POWER !");
        }
        if (!isCool) {
            text.add("&[" + (blink ? 0xff0000 : 0xffff00) + "&]! ! ! INSUFFICIENT COOLING ! ! !");
        }
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /** 1.7: the clearance boxes beyond getDimensions. */
    private static final int[][] EXTRA_BOXES = {
            {3, -2, 6, 2, 1, 1},
            {3, -2, -6, 7, 1, 1},
            {3, -2, -3, 5, 2, 2},
            {4, -3, -3, 5, 1, 1}};

    /** The intake cowl sits three blocks further down the axis than the rest. */
    private static final int[] NOZZLE_BOX = {1, 0, 0, 1, 3, 3};

    @Override
    protected boolean checkRequirement(Level level, BlockPos core, Direction facing, BlockPos clicked) {
        if (!super.checkRequirement(level, core, facing, clicked)) return false;
        for (int[] box : EXTRA_BOXES) {
            if (!MultiblockHandlerXR.checkSpace(level, core, box, clicked, facing)) return false;
        }
        return MultiblockHandlerXR.checkSpace(level, nozzle(core, facing), NOZZLE_BOX, clicked, facing);
    }

    private static BlockPos nozzle(BlockPos core, Direction facing) {
        return core.offset(facing.getStepX() * 3, 0, facing.getStepZ() * 3);
    }

    /** Two coolant laterals four out and three across, and the exhaust mouth at the end of the duct. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int[] box : EXTRA_BOXES) MultiblockHandlerXR.visitBox(core, box, facing, visitor);
        MultiblockHandlerXR.visitBox(nozzle(core, facing), NOZZLE_BOX, facing, visitor);

        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        visitor.cell(core.offset(dx * 4 + rx * 3, 0, dz * 4 + rz * 3), MASK_WEST);
        visitor.cell(core.offset(dx * 4 - rx * 3, 0, dz * 4 - rz * 3), MASK_EAST);
        visitor.cell(core.offset(dx * 7, 1, dz * 7), MASK_SOUTH);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction rot = facing.getClockWise();
        int dx = facing.getStepX(), dz = facing.getStepZ();
        int rx = rot.getStepX(), rz = rot.getStepZ();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.offset(dx * 4 + rx * 3, 0, dz * 4 + rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 4 - rx * 3, 0, dz * 4 - rz * 3), MASK_ALL, domains);
        visitor.passiveCell(core.offset(dx * 7, 1, dz * 7), MASK_ALL, domains);
    }
}
