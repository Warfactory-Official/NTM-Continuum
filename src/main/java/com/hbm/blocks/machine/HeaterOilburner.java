package com.hbm.blocks.machine;

import com.hbm.lib.ForgeDirection;
import com.hbm.blocks.ITooltipProvider;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityHeaterOilburner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluid Burner ({@code heater_oilburner}), a 1:1 port of the 1.12.2 {@code HeaterOilburner}: a 2-tall
 * 3×3 the folded core (dims {@code {1,0,1,1,1,1}}, offset 1) with the 4 side cells and the
 * top-center marked as extras (fluid ports + the heat cell the consumer above polls through). Look
 * overlay shows setting, tank and stored heat. IToolable (screwdriver setting) is replaced by GUI
 * buttons. This is a documented deviation.
 */
public class HeaterOilburner extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay, ITooltipProvider {

    public HeaterOilburner(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHeaterOilburner(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{1, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityHeaterOilburner heater)) return null;

        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + heater.setting + " mB/t");
        text.add(heater.tank.getTankType().getDisplayNameString() + ": " + String.format("%,d", heater.tank.getFill()) + "/" + String.format("%,d", heater.tank.getMaxFill()) + " mB");
        text.add(String.format("%,d", heater.heatEnergy) + " TU");
        return new ILookOverlay.Data(this.getName().getString(), 0xffff00, 0x404000, text);
    }

    /** Four fluid inlets around the waist; the cell above the core is body, not port. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);

        visitor.cell(core.offset(1, 0, 0), MASK_EAST);
        visitor.cell(core.offset(-1, 0, 0), MASK_WEST);
        visitor.cell(core.offset(0, 0, 1), MASK_SOUTH);
        visitor.cell(core.offset(0, 0, -1), MASK_NORTH);
        visitor.cell(core.offset(0, 1, 0), MASK_NONE);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.offset(1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(-1, 0, 0), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, 1), MASK_ALL, PASSIVE_FLUID_IN);
        visitor.passiveCell(core.offset(0, 0, -1), MASK_ALL, PASSIVE_FLUID_IN);
    }

    @Override
    public int[][] getAllPorts(ForgeDirection dir) {
        return rotatePorts(new int[][] {
                {1, 0, 0, ForgeDirection.EAST.ordinal()},
                {-1, 0, 0, ForgeDirection.WEST.ordinal()},
                {0, 0, 1, ForgeDirection.SOUTH.ordinal()},
                {0, 0, -1, ForgeDirection.NORTH.ordinal()},
        }, dir);
    }

    @Override
    public double[][] getAABBExtras() {
        return new double[][] {
                {2, 2, 0.5, -0.3, 0.4, -0.4, 1},
        };
    }
}
