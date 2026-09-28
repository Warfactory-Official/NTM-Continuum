package com.hbm.blocks.generic;

import com.hbm.blocks.ModDungeonBlocks;
import com.hbm.items.ModItems;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 1.7 {@code BlockForgottenBrick}, the unbreakable "Block" of the forgotten dungeon. 1.7's seven metadata looks
 * are flattened the port's way: {@code brick_forgotten} (0), {@code _bw} (1), {@code _nullstone} (2), {@code _hole}
 * (3), {@code _hole_empty} (4), {@code _nullroom_wood} (5) and {@code _nullroom_stone} (6).
 *
 * <p>Only the hole does anything: an empty hand pulls an eternal coal out of it, straight into the selected
 * hotbar slot, and leaves the empty hole behind.
 */
public class BlockForgottenBrick extends CompatBlock {

    public enum Variant { DEFAULT, BW, NULLSTONE, HOLE, HOLE_EMPTY, NULLROOM_WOOD, NULLROOM_STONE }

    private final Variant variant;

    public BlockForgottenBrick(Properties properties, Variant variant) {
        super(properties);
        this.variant = variant;
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        if (variant != Variant.HOLE || hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            player.getInventory().setItem(Compat.selectedSlot(player), new ItemStack(ModItems.coal_eternal.get()));
            level.setBlock(pos, ModDungeonBlocks.brick_forgotten_hole_empty.get().defaultBlockState(), 3);
        }
        return InteractionResult.SUCCESS;
    }
}
