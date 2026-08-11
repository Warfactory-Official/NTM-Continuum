package com.hbm.mixin;

import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes {@link FireBlock}'s private burn-odds lookup so the fallout sweep can test flammability
 * exactly like 1.12's {@code Block.isFlammable} (backed by the same fire-info table). Use via
 * {@code ((FireBlockAccessor) Blocks.FIRE).hbm$getBurnOdds(state) > 0}.
 */
@Mixin(FireBlock.class)
public interface FireBlockAccessor {

    @Invoker("getBurnOdds")
    int hbm$getBurnOdds(BlockState state);

    /**
     * The other half of the same table — 1.12's {@code Block.getFireSpreadSpeed}, which balefire's
     * area sweep reads to decide whether an air cell has anything nearby worth igniting.
     */
    @Invoker("getIgniteOdds")
    int hbm$getIgniteOdds(BlockState state);
}
