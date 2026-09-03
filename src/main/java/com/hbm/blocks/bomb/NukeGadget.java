package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeGadget;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The Gadget, with four implosion lenses around its wiring and plutonium core. */
public class NukeGadget extends NukeBombBlock {

    public NukeGadget(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeGadget newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeGadget(pos, state);
    }

    @Override
    protected int radiusFor(TileEntityNukeBomb bomb) {
        return BombConfig.gadgetRadius;
    }
}
