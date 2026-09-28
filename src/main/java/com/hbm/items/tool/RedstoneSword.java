package com.hbm.items.tool;

import dev.golem.core.world.CompatSwordItem;
import dev.golem.core.world.ToolTier;
import dev.golem.core.world.Tools;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/**
 * 1.7 {@code RedstoneSword} ("Pridenauer you damn bastard."): a stone-material sword that lays redstone
 * wire on the clicked face, costing 14 durability per use whether or not wire was placed.
 */
public class RedstoneSword extends CompatSwordItem {

    /** Vanilla 1.7 {@code ToolMaterial.STONE}: level 1, 131 uses, speed 4, damage 1, enchantability 5. */
    public static final ToolTier STONE = new ToolTier(1, 131, 4.0F, 1.0F, 5, ItemTags.STONE_TOOL_MATERIALS);

    public RedstoneSword(Properties properties) {
        // 1.7 ItemSword: 4 + the material's damage.
        super(properties, STONE, 4.0F + STONE.attackDamageBonus(), Tools.SWORD_SPEED, 0.0D);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack itemStack = context.getItemInHand();
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());

        if (player == null || !player.mayUseItemAt(pos, context.getClickedFace(), itemStack)) {
            return InteractionResult.FAIL;
        }

        if (!world.isClientSide()) {
            if (world.isEmptyBlock(pos)) {
                world.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, SoundEvents.ITEM_BREAK,
                        SoundSource.BLOCKS, 1.0F, world.getRandom().nextFloat() * 0.4F + 0.8F);
                world.setBlockAndUpdate(pos, Blocks.REDSTONE_WIRE.defaultBlockState());
            }

            Tools.hurt(itemStack, 14, player);
        }
        return InteractionResult.SUCCESS;
    }
}
