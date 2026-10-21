package com.hbm.gametest;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import com.hbm.items.ModArmorModules;
import com.hbm.items.ModGunItems;
import com.hbm.items.ModMedicineItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.lib.ModDamageSources;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Compat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Release-audit checks for armor modules, the 1.7 damage types and the tau cannon's overcharge. */
public final class RcGear2GameTests {

    private RcGear2GameTests() {}

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return player;
    }

    /** A poison syringe on someone else is "euthanized" with the user as attacker; on yourself it is the self type. */
    public static void euthanizedAttacker(GameTestHelper helper) {
        Player user = player(helper);
        Player victim = player(helper);
        ItemStack syringe = new ItemStack(ModMedicineItems.syringe_poison.get());
        user.setItemInHand(InteractionHand.MAIN_HAND, syringe);
        ModMedicineItems.syringe_poison.get().hurtEnemy(syringe, victim, user);
        DamageSource source = victim.getLastDamageSource();
        helper.assertTrue(victim.isDeadOrDying(), "30 damage kills a fresh player");
        helper.assertTrue(source != null && "euthanized".equals(source.getMsgId()) && source.getEntity() == user,
                "poison on another entity is hbm:euthanized credited to the user, got " + source);

        ItemStack second = new ItemStack(ModMedicineItems.syringe_poison.get());
        user.setItemInHand(InteractionHand.MAIN_HAND, second);
        ModMedicineItems.syringe_poison.get().use(user.level(), user, InteractionHand.MAIN_HAND);
        DamageSource self = user.getLastDamageSource();
        helper.assertTrue(self != null && self.getMsgId().startsWith("euthanizedSelf"),
                "poison on yourself keeps the self types, got " + self);
        helper.succeed();
    }

    /** Every 1.7 damage type resolves with its 1.7 flags. */
    public static void damageTypes(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        for (ResourceKey<DamageType> key : List.of(ModDamageSources.EUTHANIZED, ModDamageSources.TAU_BLAST,
                ModDamageSources.ELECTRIFIED, ModDamageSources.PLASMA, ModDamageSources.SUICIDE,
                ModDamageSources.TELEPORTER, ModDamageSources.SUBATOMIC_1, ModDamageSources.SUBATOMIC_5)) {
            helper.assertTrue(ModDamageSources.source(level, key) != null, key + " resolves");
        }
        DamageSource blast = ModDamageSources.tauBlast(level);
        helper.assertTrue("tauBlast".equals(blast.getMsgId()), "tau blast message id " + blast.getMsgId());
        Player player = player(helper);
        for (int i = 0; i < 20; i++) {
            String id = ModDamageSources.subatomic(level, player, player).getMsgId();
            helper.assertTrue(id.matches("subAtomic[1-5]"), "subatomic message id " + id);
        }
        helper.succeed();
    }

    /** Holding the tau cannon's charge past 200 ticks blows the shooter up and wears the gun. */
    public static void tauOvercharge(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack gun = new ItemStack(ModGunItems.gun_tau.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, gun);
        player.getInventory().add(new ItemStack(ModAmmoItems.tau_uranium.get(), 30));
        ItemGunBaseNT item = (ItemGunBaseNT) gun.getItem();
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 40; i++) item.serverTick(gun, level, player, true);

        item.handleKeybind(player, player.getInventory(), gun, EnumKeybind.GUN_SECONDARY, true);
        for (int i = 0; i < 100; i++) item.serverTick(gun, level, player, true);
        helper.assertTrue(player.isAlive(), "still alive after 100 ticks of charge");
        int left = count(player, ModAmmoItems.tau_uranium.get());
        helper.assertTrue(left == 30 - 10, "the charge spends one round every 10 ticks, left " + left);

        for (int i = 0; i < 50; i++) item.serverTick(gun, level, player, true);
        left = count(player, ModAmmoItems.tau_uranium.get());
        helper.assertTrue(left == 30 - 13, "13 rounds spent in all (charge stops at 130), left " + left);
        for (int i = 0; i < 60; i++) item.serverTick(gun, level, player, true);
        helper.assertTrue(player.isDeadOrDying(), "the overcharge kills the shooter");
        DamageSource source = player.getLastDamageSource();
        helper.assertTrue(source != null && "tauBlast".equals(source.getMsgId()), "killed by the tau blast, got " + source);
        helper.assertTrue(ItemGunBaseNT.getWear(gun, 0) >= 6_400F, "the blast adds 10,000 wear, capped at the tau cannon's 6400 durability, got " + ItemGunBaseNT.getWear(gun, 0));
        helper.succeed();
    }

    /** A released charge fires with the rounds already spent and spends no more. */
    public static void tauChargedShot(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack gun = new ItemStack(ModGunItems.gun_tau.get());
        player.setItemSlot(EquipmentSlot.MAINHAND, gun);
        player.getInventory().add(new ItemStack(ModAmmoItems.tau_uranium.get(), 30));
        ItemGunBaseNT item = (ItemGunBaseNT) gun.getItem();
        ServerLevel level = helper.getLevel();
        for (int i = 0; i < 40; i++) item.serverTick(gun, level, player, true);
        item.handleKeybind(player, player.getInventory(), gun, EnumKeybind.GUN_SECONDARY, true);
        for (int i = 0; i < 35; i++) item.serverTick(gun, level, player, true);
        int spent = 30 - count(player, ModAmmoItems.tau_uranium.get());
        helper.assertTrue(spent == 4, "35 ticks of charge spend rounds at timers 0, 10, 20, 30, spent " + spent);
        item.handleKeybind(player, player.getInventory(), gun, EnumKeybind.GUN_SECONDARY, false);
        helper.assertTrue(player.isAlive() && ItemGunBaseNT.getWear(gun, 0) > 0F, "the release fired the beam and wore the gun");
        helper.assertTrue(30 - count(player, ModAmmoItems.tau_uranium.get()) == 4, "the release spends nothing more");
        helper.succeed();
    }

    private static int count(Player player, Item item) {
        int total = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }

    /** Insert numbers, speed modifiers and the polonium and ERA behaviour from 1.7. */
    public static void inserts(GameTestHelper helper) {
        helper.assertTrue(ModArmorModules.cladding_rubber.get().rad == 0.005D, "rubber cladding is +0.005");
        Player player = player(helper);
        ItemStack chest = new ItemStack(ModArmorItems.steel_plate.get());
        ArmorModHandler.applyMod(chest, new ItemStack(ModArmorModules.insert_steel.get()));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        double base = player.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        ArmorModEvents.onLivingUpdate(player);
        double speed = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(speed - base * 0.95D) < 1.0E-6D, "steel insert slows by 5%, got " + speed + " of " + base);
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ArmorModEvents.onLivingUpdate(player);
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - base) < 1.0E-6D, "speed restored");

        ItemStack polonium = new ItemStack(ModArmorItems.steel_plate.get());
        ArmorModHandler.applyMod(polonium, new ItemStack(ModArmorModules.insert_polonium.get()));
        player.setItemSlot(EquipmentSlot.CHEST, polonium);
        HbmLivingProps.setRadiation(player, 0D);
        for (int i = 0; i < 20; i++) ArmorModEvents.onLivingUpdate(player);
        helper.assertTrue(Math.abs(HbmLivingProps.getRadiation(player) - 2000D) < 1.0E-6D,
                "polonium insert adds 100 RAD per tick, 2000 per second: " + HbmLivingProps.getRadiation(player));
        List<Component> lines = new ArrayList<>();
        ModArmorModules.insert_polonium.get().addArmorModDescription(lines,
                new ItemStack(ModArmorModules.insert_polonium.get()), polonium);
        helper.assertTrue(lines.stream().anyMatch(c -> c.getString().contains("+2000 RAD/s")),
                "polonium description shows the per-second rate");

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ItemStack era = new ItemStack(ModArmorItems.steel_plate.get());
        ArmorModHandler.applyMod(era, new ItemStack(ModArmorModules.insert_era.get()));
        player.setItemSlot(EquipmentSlot.CHEST, era);
        HbmLivingProps.setRadiation(player, 0D);
        player.invulnerableTime = 0;
        Compat.hurt(player, helper.getLevel(), ModDamageSources.source(helper.getLevel(), ModDamageSources.LEAD), 1F);
        ItemStack left = ArmorModHandler.pryMod(player.getItemBySlot(EquipmentSlot.CHEST), ArmorModHandler.kevlar);
        helper.assertTrue(left.getDamageValue() == 1, "a hit wears the ERA insert by one, got " + left.getDamageValue());
        helper.succeed();
    }

    /** Charms and auto-injector. */
    public static void charmsAndInjector(GameTestHelper helper) {
        ArmorModEvents.init();
        ServerLevel level = helper.getLevel();
        Player player = player(helper);
        ItemStack helmet = new ItemStack(ModArmorItems.steel_helmet.get());
        ArmorModHandler.applyMod(helmet, new ItemStack(ModArmorModItems.meteor_charm.get()));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        player.invulnerableTime = 0;
        float before = player.getHealth();
        Compat.hurt(player, level, ModDamageSources.source(level, ModDamageSources.BROADCAST), 10F);
        helper.assertTrue(player.getHealth() == before, "the meteor charm negates broadcaster damage, health " + player.getHealth());

        ItemStack protection = new ItemStack(ModArmorItems.steel_helmet.get());
        ArmorModHandler.applyMod(protection, new ItemStack(ModArmorModItems.protection_charm.get()));
        player.setItemSlot(EquipmentSlot.HEAD, protection);
        player.invulnerableTime = 0;
        Compat.hurt(player, level, ModDamageSources.source(level, ModDamageSources.BROADCAST), 10F);
        helper.assertTrue(Math.abs(player.getHealth() - (before - 5F)) < 1.0E-4F,
                "the protection charm halves broadcaster damage, health " + player.getHealth());

        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        ItemStack legs = new ItemStack(ModArmorItems.steel_legs.get());
        ArmorModHandler.applyMod(legs, new ItemStack(ModArmorModItems.injector_5htp.get()));
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        HbmLivingProps.setDigamma(player, 5F);
        player.setHealth(5F);
        ArmorModEvents.onLivingUpdate(player);
        helper.assertTrue(ArmorModHandler.pryMod(player.getItemBySlot(EquipmentSlot.LEGS), ArmorModHandler.extra).isEmpty(),
                "the 5-HTP injector is used up");
        helper.assertTrue(HbmLivingProps.getDigamma(player) == 0F, "it purges 5 digamma");
        helper.assertTrue(player.getHealth() == 20F, "and heals 20, health " + player.getHealth());
        helper.succeed();
    }
}
