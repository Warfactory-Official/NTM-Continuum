package com.hbm.blocks.machine;

import com.hbm.blocks.multiblock.BlockMultiblockMachine;
import com.hbm.tileentity.machine.oil.TileEntitySpacer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fractioning Tower Separator ({@code fraction_spacer}) — 1:1 port of the 1.12.2 {@code FractionSpacer}:
 * a plain 1-tall 3×3 the folded core (dims {@code {0,0,1,1,1,1}}, offset 1) with a logic-less
 * {@link TileEntitySpacer} core (render anchor only). Placed between fraction towers to raise the stack.
 */
public class FractionSpacer extends BlockMultiblockMachine {

    public FractionSpacer(Properties properties) {
        super(properties);
    }

    @Override
    protected BlockEntity createCoreBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySpacer(pos, state);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }
}
