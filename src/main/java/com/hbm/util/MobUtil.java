package com.hbm.util;

import com.hbm.entity.mob.ai.EntityAIFireGun;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModGearItems;
import dev.golem.core.Ids;
import dev.golem.core.world.Mobs;
import java.util.TreeMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 1.7 {@code MobUtil}, the part the dungeon action blocks use: the equipment slot pools the "gob block" mobs are
 * dressed from, {@link #assignItemsToEntity} and the gun task.
 *
 * <p>The pools name items by registry id and are resolved at draw time; an entry naming an item this port has not
 * registered draws nothing, keeping its weight (1.7 would have dressed the mob in it). 1.7's slot keys are kept:
 * 0 the held item, 1 boots, 2 leggings, 3 chestplate, 4 helmet. The soot-mob pools ({@code slotPoolCommonS},
 * {@code slotPoolRangedS}, {@code slotPoolGuns}) are drawn by {@link com.hbm.main.ModEventHandlerMobs} (1.7
 * {@code ModEventHandler.decorateMob}).
 */
public final class MobUtil {

    private MobUtil() {}

    /** One weighted choice: an item id, or null for "nothing" (1.7's null {@code WeightedRandomObject}). */
    public record Choice(@Nullable String id, int weight) {}

    public static final Map<Integer, List<Choice>> slotPoolCommon = new LinkedHashMap<>();
    public static final Map<Integer, List<Choice>> slotPoolRanged = new LinkedHashMap<>();
    public static final Map<Integer, List<Choice>> slotPoolAdv = new LinkedHashMap<>();
    public static final Map<Integer, List<Choice>> slotPoolAdvRanged = new LinkedHashMap<>();
    public static final Map<Integer, List<Choice>> slotPoolGunsTier1 = new HashMap<>();
    public static final Map<Integer, List<Choice>> slotPoolGunsTier2 = new HashMap<>();
    public static final Map<Integer, List<Choice>> slotPoolGunsTier3 = new HashMap<>();
    /** Soot mobs (1.7 {@code slotPoolCommonS}/{@code slotPoolRangedS}); key order is 1.7's HashMap order, 0..4. */
    public static final Map<Integer, List<Choice>> slotPoolCommonS = new TreeMap<>();
    public static final Map<Integer, List<Choice>> slotPoolRangedS = new TreeMap<>();
    /** 1.7 {@code slotPoolGuns}: keyed by the minimum soot level, not by slot. */
    public static final Map<Double, List<Choice>> slotPoolGuns = new HashMap<>();
    /** Declared in 1.7 and never filled; drawing from it does nothing, as there. */
    public static final Map<Integer, List<Choice>> slotPoolMasks = new HashMap<>();

    static {
        // soot mobs
        slotPoolCommonS.put(4, pool(8000, "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "cobalt_helmet", 2, "rag_piss", 1, "nossy_hat", 1, "alloy_helmet", 2,
                "titanium_helmet", 4, "steel_helmet", 8));
        slotPoolCommonS.put(3, pool(7000, "starmetal_plate", 1, "cobalt_plate", 2, "robes_plate", 32, "jackt", 32,
                "jackt2", 32, "alloy_plate", 2, "steel_plate", 2));
        slotPoolCommonS.put(2, pool(7000, "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2));
        slotPoolCommonS.put(1, pool(7000, "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2));
        slotPoolCommonS.put(0, pool(10000, "weapon_pipe_lead", 30, "crowbar", 25, "geiger_counter", 20, "reer_graar", 16,
                "steel_pickaxe", 12, "stopsign", 10, "sopsign", 8, "chernobylsign", 6, "steel_sword", 15,
                "titanium_sword", 8, "lead_gavel", 4, "wrench_flipped", 2, "wrench", 20));

        slotPoolRangedS.put(4, pool(8000, "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "rag_piss", 1, "goggles", 1, "alloy_helmet", 2, "titanium_helmet", 4,
                "steel_helmet", 8));
        slotPoolRangedS.put(3, pool(7000, "starmetal_plate", 1, "cobalt_plate", 2, "alloy_plate", 2, "steel_plate", 8,
                "titanium_plate", 4));
        slotPoolRangedS.put(2, pool(7000, "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2));
        slotPoolRangedS.put(1, pool(10000, "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2,
                "titanium_boots", 6));

        // soot guns (1.7 createSlotPool(Object[][]): no null entry)
        slotPoolGuns.put(0.3, pool(-1, "gun_light_revolver", 16, "gun_greasegun", 8, "gun_maresleg", 2));
        slotPoolGuns.put(1D, pool(-1, "gun_light_revolver", 6, "gun_greasegun", 8, "gun_maresleg", 4, "gun_henry", 6));
        slotPoolGuns.put(3D, pool(-1, "gun_uzi", 10, "gun_maresleg", 8, "gun_henry", 12, "gun_heavy_revolver", 4,
                "gun_flaregun", 2));
        slotPoolGuns.put(5D, pool(-1, "gun_am180", 6, "gun_uzi", 10, "gun_spas12", 8, "gun_henry_lincoln", 2,
                "gun_heavy_revolver", 12, "gun_flaregun", 4, "gun_flamer", 2));

        // gob block
        slotPoolCommon.put(4, pool(0, "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "cobalt_helmet", 2, "rag_piss", 1, "nossy_hat", 1, "alloy_helmet", 2,
                "titanium_helmet", 4, "steel_helmet", 8));
        slotPoolCommon.put(3, pool(10, "starmetal_plate", 1, "cobalt_plate", 2, "robes_plate", 32, "jackt", 32,
                "jackt2", 32, "alloy_plate", 2, "steel_plate", 2));
        slotPoolCommon.put(2, pool(20, "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2));
        slotPoolCommon.put(1, pool(10, "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2));
        slotPoolCommon.put(0, pool(1000, "weapon_pipe_lead", 30, "crowbar", 25, "geiger_counter", 20, "reer_graar", 16,
                "steel_pickaxe", 12, "stopsign", 10, "sopsign", 8, "chernobylsign", 6, "steel_sword", 15,
                "titanium_sword", 8, "lead_gavel", 4, "wrench_flipped", 2, "wrench", 20));

        slotPoolRanged.put(4, pool(0, "gas_mask_m65", 16, "gas_mask_olde", 12, "mask_of_infamy", 8, "gas_mask_mono", 8,
                "robes_helmet", 32, "no9", 16, "rag_piss", 1, "goggles", 1, "alloy_helmet", 2, "titanium_helmet", 4,
                "steel_helmet", 8));
        slotPoolRanged.put(3, pool(10, "starmetal_plate", 1, "cobalt_plate", 2, "alloy_plate", 2, "steel_plate", 8,
                "titanium_plate", 4));
        slotPoolRanged.put(2, pool(10, "zirconium_legs", 1, "cobalt_legs", 2, "steel_legs", 16, "titanium_legs", 8,
                "robes_legs", 32, "alloy_legs", 2));
        slotPoolRanged.put(1, pool(10, "robes_boots", 32, "steel_boots", 16, "cobalt_boots", 2, "alloy_boots", 2,
                "titanium_boots", 6));

        // 1.7 createSlotPool(Object[][]) without a null weight
        slotPoolAdv.put(4, pool(-1, "security_helmet", 10, "t51_helmet", 4, "asbestos_helmet", 12,
                "liquidator_helmet", 4, "no9", 12, "hazmat_helmet", 6));
        slotPoolAdv.put(3, pool(-1, "liquidator_plate", 4, "security_plate", 8, "asbestos_plate", 12, "t51_plate", 4,
                "hazmat_plate", 6, "steel_plate", 8));
        slotPoolAdv.put(2, pool(-1, "liquidator_legs", 4, "security_legs", 8, "asbestos_legs", 12, "t51_legs", 4,
                "hazmat_legs", 6, "steel_legs", 8));
        slotPoolAdv.put(1, pool(-1, "liquidator_boots", 4, "security_boots", 8, "asbestos_boots", 12, "t51_boots", 4,
                "hazmat_boots", 6, "robes_boots", 8));
        slotPoolAdv.put(0, pool(500, "weapon_pipe_lead", 20, "crowbar", 10, "geiger_counter", 10, "reer_graar", 20,
                "wrench_flipped", 20, "stopsign", 16, "sopsign", 4, "chernobylsign", 16, "titanium_sword", 18,
                "lead_gavel", 8, "wrench", 20));

        // for the action block
        slotPoolGunsTier1.put(0, pool(0, "gun_light_revolver", 16, "gun_greasegun", 8, "gun_maresleg", 2,
                "gun_flaregun", 1));
        slotPoolGunsTier2.put(0, pool(0, "gun_uzi", 12, "gun_maresleg", 8, "gun_henry", 12, "gun_heavy_revolver", 8,
                "gun_flaregun", 4, "gun_star_f", 8));
        slotPoolGunsTier3.put(0, pool(0, "gun_g3", 25, "gun_spas12", 20, "gun_carbine", 15, "gun_star_f", 20,
                "gun_am180", 6, "gun_amat", 5));

        slotPoolAdvRanged.putAll(slotPoolAdv);
        slotPoolAdvRanged.remove(0);
    }

    /** 1.7 {@code createSlotPool}: a null choice of {@code nullWeight} first (none if negative), then the items. */
    public static List<Choice> pool(int nullWeight, Object... idsAndWeights) {
        List<Choice> pool = new ArrayList<>();
        if (nullWeight >= 0) pool.add(new Choice(null, nullWeight));
        for (int i = 0; i < idsAndWeights.length; i += 2) {
            pool.add(new Choice("hbm:" + idsAndWeights[i], (Integer) idsAndWeights[i + 1]));
        }
        return pool;
    }

    private static EquipmentSlot slot(int legacy) {
        return switch (legacy) {
            case 1 -> EquipmentSlot.FEET;
            case 2 -> EquipmentSlot.LEGS;
            case 3 -> EquipmentSlot.CHEST;
            case 4 -> EquipmentSlot.HEAD;
            default -> EquipmentSlot.MAINHAND;
        };
    }

    /** 1.7 {@code WeightedRandom.getRandomItem}. */
    public static @Nullable Choice draw(List<Choice> pool, RandomSource rand) {
        int total = 0;
        for (Choice c : pool) total += c.weight();
        if (total <= 0) return null;
        int roll = rand.nextInt(total);
        for (Choice c : pool) {
            roll -= c.weight();
            if (roll < 0) return c;
        }
        return null;
    }

    /** 1.7 {@code equipFullSet}: helmet, chestplate, leggings and boots by registry id. */
    public static void equipFullSet(LivingEntity entity, String helmet, String chest, String legs, String boots) {
        String[] ids = {boots, legs, chest, helmet};
        for (int i = 0; i < 4; i++) {
            ItemStack stack = stack(ids[i]);
            if (!stack.isEmpty()) entity.setItemSlot(slot(i + 1), stack);
        }
    }

    /** The stack a pool id names, or empty when this port has no such item. */
    public static ItemStack stack(@Nullable String id) {
        if (id == null) return ItemStack.EMPTY;
        Identifier key = Ids.parse(id.contains(":") ? id : "hbm:" + id);
        if (!BuiltInRegistries.ITEM.containsKey(key)) return ItemStack.EMPTY;
        return new ItemStack(BuiltInRegistries.ITEM.getValue(key));
    }

    public static void assignItemsToEntity(LivingEntity entity, Map<Integer, List<Choice>> slotPools, RandomSource rand) {
        for (Map.Entry<Integer, List<Choice>> entry : slotPools.entrySet()) {
            int slot = entry.getKey();
            List<Choice> pool = entry.getValue();

            Choice choice = draw(pool, rand);
            if (choice == null || choice.id() == null) continue;
            Identifier id = Ids.parse(choice.id());
            if (!BuiltInRegistries.ITEM.containsKey(id)) continue;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));

            String path = id.getPath();
            if (path.equals("gas_mask_m65") || path.equals("gas_mask_olde") || path.equals("gas_mask_mono")) {
                ModDataComponents.GAS_MASK_FILTER.set(stack, new ItemStack(ModGearItems.gas_mask_filter.get()));
            }

            entity.setItemSlot(slot(slot), stack);

            // Give a skeleton the gun task if it has something in hand from the held pool
            if (slot == 0 && entity instanceof AbstractSkeleton mob && pool == slotPools.get(0)) {
                addFireTask(mob);
            }
        }
    }

    public static void addFireTask(Mob entity) {
        addFireTask(entity, new EntityAIFireGun(entity));
    }

    // 1.7: "these fucking tasks keep stacking on top of themselves"
    public static void addFireTask(Mob entity, EntityAIFireGun gunTask) {
        entity.setDropChance(EquipmentSlot.MAINHAND, 0); // Prevent dropping guns
        for (WrappedGoal task : Mobs.goals(entity).getAvailableGoals()) {
            if (task.getGoal() instanceof EntityAIFireGun) return;
        }
        Mobs.goals(entity).addGoal(3, gunTask);
    }
}
