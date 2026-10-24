package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.ModDataComponents;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
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
 * Gas blowtorch: the {@link IToolable.ToolType#TORCH} tool. Right-clicking a {@link IToolable} block
 * welds it into its upgraded form, spending {@link #COST} mB of {@link Fluids#GAS}. Its one job is
 * turning a raw fusion component into a welded BSCCO magnet coil.
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
        BlockPos pos = IToolable.targetPos(level, context.getClickedPos());
        if (pos == null) return InteractionResult.PASS;
        var toolable = com.hbm.capability.NtmContracts.TOOLABLE.at(level, pos);
        if (toolable == null) return InteractionResult.PASS;

        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        if (getFill(stack) < COST) return InteractionResult.PASS;
        if (level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof com.hbm.interfaces.IRepairable) {
                return com.hbm.interfaces.IRepairable.tryRepairMultiblock(level, pos, player)
                        ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            return InteractionResult.SUCCESS;
        }

        if (toolable.onScrew(level, player, pos, context.getClickedFace(), context.getClickLocation(),
                context.getHand(), IToolable.ToolType.TORCH)) {
            setFill(stack, getFill(stack) - COST);
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.7F, 1.6F);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /**
     * Writes the fill. Note the clamped zero is stored rather than removed: absence means <i>full</i>
     * here (see {@link #getFill}), so dropping the component on an emptied torch would silently refuel it.
     */
    public void setFill(ItemStack stack, int amount) {
        ModDataComponents.fluidAmount(stack, Math.max(0, Math.min(amount, MAX_FILL)));
    }

    // IFillableItem

    /**
     * Absence of the component means <b>full</b>, not empty: the 1.12.2 original lazily ran {@code initNBT} on
     * first read, which filled a pristine torch to capacity. Defaulting to zero would make every new torch inert.
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
        // 1.7 takes at most 50 mB per call
        int added = Math.min(Math.min(amount, 50), MAX_FILL - getFill(stack));
        setFill(stack, getFill(stack) + added);
        return amount - added;
    }

    // Display

    @Override public boolean isBarVisible(ItemStack stack) { return getFill(stack) < MAX_FILL; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(getFill(stack) * 13.0F / MAX_FILL); }
    @Override public int getBarColor(ItemStack stack) { return Fluids.GAS.getColor(); }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.empty().append(Fluids.GAS.getDisplayName())
                .append(String.format(Locale.US, ": %,d / %,d", getFill(stack), MAX_FILL))
                .withStyle(ChatFormatting.YELLOW));
    }
}
