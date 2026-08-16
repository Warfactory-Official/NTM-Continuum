package com.hbm.blocks.generic;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that seals radiation ({@link IRadResistantBlock}): the chunk engine's flood‑fill treats it as a
 * wall, so a room walled with these holds its radiation separately. Placing/removing one re‑partitions the
 * section's pockets, so both edges call {@link RadiationSystemNT#markSectionForRebuild}. Ported from
 * 1.12.2's {@code BlockRadResistant}.
 */
public class BlockRadResistant extends dev.golem.core.world.CompatBlock implements IRadResistantBlock {

    public BlockRadResistant(Properties props) {
        super(props);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild((net.minecraft.server.level.ServerLevel) level, pos);
        }
    }

    @Override
    protected void onBlockRemoved(BlockState state, Level level, BlockPos pos, boolean movedByPiston) {
        if (!level.isClientSide()) {
            RadiationSystemNT.markSectionForRebuild((net.minecraft.server.level.ServerLevel) level, pos);
        }
    }
}
