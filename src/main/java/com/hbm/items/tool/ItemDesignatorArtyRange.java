package com.hbm.items.tool;

import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.turret.TileEntityTurretBaseArtillery;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import dev.golem.core.world.SubLevels;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The artillery designator: right-click a gun to bind to it, then right-click anywhere within five
 * hundred blocks to queue that as a fire mission.
 *
 * <p>The binding half follows {@link ItemRBMKTool}, a linked multiblock core stored as a packed
 * position on the stack, resolved through {@link MultiblockSurface#coreOfAny} so clicking any cell of
 * the emplacement works. Without this item the guns' manual mode is unreachable in game: nothing else
 * ported issues a command to an {@link com.hbm.api.block.IRadarCommandReceiver}.
 */
public class ItemDesignatorArtyRange extends CompatItem {

    private static final double REACH = 500D;

    public ItemDesignatorArtyRange(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        Long packed = ModDataComponents.ARTY_DESIGNATOR_TARGET.get(stack).orElse(null);
        if (packed == null) {
            tooltip.add(Component.translatable("item.hbm.designator_arty_range.unlinked")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        BlockPos pos = BlockPos.of(packed);
        tooltip.add(Component.translatable("item.hbm.designator_arty_range.linked",
                pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockPos core = MultiblockSurface.coreOfAny(level, clicked);
        if (core == null) core = clicked;

        if (!(level.getBlockEntity(core) instanceof TileEntityTurretBaseArtillery)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ModDataComponents.ARTY_DESIGNATOR_TARGET.set(context.getItemInHand(), core.asLong());
        Player player = context.getPlayer();
        if (player != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BLEEP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Long packed = ModDataComponents.ARTY_DESIGNATOR_TARGET.get(stack).orElse(null);
        if (packed == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        HitResult hit = player.pick(REACH, 1.0F, false);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.TRY_WITH_EMPTY_HAND;
        BlockPos aim = SubLevels.toWorldBlock(level, ((BlockHitResult) hit).getBlockPos());

        BlockEntity blockEntity = level.getBlockEntity(BlockPos.of(packed));
        if (blockEntity instanceof TileEntityTurretBaseArtillery arty) {
            arty.enqueueTarget(aim.getX() + 0.5D, aim.getY() + 0.5D, aim.getZ() + 0.5D);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }
}
