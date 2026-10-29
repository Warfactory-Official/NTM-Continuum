package com.hbm.items.machine;

import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import dev.golem.core.Ids;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Modern HBM battery / capacitor item: the "actual storage" the {@code machine_battery_socket} holds.
 * Each {@link EnumBatteryPack} variant (6 batteries + 6 capacitors) is registered as its <b>own</b> item
 * in 26.1 (the old mod used a single item with metadata subtypes, which 1.21+ removed). The stored charge
 * lives in the {@link ModDataComponents#BATTERY_CHARGE} component (the old NBT {@code "charge"} long).
 *
 * <p>Capacity / charge-rate / discharge-rate are intrinsic to the variant; the socket reads them via
 * {@link IBatteryItem}. The 3D inventory icon is rendered from {@code battery_socket.obj} (the Battery or
 * Capacitor part) by the {@code machine_obj} special-model system, per the variant's texture.
 */
public class ItemBatteryPack extends CompatItem implements IBatteryItem {

    public final EnumBatteryPack pack;

    /** The pack values: the {@code hbm:battery_pack} entry, else HBM's enum constant. */
    public ReactorDataMaps.BatteryPack stats() {
        ReactorDataMaps.BatteryPack data = ReactorDataMaps.BATTERY_PACK.get(this);
        return data != null ? data : codeStats();
    }

    /** The code defaults (constructor or enum values) that the data map entry overrides. */
    public ReactorDataMaps.BatteryPack codeStats() {
        return new ReactorDataMaps.BatteryPack(pack.capacity, pack.chargeRate,
                pack.dischargeRate, pack.isCapacitor(), pack.texture.toString());
    }

    public ItemBatteryPack(Properties properties, EnumBatteryPack pack) {
        super(properties);
        this.pack = pack;
    }

    private long charge(ItemStack stack) {
        return ModDataComponents.batteryCharge(stack);
    }

    public ItemStack emptyStack() {
        var stack = new ItemStack(this);
        setCharge(stack, 0L);
        return stack;
    }

    public ItemStack fullStack() {
        var stack = new ItemStack(this);
        setCharge(stack, getMaxCharge(stack));
        return stack;
    }

    public List<ItemStack> creativeStacks() {
        return List.of(emptyStack(), fullStack());
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        setCharge(stack, IBatteryItem.adjustCharge(getCharge(stack), i, getMaxCharge(stack), false));
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        setCharge(stack, IBatteryItem.adjustCharge(getCharge(stack), i, getMaxCharge(stack), true));
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        var clamped = IBatteryItem.clampCharge(i, stats().capacity());
        ModDataComponents.batteryCharge(stack, clamped);
    }

    @Override
    public long getCharge(ItemStack stack) {
        return IBatteryItem.clampCharge(charge(stack), getMaxCharge(stack));
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return stats().capacity();
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return stats().chargeRate();
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return stats().dischargeRate();
    }

    // --- durability bar shows the fill level ---

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCharge(stack) < getMaxCharge(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getCharge(stack) / getMaxCharge(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x33CC33; // green
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        long c = getCharge(stack);
        var pack = stats();
        tooltip.add(Component.literal("Energy stored: " + Library.getShortNumber(c) + "/" + Library.getShortNumber(pack.capacity())
                + "HE (" + (pack.capacity() <= 0 ? 0 : c * 100 / pack.capacity()) + "%)").withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.literal("Charge rate: " + Library.getShortNumber(pack.chargeRate()) + "HE/t").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.literal("Discharge rate: " + Library.getShortNumber(pack.dischargeRate()) + "HE/t").withStyle(ChatFormatting.YELLOW));
    }

    /**
     * The 12 battery/capacitor variants with their real 1.12.2 capacities/rates. The two private
     * constructors reproduce the original's derivation: a "battery" stores 15 minutes of discharge and
     * charges 10× as fast; a "capacitor" stores 30 seconds and charges at its discharge rate; the quantum
     * battery uses an explicit duration (1 hour).
     */
    public enum EnumBatteryPack {
        BATTERY_REDSTONE("battery_redstone", 100L, false),
        BATTERY_LEAD("battery_lead", 1_000L, false),
        BATTERY_LITHIUM("battery_lithium", 10_000L, false),
        BATTERY_SODIUM("battery_sodium", 50_000L, false),
        BATTERY_SCHRABIDIUM("battery_schrabidium", 250_000L, false),
        BATTERY_QUANTUM("battery_quantum", 1_000_000L, 20 * 60 * 60),

        CAPACITOR_COPPER("capacitor_copper", 1_000L, true),
        CAPACITOR_GOLD("capacitor_gold", 10_000L, true),
        CAPACITOR_NIOBIUM("capacitor_niobium", 100_000L, true),
        CAPACITOR_TANTALUM("capacitor_tantalum", 500_000L, true),
        CAPACITOR_BISMUTH("capacitor_bismuth", 2_500_000L, true),
        CAPACITOR_SPARK("capacitor_spark", 10_000_000L, true);

        /** Registry id and texture base name. */
        public final String name;
        /** Model texture {@code textures/models/machines/<name>.png}. */
        public final Identifier texture;
        public final long capacity;
        public final long chargeRate;
        public final long dischargeRate;
        public static final EnumBatteryPack[] VALUES = values();

        EnumBatteryPack(String name, long dischargeRate, boolean capacitor) {
            this(name,
                    capacitor ? (dischargeRate * 20 * 30) : (dischargeRate * 20 * 60 * 15),
                    capacitor ? dischargeRate : dischargeRate * 10,
                    dischargeRate);
        }

        EnumBatteryPack(String name, long dischargeRate, long duration) {
            this(name, dischargeRate * duration, dischargeRate * 10, dischargeRate);
        }

        EnumBatteryPack(String name, long capacity, long chargeRate, long dischargeRate) {
            this.name = name;
            this.texture = Ids.of("hbm", "textures/models/machines/" + name + ".png");
            this.capacity = capacity;
            this.chargeRate = chargeRate;
            this.dischargeRate = dischargeRate;
        }

        public boolean isCapacitor() {
            return this.ordinal() > BATTERY_QUANTUM.ordinal();
        }
    }
}
