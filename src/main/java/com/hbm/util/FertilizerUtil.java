package com.hbm.util;

import dev.golem.core.world.Plants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code items/tool/ItemFertilizer#fertilize}, the shared growth tick behind powder_fertilizer and the
 * fat pigeon. It uses the current {@link BonemealableBlock} surface directly because NTM's force flag has no
 * {@code BoneMealItem} counterpart.
 */
public final class FertilizerUtil {

    private FertilizerUtil() {
    }

    /**
     * @param force 1.7's force flag: skip the block's own success roll and always grow
     * @return whether the block accepted the growth
     */
    public static boolean fertilize(Level level, BlockPos pos, @Nullable Player player, ItemStack stack,
                                    boolean force) {

        BlockState state = level.getBlockState(pos);

        // ntm-next asks the loader's bonemeal event first; this port has no such bus, so the vanilla path decides.

        if (!(state.getBlock() instanceof BonemealableBlock growable)) return false;
        if (!Plants.isValidBonemealTarget(growable, level, pos, state)) return false;

        if (level instanceof ServerLevel server) {
            if (force || growable.isBonemealSuccess(level, level.getRandom(), pos, state)) {
                growable.performBonemeal(server, level.getRandom(), pos, state);
            }
        }

        return true;
    }

    /**
     * 1.7 {@code playAuxSFX(2005, ...)} - the bonemeal puff, renumbered in 26.1.
     */
    public static void growthParticles(Level level, BlockPos pos) {
        level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 0);
    }
}
