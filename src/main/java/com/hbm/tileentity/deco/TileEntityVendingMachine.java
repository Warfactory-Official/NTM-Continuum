package com.hbm.tileentity.deco;

import com.hbm.tileentity.ModDecoCrateBlockEntities;
import dev.golem.core.data.CompatBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 1.7 {@code BlockVendingMachine.TileEntityVendingMachine}: no state, only the renderer's anchor on the core. */
public class TileEntityVendingMachine extends CompatBlockEntity {

    public TileEntityVendingMachine(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.VENDING_MACHINE.get(), pos, state);
    }
}
