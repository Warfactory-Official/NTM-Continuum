package com.hbm.blocks.machine.solar;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.solar.TileEntitySolarBoiler;
import com.hbm.tileentity.machine.solar.ModSolarBlockEntities;
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

/** The three-high solar boiler dummyable, with only its legacy top and bottom fluid ports. */
public class MachineSolarBoiler extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {

    public MachineSolarBoiler(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySolarBoiler(pos, state);
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
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntitySolarBoiler boiler)) return null;

        List<String> text = new ArrayList<>();
        // 1.7 printHook: both tanks unconditionally, then a blinking "Too cold!" when no operation is possible.
        text.add("§a-> §r" + boiler.tanks[0].getTankType().getDisplayNameString() + ": "
                + boiler.tanks[0].getFill() + "/" + boiler.tanks[0].getMaxFill() + "mB");
        text.add("§c<- §r" + boiler.tanks[1].getTankType().getDisplayNameString() + ": "
                + boiler.tanks[1].getFill() + "/" + boiler.tanks[1].getMaxFill() + "mB");
        if (boiler.display < 1) {
            text.add("&[" + (com.hbm.util.BobMathUtil.getBlink() ? 0xff0000 : 0xffff00) + "&]Too cold!");
        }
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    @Override
    public int coreMask() {
        return MASK_DOWN;
    }

    /** The steam outlet two above the core. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        visitor.cell(core.above(2), MASK_UP);
    }

    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        visitor.passiveCell(core.above(2), MASK_ALL, PASSIVE_FLUID_IN);
    }
}
