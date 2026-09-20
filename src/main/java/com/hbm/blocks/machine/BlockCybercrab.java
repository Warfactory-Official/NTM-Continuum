package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityCyberCrab;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockCybercrab}: the meteor nest that {@link TileEntityCyberCrab} restocks.
 *
 * <p>Non-directional - upstream registers no facing and picks its faces by side alone, top and bottom
 * differing from the four sides - so no {@code FACING} property and no {@link BlockMachine}. It drops
 * nothing when broken ({@code getItemDropped} returns null upstream), which is the loot table's job
 * here rather than the block's.
 */
public class BlockCybercrab extends CompatBlock implements TickingBlock {

    public BlockCybercrab(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCyberCrab(pos, state);
    }
}
