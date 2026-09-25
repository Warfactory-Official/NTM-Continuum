package com.hbm.handler;

import java.util.HashMap;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.hbm.potion.ModEffects;
import dev.golem.core.world.Effects;

/**
 * Per-armor-item radiation-resistance coefficients, ported from 1.12.2's {@code HazmatRegistry}. The
 * total per-entity resistance is the sum over the 4 worn pieces; the radiation reduction factor is
 * {@code 10^-resistance} (see {@code ContaminationUtil.calculateRadiationMod}).
 *
 * <p>Coefficients copied verbatim: hazmat yellow 0.6, red 1.0, grey 2.0 (per set — split over the 4
 * slots by the {@code helmet .2 / chest .4 / legs .3 / boots .1} weights); vanilla iron/gold 0.0225.
 * Registration happens where the armor items are created ({@code ModGearItems}). Deferred: cladding
 * armor mods, the rad-x potion bonus, the JSON config override.
 */
public class HazmatRegistry {

    public static final double helmet = 0.2D;
    public static final double chest = 0.4D;
    public static final double legs = 0.3D;
    public static final double boots = 0.1D;

    // Set coefficients (multiply by the slot weight when registering a piece).
    public static final double iron = 0.0225D;      // 5%
    public static final double gold = 0.0225D;      // 5%
    public static final double hazYellow = 0.6D;    // 50%
    public static final double hazRed = 1.0D;       // 90%
    public static final double hazGray = 2D;        // 99%
    public static final double liquidator = 2.4D;   // 99.6%
    public static final double security = 0.825D;   // 85%
    public static final double star = 1.0D;         // 90%
    public static final double schrab = 3D;         // 99.9%
    public static final double euph = 10D;          // <100%
    public static final double cobalt = 0.125D;     // 25%
    public static final double paa = 1.7D;          // 97%

    private static final HashMap<Item, Double> entries = new HashMap<>();

    public static void registerHazmat(Item item, double resistance) {
        entries.put(item, resistance);
    }

    public static @org.jspecify.annotations.Nullable Double registered(Item item) {
        return entries.get(item);
    }

    public static void unregisterHazmat(Item item) {
        entries.remove(item);
    }

    public static double getResistance(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return 0;

        double cladding = getCladding(stack);
        Double f = entries.get(stack.getItem());
        return f != null ? f + cladding : cladding;
    }

    /** Returns radiation cladding from the ordered armor-module component, if installed. */
    public static double getCladding(ItemStack stack) {
        ItemStack mod = ArmorModHandler.pryMod(stack, ArmorModHandler.cladding);
        return mod.getItem() instanceof com.hbm.items.armor.ItemModCladding cladding ? cladding.rad : 0D;
    }

    public static float getResistance(LivingEntity entity) {
        float res = 0.0F;

        if (entity instanceof Player player) {
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.HEAD));
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.CHEST));
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.LEGS));
            res += (float) getResistance(player.getItemBySlot(EquipmentSlot.FEET));
        }

        if (Effects.hasRegistered(entity, ModEffects.RADX.get())) res += 0.2F;

        return res;
    }
}
