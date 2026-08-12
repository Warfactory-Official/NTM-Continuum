package com.hbm.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A do-nothing block entity so the OBJ-rendered {@link com.hbm.blocks.machine.NTMAnvil} can carry
 * {@link com.hbm.client.render.AnvilObjRenderer} — the same invisible-block + BER pattern the machines
 * and deco blocks use (the 1.12.2 anvils were {@code forge_marker} OBJ blockstates, which 26.1 lacks).
 */
public class TileEntityAnvil extends BlockEntity {

    public TileEntityAnvil(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ANVIL.get(), pos, state);
    }
}
