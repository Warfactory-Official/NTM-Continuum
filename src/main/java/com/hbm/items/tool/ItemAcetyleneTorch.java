package com.hbm.items.tool;

import com.hbm.api.block.IToolable;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;
import java.util.Locale;

/**
 * 1.7 {@code ItemBlowtorch} as the acetylene welding torch: the {@link IToolable.ToolType#TORCH} tool that runs on
 * two fluids, 8,000 mB of unsaturated hydrocarbons and 16,000 mB of oxygen, and spends 20 and 10 of them per
 * successful weld. The gas blowtorch half of the same 1.7 class is {@link ItemBlowtorch}.
 *
 * <p>Like 1.7's lazy {@code initNBT}, a stack with no stored fill reads as full; {@link #getEmptyTool} is the
 * crafted, empty torch. The Tau particle burst on a weld is not ported (the gas blowtorch skips it too).
 */
public class ItemAcetyleneTorch extends CompatItem implements IFillableItem {

    public static final GolemComponent<Integer> UNSATURATEDS = GolemComponent.integer("hbm", "torch_unsaturateds");
    public static final GolemComponent<Integer> OXYGEN = GolemComponent.integer("hbm", "torch_oxygen");

    public ItemAcetyleneTorch(Properties properties) {
        super(properties);
    }

    public static int getMaxFill(FluidType type) {
        if (type == Fluids.UNSATURATEDS) return 8_000;
        if (type == Fluids.OXYGEN) return 16_000;
        return 0;
    }

    private static GolemComponent<Integer> component(FluidType type) {
        return type == Fluids.UNSATURATEDS ? UNSATURATEDS : type == Fluids.OXYGEN ? OXYGEN : null;
    }

    public int getFill(ItemStack stack, FluidType type) {
        GolemComponent<Integer> component = component(type);
        return component == null ? 0 : component.getOr(stack, getMaxFill(type));
    }

    public void setFill(ItemStack stack, FluidType type, int fill) {
        GolemComponent<Integer> component = component(type);
        if (component != null) component.set(stack, fill);
    }

    /** 1.7 {@code ItemBlowtorch.getEmptyTool}: both fills written as zero. */
    public ItemStack getEmptyTool() {
        ItemStack stack = new ItemStack(this);
        setFill(stack, Fluids.UNSATURATEDS, 0);
        setFill(stack, Fluids.OXYGEN, 0);
        return stack;
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == Fluids.UNSATURATEDS || type == Fluids.OXYGEN;
    }

    /** 1.7: at most 50 mB per call, clamped to the fluid's room. */
    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;
        int toFill = Math.min(amount, 50);
        toFill = Math.min(toFill, getMaxFill(type) - getFill(stack, type));
        setFill(stack, type, getFill(stack, type) + toFill);
        return amount - toFill;
    }

    @Override public boolean providesFluid(FluidType type, ItemStack stack) { return false; }
    @Override public int tryEmpty(FluidType type, int amount, ItemStack stack) { return amount; }
    @Override public FluidType getFirstFluidType(ItemStack stack) { return null; }
    @Override public int getFill(ItemStack stack) { return 0; }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block b = world.getBlockState(pos).getBlock();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (b instanceof IToolable toolable && player != null) {
            if (getFill(stack, Fluids.UNSATURATEDS) < 20) return InteractionResult.TRY_WITH_EMPTY_HAND;
            if (getFill(stack, Fluids.OXYGEN) < 10) return InteractionResult.TRY_WITH_EMPTY_HAND;

            if (toolable.onScrew(world, player, pos, context.getClickedFace(), context.getClickLocation(),
                    context.getHand(), IToolable.ToolType.TORCH)) {
                if (!world.isClientSide()) {
                    setFill(stack, Fluids.UNSATURATEDS, getFill(stack, Fluids.UNSATURATEDS) - 20);
                    setFill(stack, Fluids.OXYGEN, getFill(stack, Fluids.OXYGEN) - 10);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private double frac(ItemStack stack) {
        return Math.min((double) getFill(stack, Fluids.UNSATURATEDS) / getMaxFill(Fluids.UNSATURATEDS),
                (double) getFill(stack, Fluids.OXYGEN) / getMaxFill(Fluids.OXYGEN));
    }

    /** 1.7 showed the bar whenever {@code 1 - frac > 0}, i.e. whenever either tank is below full. */
    @Override public boolean isBarVisible(ItemStack stack) { return frac(stack) < 1D; }
    @Override public int getBarWidth(ItemStack stack) { return (int) Math.round(13.0D * frac(stack)); }
    @Override public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb((float) (frac(stack) / 3.0D), 1.0F, 1.0F);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(gauge(stack, Fluids.UNSATURATEDS).withStyle(ChatFormatting.YELLOW));
        list.add(gauge(stack, Fluids.OXYGEN).withStyle(ChatFormatting.AQUA));
    }

    private MutableComponent gauge(ItemStack stack, FluidType type) {
        return Component.empty().append(type.getDisplayName()).append(": "
                + String.format(Locale.US, "%,d", getFill(stack, type)) + " / "
                + String.format(Locale.US, "%,d", getMaxFill(type)));
    }
}
