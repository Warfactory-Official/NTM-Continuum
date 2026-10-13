package com.hbm.items.tool;

import com.hbm.Constants;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.inventory.fluid.FluidType;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.ToolTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 1.7 {@code ItemToolAbilityFueled}: an ability tool that burns fluid fuel, {@code consumption} mB per
 * point of wear, refilled by any fluid-filling machine at {@code fillRate} mB per operation.
 */
public class ItemToolAbilityFueled extends ItemToolAbility implements IFillableItem {

    /** 1.7 NBT {@code fuel}. */
    public static final GolemComponent<Integer> FUEL = GolemComponent.integer(Constants.MOD_ID, "tool_fuel");

    protected int fillRate;
    protected int consumption;
    protected int maxFuel;
    protected final Set<FluidType> acceptedFuels = new LinkedHashSet<>();

    public ItemToolAbilityFueled(Properties properties, float damage, double movement, ToolTier material,
                                 EnumToolType type, int maxFuel, int consumption, int fillRate,
                                 FluidType... acceptedFuels) {
        super(properties, damage, movement, withoutWear(material), type);
        this.maxFuel = maxFuel;
        this.consumption = consumption;
        this.fillRate = fillRate;
        for (FluidType fuel : acceptedFuels) this.acceptedFuels.add(fuel);
    }

    /**
     * 1.7 {@code setMaxDamage(1)} plus a {@code setDamage} that burned fuel instead: the material's durability never
     * applied, every point of wear was fuel. Vanilla wears a tool by its tier's uses on every hit and block (the
     * {@code weapon}/{@code tool} components), so the tier keeps everything but its uses and wear reaches the fuel
     * through {@link #damageTool} alone. Keeping the uses would change the damage component on each hit and
     * re-equip the held chainsaw.
     */
    private static ToolTier withoutWear(ToolTier material) {
        return ToolTier.of(material.level(), 0, material.speed(), material.attackDamageBonus(),
                material.enchantability(), material::repairItems);
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> list,
                                         TooltipFlag flag) {
        list.add(Component.literal("Fuel: " + this.getFill(stack) + "/" + this.maxFuel + "mB").withStyle(ChatFormatting.GOLD));

        for (FluidType type : acceptedFuels) {
            list.add(Component.literal("- ").append(type.getDisplayName()).withStyle(ChatFormatting.YELLOW));
        }

        super.appendCompatHoverText(stack, context, list, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < maxFuel;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFill(stack) / (float) maxFuel);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0.0F, (float) getFill(stack) / maxFuel) / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean canOperate(ItemStack stack) {
        return getFill(stack) >= this.consumption;
    }

    /** 1.7 {@code setDamage}: every point of wear burns {@code consumption} mB. */
    @Override
    public void damageTool(ItemStack stack, int amount, LivingEntity user) {
        this.setFill(stack, Math.max(this.getFill(stack) - amount * consumption, 0));
    }

    @Override
    protected void spendOnHit(ItemStack stack, LivingEntity attacker) {
        if (!attacker.level().isClientSide()) damageTool(stack, 1, attacker);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity owner) {
        if (!level.isClientSide() && state.getDestroySpeed(level, pos) != 0.0F) damageTool(stack, 1, owner);
        return true;
    }

    /** 1.7: a stack that never had fuel written is full. */
    @Override
    public int getFill(ItemStack stack) {
        return FUEL.getOr(stack, maxFuel);
    }

    public void setFill(ItemStack stack, int fill) {
        FUEL.set(stack, fill);
    }

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return this.acceptedFuels.contains(type);
    }

    @Override
    public int tryFill(FluidType type, int amount, ItemStack stack) {
        if (!acceptsFluid(type, stack)) return amount;

        int toFill = Math.min(amount, this.fillRate);
        toFill = Math.min(toFill, this.maxFuel - this.getFill(stack));
        this.setFill(stack, this.getFill(stack) + toFill);

        return amount - toFill;
    }

    @Override
    public boolean providesFluid(FluidType type, ItemStack stack) {
        return false;
    }

    @Override
    public int tryEmpty(FluidType type, int amount, ItemStack stack) {
        return amount;
    }

    public static ItemStack getEmptyTool(Item item) {
        ItemToolAbilityFueled tool = (ItemToolAbilityFueled) item;
        ItemStack stack = new ItemStack(item);
        tool.setFill(stack, 0);
        return stack;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return null;
    }
}
