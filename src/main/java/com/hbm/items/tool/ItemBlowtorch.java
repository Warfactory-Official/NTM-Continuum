package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.Locale;

/**
 * Gas blowtorch — the {@link IToolable.ToolType#TORCH} tool. Right-clicking a {@link IToolable} block
 * welds it into its upgraded form, spending {@link #COST} mB of {@link Fluids#GAS}. Its one job in this
 * wave is turning a raw fusion component into a welded BSCCO magnet coil.
 *
 * <p>Reduced from the 1.12.2 original, which also backed the acetylene torch (a second fuel pair) and
 * spawned a Tau particle burst on use. The gas variant, its fill semantics and the 250&nbsp;mB cost are
 * unchanged; fill rides {@link ModDataComponents#FLUID_AMOUNT}, though unlike {@code ItemFluidTank} an absent
 * component means a full torch rather than an empty one.
 */
public class ItemBlowtorch extends CompatItem implements IFillableItem {

    public static final int MAX_FILL = 4_000;
    /** mB of gas burned per successful weld (1.12.2 {@code ItemBlowtorch.onItemUse}). */
    public static final int COST = 250;

    public ItemBlowtorch(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof IToolable toolable)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        // The torch always reports SUCCESS so vanilla doesn't fall through to placing/using something
        // else on a block it clearly meant to weld (the original's "minecraft is stupid" comment).
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        ItemStack stack = context.getItemInHand();
        if (getFill(stack) < COST) return InteractionResult.SUCCESS;

        if (toolable.onScrew(level, player, pos, context.getClickedFace(), context.getClickLocation(),
                context.getHand(), IToolable.ToolType.TORCH)) {
            setFill(stack, getFill(stack) - COST);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Writes the fill. Note the clamped zero is stored rather than removed: absence means <i>full</i>
     * here (see {@link #getFill}), so dropping the component on an emptied torch would silently refuel it.
     */
    public void setFill(ItemStack stack, int amount) {
        ModDataComponents.fluidAmount(stack, Math.max(0, Math.min(amount, MAX_FILL)));
    }

    // --- IFillableItem ---

    /**
     * Absence of the component means <b>full</b>, not empty — the 1.12.2 original lazily ran
     * {@code initNBT} on first read, which filled a pristine torch to capacity, so one taken from the
     * creative tab or crafted is ready to use. Defaulting to zero instead makes every new torch inert.
     */
    @Override public int getFill(ItemStack stack) {
        return Math.max(0, ModDataComponents.FLUID_AMOUNT.getOr(stack, MAX_FILL));
    }
    @Override public boolean acceptsFluid(FluidType type, ItemStack stack) { return type == Fluids.GAS && getFill(stack) < MAX_FILL; }
    @Override public boolean providesFluid(FluidType type, ItemStack stack) { return false; }
    @Override public int tryEmpty(FluidType type, int amount, ItemStack stack) { return 0; }
    @Override public FluidType getFirstFluidType(ItemStack stack) { return getFill(stack) > 0 ? Fluids.GAS : Fluids.NONE; }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack) || amount <= 0) return amount;
        int added = Math.min(amount, MAX_FILL - getFill(stack));
        setFill(stack, getFill(stack) + added);
        return amount - added;
    }

    // --- display ---

    @Override public boolean isBarVisible(ItemStack stack) { return getFill(stack) < MAX_FILL; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(getFill(stack) * 13.0F / MAX_FILL); }
    @Override public int getBarColor(ItemStack stack) { return Fluids.GAS.getColor(); }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         java.util.List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal(String.format(Locale.US, "%,d / %,d mB", getFill(stack), MAX_FILL))
                .withStyle(ChatFormatting.YELLOW));
    }
}
