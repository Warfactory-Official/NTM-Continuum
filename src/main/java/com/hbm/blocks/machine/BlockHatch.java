package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.TileEntityHatch;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code BlockHatch}, the "Silo Hatch": an unbreakable, blast-proof cube ({@code seal_hatch_3}) that only a
 * {@link BlockSeal} places. Its block entity remembers the controller and removes the hatch as soon as that
 * controller or its frame is gone.
 */
public class BlockHatch extends CompatBlock implements TickingBlock {

    public BlockHatch(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityHatch(pos, state);
    }
}
