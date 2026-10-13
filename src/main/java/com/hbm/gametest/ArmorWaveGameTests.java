package com.hbm.gametest;

import com.hbm.blocks.machine.ModArmorTableBlocks;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModules;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.inventory.container.ContainerArmorTable;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.DamageResistanceHandler;
import dev.golem.core.gametest.GameTestHelpers;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;

/** Focused regression coverage for resistance math, module storage, and the armor table lifecycle. */
public final class ArmorWaveGameTests {

    private ArmorWaveGameTests() {}

    public static void resistancePrecedenceAndMath(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorItems.t51_helmet.get(), ModArmorItems.t51_plate.get(),
                ModArmorItems.t51_legs.get(), ModArmorItems.t51_boots.get());

        DamageSource attack = player.damageSources().playerAttack(player);
        float normal = DamageResistanceHandler.calculateDamage(player, attack, 10F, 0F, 0F);
        helper.assertTrue(Math.abs(normal - 6.8F) < 1.0E-5F,
                "T-51 physical resistance is " + normal + ", expected 6.8");

        float pierced = DamageResistanceHandler.calculateDamage(player, attack, 10F, 1F, 0.5F);
        helper.assertTrue(Math.abs(pierced - 8.325F) < 1.0E-5F,
                "pierced T-51 resistance is " + pierced + ", expected 8.325");

        DamageResistanceHandler.ResistanceStats precedence = new DamageResistanceHandler.ResistanceStats()
                .addExact("generic", 4F, 0.25F)
                .addCategory(DamageResistanceHandler.CATEGORY_PHYSICAL, 2F, 0.1F)
                .setOther(1F, 0.05F);
        DamageResistanceHandler.Resistance exact = precedence.getResistance(player.damageSources().generic());
        helper.assertTrue(exact.threshold() == 4F && exact.resistance() == 0.25F,
                "exact source did not take precedence");
        DamageResistanceHandler.Resistance category = precedence.getResistance(attack);
        helper.assertTrue(category.threshold() == 2F && category.resistance() == 0.1F,
                "category did not take precedence over other");

        wear(player, ModArmorItems.starmetal_helmet.get(), ModArmorItems.starmetal_plate.get(),
                ModArmorItems.starmetal_legs.get(), ModArmorItems.starmetal_boots.get());
        float starmetalPhysical = DamageResistanceHandler.calculateDamage(player, attack, 10F, 0F, 0F);
        helper.assertTrue(Math.abs(starmetalPhysical - 5.25F) < 1.0E-5F,
                "starmetal physical resistance is " + starmetalPhysical + ", expected 5.25");
        DamageResistanceHandler.Resistance other = new DamageResistanceHandler.ResistanceStats()
                .setOther(1F, 0.1F).getResistance(attack);
        helper.assertTrue(other.threshold() == 1F && other.resistance() == 0.1F,
                "non-absolute source did not resolve the other profile");
        helper.succeed();
    }

    public static void fullSetAndAbsorption(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorItems.t51_helmet.get(), ModArmorItems.t51_plate.get(),
                ModArmorItems.t51_legs.get(), ModArmorItems.t51_boots.get());
        DamageSource fall = player.damageSources().fall();

        helper.assertTrue(DamageResistanceHandler.calculateDamage(player, fall, 10F, 0F, 0F) == 0F,
                "T-51 exact fall resistance did not absorb the hit");
        float health = player.getHealth();
        int armorDamage = player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue();
        boolean accepted = DamageResistanceHandler.hurt((ServerLevel) player.level(), player, fall, 10F, 0F, 0F);
        helper.assertFalse(accepted, "a fully absorbed hit was not canceled");
        helper.assertTrue(player.getHealth() == health, "a canceled hit changed health");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.CHEST).getDamageValue() == armorDamage,
                "a fully absorbed hit damaged armor");

        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        helper.assertFalse(ArmorFSB.hasFSBArmor(player), "partial set still detected");
        helper.assertTrue(DamageResistanceHandler.getDTDR(player, fall, 10F, 0F, 0F)[1] == 0F,
                "partial set retained the full-set resistance");
        helper.succeed();
    }

    public static void familiesStatsAndHazards(GameTestHelper helper) {
        assertMaxDamage(helper, ModArmorItems.cobalt_helmet.get(), 770);
        assertMaxDamage(helper, ModArmorItems.starmetal_plate.get(), 2400);
        assertMaxDamage(helper, ModArmorItems.schrabidium_legs.get(), 1500);
        assertMaxDamage(helper, ModArmorItems.security_boots.get(), 1300);
        assertMaxDamage(helper, ModArmorItems.asbestos_plate.get(), 320);
        assertMaxDamage(helper, ModArmorItems.paa_legs.get(), 1125);

        helper.assertTrue(Math.abs(HazmatRegistry.getResistance(new ItemStack(ModArmorItems.cobalt_helmet.get()))
                - HazmatRegistry.cobalt * HazmatRegistry.helmet) < 1.0E-6,
                "cobalt radiation coefficient is incorrect");
        helper.assertTrue(ArmorRegistry.getProtectionFromItem(new ItemStack(ModArmorItems.schrabidium_helmet.get()))
                        .contains(ArmorRegistry.HazardClass.LIGHT),
                "schrabidium helmet is missing light protection");

        Player player = player(helper);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModArmorItems.paa_plate.get()));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModArmorItems.paa_legs.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModArmorItems.paa_boots.get()));
        helper.assertTrue(ArmorFSB.hasFSBArmor(player), "helmetless PAA set is not detected");
        helper.succeed();
    }

    public static void moduleStorageRoundTrip(GameTestHelper helper) {
        ItemStack armor = new ItemStack(ModArmorItems.t51_plate.get());
        ItemStack module = new ItemStack(ModArmorModules.armor_battery_mk2.get());
        ArmorModHandler.applyMod(armor, module);

        ItemStack copy = armor.copy();
        ItemStack restored = ArmorModHandler.pryMod(copy, ArmorModHandler.battery);
        helper.assertTrue(restored.getItem() == module.getItem() && restored.getCount() == 1,
                "armor module component did not round-trip");
        ArmorModHandler.clearMods(copy);
        helper.assertFalse(ArmorModHandler.hasMods(copy), "cleared armor still has module data");
        helper.succeed();
    }

    public static void tableSlotsAndLifecycle(GameTestHelper helper) {
        // Item dropping is a ServerPlayer operation on vanilla/Fabric, not the helper's bare Player.
        try (var fixture = GameTestHelpers.mockServerPlayer(helper)) {
        Player player = fixture.player();
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.setBlock(new BlockPos(1, 1, 1), ModArmorTableBlocks.ARMOR_TABLE.get().defaultBlockState());
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);

        ContainerArmorTable menu = new ContainerArmorTable(0, player.getInventory(), pos);
        ItemStack armor = new ItemStack(ModArmorItems.t51_plate.get());
        ItemStack insert = new ItemStack(ModArmorModules.insert_kevlar.get());
        ItemStack cladding = new ItemStack(ModArmorModules.cladding_lead.get());
        menu.getSlot(ContainerArmorTable.ARMOR_SLOT).setByPlayer(armor);

        helper.assertTrue(menu.stillValid(player), "armor table menu is not valid at its block");
        helper.assertTrue(menu.getSlot(ArmorModHandler.kevlar).mayPlace(insert),
                "valid chest insert rejected by its module slot");
        helper.assertFalse(menu.getSlot(ArmorModHandler.kevlar).mayPlace(cladding),
                "cladding accepted by the insert slot");
        menu.getSlot(ArmorModHandler.kevlar).setByPlayer(insert);
        helper.assertTrue(ArmorModHandler.pryMod(armor, ArmorModHandler.kevlar).getItem() == insert.getItem(),
                "module slot did not write armor storage");

        // Count only what closing dropped: a 4-block sphere reaches a neighbouring plot's drops when the
        // legacy framework packs batches tighter than 26.1 does.
        // Only stacks of the two dropped items count, and the count is polled: on 26.1 a plot's entity section
        // can still be hidden from level queries on the test's first tick (a freshly loaded chunk), so the
        // dropped entities exist but are not returned yet.
        AABB area = player.getBoundingBox().inflate(4D);
        Set<Integer> before = new HashSet<>();
        for (ItemEntity e : helper.getLevel().getEntitiesOfClass(ItemEntity.class, area)) before.add(e.getId());
        menu.removed(player);
        helper.succeedWhen(() -> {
            int drops = (int) helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).stream()
                    .filter(e -> !before.contains(e.getId()))
                    .filter(e -> e.getItem().is(armor.getItem()) || e.getItem().is(insert.getItem())).count();
            helper.assertTrue(drops == 2, "closing the table dropped " + drops + " stacks, expected armor + module");
        });
        }
    }

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return player;
    }

    private static void wear(Player player, Item helmet,
                             Item chest, Item legs,
                             Item boots) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chest));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
    }

    private static void assertMaxDamage(GameTestHelper helper, Item item, int expected) {
        helper.assertTrue(new ItemStack(item).getMaxDamage() == expected,
                "wrong armor durability for " + item.getDescriptionId());
    }
}
