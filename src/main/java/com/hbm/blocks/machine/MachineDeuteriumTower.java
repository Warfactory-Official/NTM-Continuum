package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityDeuteriumTower;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Ten-block deuterium tower with an irregular 3x3 base and eight network ports. */
public class MachineDeuteriumTower extends BlockMultiblockMachine
        implements IEnergyConnectorBlock, IFluidConnectorBlockMK2, ILookOverlay {

    public MachineDeuteriumTower(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityDeuteriumTower(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{9, 0, 1, 0, 0, 1};
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityDeuteriumTower tower)) return null;

        List<String> text = new ArrayList<>();
        text.add(Library.getShortNumber(tower.power) + "/" + Library.getShortNumber(tower.getMaxPower()) + " HE");
        text.add("§a-> §r" + tower.tanks[0].getTankType().getDisplayNameString() + ": "
                + tower.tanks[0].getFill() + "/" + tower.tanks[0].getMaxFill() + "mB");
        text.add("§c<- §r" + tower.tanks[1].getTankType().getDisplayNameString() + ": "
                + tower.tanks[1].getFill() + "/" + tower.tanks[1].getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }

    /** The four cells of the 2x2 base, each open on the two faces its corner turns outward. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        Direction back = facing.getOpposite();
        Direction rot = facing.getCounterClockWise();
        visitor.cell(core.relative(back), MASK_NORTH | MASK_WEST);
        visitor.cell(core.relative(rot), MASK_SOUTH | MASK_EAST);
        visitor.cell(core.relative(back).relative(rot), MASK_NORTH | MASK_EAST);
    }

    @Override public int coreMask() { return MASK_SOUTH | MASK_WEST; }

    /** 1.7 stamps {@code TileEntityProxyCombo(false, true, true)} -- fluid and power, no inventory. */
    @Override
    protected void visitPassiveCells(BlockPos core, Direction facing, PassiveCellVisitor visitor) {
        Direction back = facing.getOpposite();
        Direction rot = facing.getCounterClockWise();
        int domains = PASSIVE_POWER_IN | PASSIVE_FLUID_IN;
        visitor.passiveCell(core.relative(back), MASK_ALL, domains);
        visitor.passiveCell(core.relative(rot), MASK_ALL, domains);
        visitor.passiveCell(core.relative(back).relative(rot), MASK_ALL, domains);
    }
}
