package com.hbm.items.tool;

import dev.golem.core.world.CompatItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

/**
 * The 1.7 {@code ItemModBucket}: a bucket of one of HBM's own fluid blocks. Used like the vanilla
 * bucket — the targeted block if it can be replaced, else the block in front of the face hit — it puts
 * the fluid block down (breaking a non-solid, non-liquid block in the way, with drops) and hands back an
 * empty bucket. The 1.7 nether-water fizzle branch only ever applied to vanilla water and is not here.
 */
public class ItemModBucket extends CompatItem {

    private final Supplier<? extends Block> fluid;

    public ItemModBucket(Properties properties, Supplier<? extends Block> fluid) {
        super(properties);
        this.fluid = fluid;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos pos = blockHit.getBlockPos();
        if (!level.mayInteract(player, pos)) return InteractionResult.FAIL;
        BlockPos target = level.getBlockState(pos).canBeReplaced() ? pos : pos.relative(blockHit.getDirection());
        if (!player.mayUseItemAt(target, blockHit.getDirection(), stack)) return InteractionResult.FAIL;
        if (!tryPlaceContainedLiquid(level, target)) return InteractionResult.FAIL;
        if (!player.isCreative()) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
        return InteractionResult.SUCCESS;
    }

    /** The 1.7 {@code tryPlaceContainedLiquid}, minus the nether-water branch. */
    public boolean tryPlaceContainedLiquid(Level level, BlockPos pos) {
        BlockState at = level.getBlockState(pos);
        boolean flag = at.getCollisionShape(level, pos).isEmpty(); // 1.7 !material.isSolid()
        if (!at.isAir() && !flag) return false;
        if (!level.isClientSide() && flag && at.getFluidState().isEmpty()) {
            level.destroyBlock(pos, true);
        }
        level.setBlock(pos, fluid.get().defaultBlockState(), 3);
        return true;
    }
}
