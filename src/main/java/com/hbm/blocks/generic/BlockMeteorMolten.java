package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import dev.golem.core.world.CompatBlock;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * 1.7 {@code BlockOre} in its {@code block_meteor_molten} branch: the still-glowing cobble a fresh
 * meteorite leaves behind. Sets whatever walks on it on fire (5 s), cools into meteorite cobblestone on
 * its random tick with a fizz, drops nothing, and a player who mines it gets a lava source instead
 * ({@code onBlockDestroyedByPlayer}).
 */
public class BlockMeteorMolten extends CompatBlock {

    public BlockMeteorMolten(Properties properties) {
        super(properties);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 1.7 BlockOre: getItemDropped is null, but 1.7 harvestBlock checks silk touch before getDrops, and this is a plain cube without a tile entity, so silk touch keeps it.
        return OreDrops.silkTouch(params) ? List.of(new ItemStack(this)) : List.of();
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        Effects.ignite(entity, 5);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlockAndUpdate(pos, ModBlocks.block_meteor_cobble.get().defaultBlockState());
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS, 0.5F, 2.6F + (random.nextFloat() - random.nextFloat()) * 0.8F);
    }

    /** 1.7 {@code onBlockDestroyedByPlayer}: the hole fills with lava. Runs after vanilla removes the block. */
    @Override
    protected void onPlayerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server) {
            server.getServer().execute(() -> {
                if (server.getBlockState(pos).isAir()) server.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
            });
        }
    }
}
