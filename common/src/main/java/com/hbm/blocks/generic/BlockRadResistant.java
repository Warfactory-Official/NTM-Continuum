package com.hbm.blocks.generic;

import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.interfaces.IRadResistantBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block that seals radiation ({@link IRadResistantBlock}): the chunk engine's flood‑fill treats it as a
 * wall, so a room walled with these holds its radiation separately. Placing/removing one re‑partitions the
 * section's pockets, so both edges call {@link RadiationSystemNT#markSectionForRebuild}. Ported from
 * 1.12.2's {@code BlockRadResistant}.
 */
public class BlockRadResistant extends Block implements IRadResistantBlock {

    public BlockRadResistant(Properties props) {
        super(props);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !state.is(oldState.getBlock())) {
            RadiationSystemNT.markSectionForRebuild(server, pos);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        RadiationSystemNT.markSectionForRebuild(level, pos);
    }
}
