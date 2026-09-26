package com.hbm.hazard.type;

import com.hbm.api.hazard.IHazardType;
import java.util.List;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModDecoBlocks;
import com.hbm.blocks.ModOutgasBlocks;
import com.hbm.config.RadiationConfig;
import com.hbm.explosion.ExplosionNukeGeneric;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.api.hazard.IHazardModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Dropped radioactive material contaminates the ground around it (chunk radiation + waste transform).
 */
public class HazardTypeContaminating implements IHazardType {

    private static final int MAX_RADIUS = 500;

    private static int computeRadius(double level) {
        return (int) Math.min(Math.sqrt(level) + 0.5D, MAX_RADIUS);
    }

    @Override
    public void onUpdate(LivingEntity target, double level, ItemStack stack) {
    }

    @Override
    public void updateEntity(ItemEntity item, double level) {
        if (!RadiationConfig.enableContaminationOnGround) return;
        if (item == null) return;

        Level world = item.level();
        if (world.isClientSide()) return;

        if (item.onGround()) {
            BlockPos pos = item.blockPosition();
            BlockPos down = pos.below();
            Block block = world.getBlockState(down).getBlock();
            if (isCleanBlock(block)) {
                getUsed(block, down, world);
                return;
            }

            int radius = computeRadius(level);
            if (radius > 1) {
                // The fallout rain has no biome change for this path, so keep the old direct radiation pulse.
                ChunkRadiationManager.proxy.incrementRad(world, pos, level);
                ExplosionNukeGeneric.transformWaste(world, pos, radius);
            }
            item.discard();
        }
    }

    @Override
    public void addHazardInformation(Player player, List<Component> list, double level, ItemStack stack, List<IHazardModifier> modifiers) {
        if (!RadiationConfig.enableContaminationOnGround) return;
        int radius = computeRadius(level);
        if (radius > 1) {
            list.add(Component.literal("[").append(Component.translatable("trait.contaminating")).append(Component.literal("]")).withStyle(ChatFormatting.DARK_GREEN));
            list.add(Component.literal(" ").append(Component.translatable("trait.contaminating.radius", radius)).withStyle(ChatFormatting.GREEN));
        }
    }

    /** The port has no BlockClean base class; keep the old clean-room short-circuit by identity. */
    private static boolean isCleanBlock(Block block) {
        return block == ModBlocks.tile_lab.get()
                || block == ModBlocks.tile_lab_cracked.get()
                || block == ModDecoBlocks.deco_rbmk.get()
                || block == ModDecoBlocks.deco_rbmk_smooth.get();
    }

    private static void getUsed(Block block, BlockPos pos, Level world) {
        if (block == ModBlocks.tile_lab.get() && world.getRandom().nextInt(2000) == 0) {
            world.setBlock(pos, ModBlocks.tile_lab_cracked.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (block == ModBlocks.tile_lab_cracked.get() && world.getRandom().nextInt(10000) == 0) {
            world.setBlock(pos, ModOutgasBlocks.tile_lab_broken.get().defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}
