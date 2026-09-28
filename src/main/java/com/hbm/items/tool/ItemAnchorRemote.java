package com.hbm.items.tool;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.blocks.ModRadarUtilityBlocks;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModRadarUtilityItems;
import com.hbm.lib.Library;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 1.7 {@code ItemAnchorRemote}, the "Recall Device": an {@code ItemBattery(1_000_000, 10_000, 0)} that remembers a
 * tele-anchor. Right-clicking the anchor saves it; right-clicking the air spends 10,000 HE to pop a harmless
 * 2-strength explosion over the anchor and put the player on it.
 *
 * <p>The charge is {@link ModDataComponents#BATTERY_CHARGE}; a stack without it reads as full, which is what
 * 1.7's {@code ItemBattery.getCharge} wrote into a stack that had no tag. The crafted remote comes out empty
 * (1.7 {@code getEmptyBattery}). 1.7 also spawned 32 portal particles from the server world, which the 1.7
 * server discarded, so nothing is spawned here.
 */
public class ItemAnchorRemote extends CompatItem implements IBatteryItem {

    public static final long MAX_CHARGE = 1_000_000L;
    public static final long CHARGE_RATE = 10_000L;
    public static final long DISCHARGE_RATE = 0L;
    public static final long USE_COST = 10_000L;

    public ItemAnchorRemote(Properties properties) {
        super(properties);
    }

    @Override public void chargeBattery(ItemStack stack, long i) { setCharge(stack, getCharge(stack) + i); }
    @Override public void dischargeBattery(ItemStack stack, long i) { setCharge(stack, getCharge(stack) - i); }
    @Override public void setCharge(ItemStack stack, long i) { ModDataComponents.BATTERY_CHARGE.set(stack, Math.max(0L, Math.min(i, MAX_CHARGE))); }
    @Override public long getCharge(ItemStack stack) { return ModDataComponents.BATTERY_CHARGE.getOr(stack, MAX_CHARGE); }
    @Override public long getMaxCharge(ItemStack stack) { return MAX_CHARGE; }
    @Override public long getChargeRate(ItemStack stack) { return CHARGE_RATE; }
    @Override public long getDischargeRate(ItemStack stack) { return DISCHARGE_RATE; }

    // 1.7 ItemBattery: the durability bar is the missing charge
    @Override public boolean isBarVisible(ItemStack stack) { return getCharge(stack) < MAX_CHARGE; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13.0F * getCharge(stack) / MAX_CHARGE); }
    @Override public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) getCharge(stack) / MAX_CHARGE) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        long charge = getCharge(stack);
        tooltip.add(Component.literal("Energy stored: " + Library.getShortNumber(charge) + "/"
                + Library.getShortNumber(MAX_CHARGE) + "HE"));
        tooltip.add(Component.literal("Charge rate: " + Library.getShortNumber(CHARGE_RATE) + "HE/t"));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        if (context.getLevel().getBlockState(pos).getBlock() == ModRadarUtilityBlocks.teleanchor.get()) {
            ModRadarUtilityItems.ANCHOR_POSITION.set(context.getItemInHand(), pos.immutable());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult compatUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || level.isClientSide()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos pos = ModRadarUtilityItems.ANCHOR_POSITION.get(stack).orElse(null);
        if (pos == null || this.getCharge(stack) < USE_COST) {
            orb(level, player);
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }

        level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);

        if (level.getBlockState(pos).getBlock() == ModRadarUtilityBlocks.teleanchor.get()) {
            if (player.isPassenger()) player.stopRiding();

            level.explode(player, pos.getX() + 0.5, pos.getY() + 1 + player.getBbHeight() / 2, pos.getZ() + 0.5, 2F,
                    false, Level.ExplosionInteraction.NONE);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            player.teleportTo(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            player.resetFallDistance();
            this.dischargeBattery(stack, USE_COST);
            return InteractionResult.SUCCESS;
        } else {
            orb(level, player);
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private static void orb(Level level, Player player) {
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                SoundSource.PLAYERS, 0.25F, 0.75F);
    }
}
