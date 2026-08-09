package com.hbm.items.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.items.ModDataComponents;
import com.hbm.lib.Library;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * Modern HBM battery / capacitor item — the "actual storage" the {@code machine_battery_socket} holds.
 * Each {@link EnumBatteryPack} variant (6 batteries + 6 capacitors) is registered as its <b>own</b> item
 * in 26.1 (the old mod used a single item with metadata subtypes, which 1.21+ removed). The stored charge
 * lives in the {@link ModDataComponents#BATTERY_CHARGE} component (the old NBT {@code "charge"} long).
 *
 * <p>Capacity / charge-rate / discharge-rate are intrinsic to the variant; the socket reads them via
 * {@link IBatteryItem}. The 3D inventory icon is rendered from {@code battery_socket.obj} (the Battery or
 * Capacitor part) by the {@code machine_obj} special-model system, per the variant's texture.
 */
public class ItemBatteryPack extends Item implements IBatteryItem {

    public final EnumBatteryPack pack;

    public ItemBatteryPack(Properties properties, EnumBatteryPack pack) {
        super(properties);
        this.pack = pack;
    }

    private long charge(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BATTERY_CHARGE.get(), 0L);
    }

    @Override
    public void chargeBattery(ItemStack stack, long i) {
        setCharge(stack, charge(stack) + i);
    }

    @Override
    public void dischargeBattery(ItemStack stack, long i) {
        setCharge(stack, charge(stack) - i);
    }

    @Override
    public void setCharge(ItemStack stack, long i) {
        long clamped = Math.max(0L, Math.min(i, pack.capacity));
        stack.set(ModDataComponents.BATTERY_CHARGE.get(), clamped);
    }

    @Override
    public long getCharge(ItemStack stack) {
        return charge(stack);
    }

    @Override
    public long getMaxCharge(ItemStack stack) {
        return pack.capacity;
    }

    @Override
    public long getChargeRate(ItemStack stack) {
        return pack.chargeRate;
    }

    @Override
    public long getDischargeRate(ItemStack stack) {
        return pack.dischargeRate;
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
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> adder, TooltipFlag flag) {
        long c = getCharge(stack);
        adder.accept(Component.literal("Energy stored: " + Library.getShortNumber(c) + "/" + Library.getShortNumber(pack.capacity)
                + "HE (" + (pack.capacity <= 0 ? 0 : c * 100 / pack.capacity) + "%)").withStyle(ChatFormatting.GREEN));
        adder.accept(Component.literal("Charge rate: " + Library.getShortNumber(pack.chargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
        adder.accept(Component.literal("Discharge rate: " + Library.getShortNumber(pack.dischargeRate) + "HE/t").withStyle(ChatFormatting.YELLOW));
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
            this.texture = Identifier.fromNamespaceAndPath("hbm", "textures/models/machines/" + name + ".png");
            this.capacity = capacity;
            this.chargeRate = chargeRate;
            this.dischargeRate = dischargeRate;
        }

        public boolean isCapacitor() {
            return this.ordinal() > BATTERY_QUANTUM.ordinal();
        }
    }
}
