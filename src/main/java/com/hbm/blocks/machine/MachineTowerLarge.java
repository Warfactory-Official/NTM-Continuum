package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;

import com.hbm.api.fluidmk2.IFluidConnectorBlockMK2;
import dev.golem.core.multiblock.MultiblockMachine;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.interfaces.ILookOverlay;
import com.hbm.tileentity.machine.TileEntityTowerLarge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

/** 9x9x13 cooling tower, folded from the legacy dummy block. */
public final class MachineTowerLarge extends BlockMultiblockMachine implements IFluidConnectorBlockMK2, ILookOverlay {
    public MachineTowerLarge(Properties properties) { super(properties); }
    @Override protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) { return new TileEntityTowerLarge(pos, state); }
    @Override public int[] getDimensions() { return new int[]{12, 0, 4, 4, 4, 4}; }
    @Override public int getOffset() { return 4; }

    /** The twelve legacy {@code makeExtra} ports sit one block beyond the 9x9 footprint. */
    @Override
    protected void visitCells(BlockPos core, Direction facing, CellVisitor visitor) {
        super.visitCells(core, facing, visitor);
        for (int offset = -3; offset <= 3; offset += 3) {
            visitor.cell(core.offset(4, 0, offset), MASK_EAST);
            visitor.cell(core.offset(-4, 0, offset), MASK_WEST);
            visitor.cell(core.offset(offset, 0, 4), MASK_SOUTH);
            visitor.cell(core.offset(offset, 0, -4), MASK_NORTH);
        }
    }

    @Override public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        if (!(MultiblockSurface.coreEntity(level, pos) instanceof TileEntityTowerLarge tower)) return null;
        List<String> text = new ArrayList<>();
        text.add("§a-> §r" + tower.input.getTankType().getDisplayNameString() + ": " + tower.input.getFill() + "/" + tower.input.getMaxFill() + "mB");
        text.add("§c<- §r" + tower.output.getTankType().getDisplayNameString() + ": " + tower.output.getFill() + "/" + tower.output.getMaxFill() + "mB");
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
