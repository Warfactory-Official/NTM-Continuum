package com.hbm.items.gear;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.items.ModDataComponents;
import com.hbm.util.BobMathUtil;
import dev.golem.core.world.ArmorSlot;
import dev.golem.core.world.CompatTooltipContext;
import dev.golem.core.world.GolemArmorMaterial;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * 1.7 {@code ArmorFSBFueled}: a full-set suit that burns a fluid instead of HE.
 *
 * <ul>
 * <li><b>A fresh suit is full.</b> 1.7 wrote {@code maxFuel} into a missing tag from inside {@code getFill};
 *     here the missing component simply reads as {@code maxFuel}, the same answer without dirtying the stack
 *     from a getter (see {@link ArmorFSBPowered}).</li>
 * <li><b>Damage burns fuel.</b> 1.7 overrode {@code setDamage} to take {@code damage * consumption} fuel and
 *     never store durability. The port's suits are unbreakable and {@code DamageResistanceHandler} forwards the
 *     damage to {@link #burn}.</li>
 * <li><b>The idle drain</b> is {@code drain} every 10 world ticks while a complete, working set is worn and
 *     the wearer is not in creative.</li>
 * </ul>
 *
 * <p>The fuel is kept in the {@code hbm:jetpack_fuel} component, the port's existing home for 1.7's {@code "fuel"}
 * integer tag. Only an explicit {@code tryFill} adds fuel; the suit never gives any back.
 */
public class ArmorFSBFueled extends ArmorFSB {

    private final Supplier<FluidType> fuelType;
    public final int maxFuel;
    public final int fillRate;
    public final int consumption;
    public final int drain;

    public ArmorFSBFueled(GolemArmorMaterial material, ArmorSlot slot, Properties properties,
                          Supplier<FluidType> fuelType, int maxFuel, int fillRate, int consumption, int drain) {
        super(material, slot, properties);
        this.fuelType = fuelType;
        this.maxFuel = maxFuel;
        this.fillRate = fillRate;
        this.consumption = consumption;
        this.drain = drain;
    }

    public FluidType fuelType() {
        return fuelType.get();
    }

    @Override
    public int getFill(ItemStack stack) {
        return ModDataComponents.JETPACK_FUEL.getOr(stack, maxFuel);
    }

    public void setFill(ItemStack stack, int fill) {
        // Write only on change: a component write resyncs the stack.
        if (ModDataComponents.JETPACK_FUEL.getOr(stack, maxFuel) != fill) {
            ModDataComponents.JETPACK_FUEL.set(stack, fill);
        }
    }

    public int getMaxFill(ItemStack stack) {
        return maxFuel;
    }

    /** 1.7 {@code setDamage(stack, damage)}: {@code damage * consumption} fuel, never below zero. */
    public void burn(ItemStack stack, int damage) {
        setFill(stack, Math.max(getFill(stack) - damage * consumption, 0));
    }

    @Override
    public boolean isArmorEnabled(ItemStack stack) {
        return getFill(stack) > 0;
    }

    /** 1.7 {@code onArmorTick}: the base FSB tick, then the idle drain every tenth world tick. */
    @Override
    public void armorTick(Level level, Player player, ItemStack stack) {
        super.armorTick(level, player, stack);

        if (this.drain > 0 && ArmorFSB.hasFSBArmor(player) && !player.isCreative()
                && level.getGameTime() % 10 == 0 && !level.isClientSide()) {
            setFill(stack, Math.max(getFill(stack) - this.drain, 0));
        }
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context, List<Component> tooltip,
                                         TooltipFlag flag) {
        tooltip.add(Component.empty().append(fuelType().getDisplayName()).append(": "
                + BobMathUtil.getShortNumber(getFill(stack)) + " / " + BobMathUtil.getShortNumber(getMaxFill(stack))));
        super.appendCompatHoverText(stack, context, tooltip, flag);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getFill(stack) < getMaxFill(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getFill(stack) / (float) getMaxFill(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        // Forge 1.7 RenderItem.renderItemOverlayIntoGUI: j = round(255 - damage * 255), colour (255 - j, j, 0).
        double damage = 1D - getFill(stack) / (double) getMaxFill(stack);
        int j = (int) Math.round(255.0D - damage * 255.0D);
        return (255 - j) << 16 | j << 8;
    }

    // ---- IFillableItem: fill only, and only with the suit's own fuel ----

    @Override
    public boolean acceptsFluid(FluidType type, ItemStack stack) {
        return type == fuelType();
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
        return 0;
    }

    @Override
    public FluidType getFirstFluidType(ItemStack stack) {
        return null;
    }
}
