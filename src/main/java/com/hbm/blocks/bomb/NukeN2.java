package com.hbm.blocks.bomb;

import com.hbm.config.BombConfig;
import com.hbm.tileentity.bomb.TileEntityNukeBomb;
import com.hbm.tileentity.bomb.TileEntityNukeN2;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** N² mine: each of the twelve one-charge slots contributes one twelfth of the configured radius. */
public class NukeN2 extends NukeBombBlock {

    public NukeN2(Properties properties) {
        super(properties);
    }

    @Override
    public TileEntityNukeN2 newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityNukeN2(pos, state);
    }

    @Override
    protected int radiusFor(TileEntityNukeBomb bomb) {
        return radiusForCharges(((TileEntityNukeN2) bomb).countCharges());
    }

    @Override
    protected boolean noRadiation() {
        return true;
    }

    /** Exposed for the exact arithmetic GameTest and for tooltip/render diagnostics. */
    public static int radiusForCharges(int charges) {
        return (int) (BombConfig.n2Radius * charges / 12F);
    }
}
