package com.hbm.gametest;

import com.hbm.capability.HbmLivingProps;
import com.hbm.handler.ArmorModEvents;
import com.hbm.handler.ArmorModHandler;
import com.hbm.items.ModArmorItems;
import com.hbm.items.ModArmorModItems;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Module behaviour on the real living-update and death routes: modifiers, per-tick effects, revives. */
public final class ArmorModGameTests {

    private ArmorModGameTests() {}

    /** Heart container, iron cladding and servos add their modifiers while worn and drop them when not. */
    public static void modifiersFollowTheWornStack(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack chest = new ItemStack(ModArmorItems.steel_plate.get());
        ArmorModHandler.applyMod(chest, new ItemStack(ModArmorModItems.heart_container.get()));
        ArmorModHandler.applyMod(chest, new ItemStack(ModArmorModItems.cladding_iron.get()));
        ArmorModHandler.applyMod(chest, new ItemStack(ModArmorModItems.servo_set.get()));
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ArmorModEvents.onLivingUpdate(player);

        helper.assertTrue(Math.abs(player.getMaxHealth() - 40F) < 1.0E-4F, "heart container max health " + player.getMaxHealth());
        double kb = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        helper.assertTrue(Math.abs(kb - 0.5D) < 1.0E-6D, "iron cladding knockback resistance " + kb);
        double damage = player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs(damage - 1.5D) < 1.0E-6D, "servo attack damage " + damage);
        helper.assertTrue(Effects.has(player, Effects.haste()), "servo haste missing");

        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        ArmorModEvents.onLivingUpdate(player);
        helper.assertTrue(Math.abs(player.getMaxHealth() - 20F) < 1.0E-4F, "max health not restored " + player.getMaxHealth());
        kb = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        helper.assertTrue(kb == 0D, "knockback resistance not restored " + kb);
        helper.succeed();
    }

    /** The liquidator medal removes 0.5 RAD per tick and spider milk strips poison. */
    public static void medalAndMilkTick(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack helmet = new ItemStack(ModArmorItems.steel_helmet.get());
        ArmorModHandler.applyMod(helmet, new ItemStack(ModArmorModItems.medal_liquidator.get()));
        ArmorModHandler.applyMod(helmet, new ItemStack(ModArmorModItems.spider_milk.get()));
        player.setItemSlot(EquipmentSlot.HEAD, helmet);
        HbmLivingProps.setRadiation(player, 100D);
        Effects.add(player, MobEffects.POISON, 200, 0);
        for (int i = 0; i < 10; i++) ArmorModEvents.onLivingUpdate(player);
        double rad = HbmLivingProps.getRadiation(player);
        helper.assertTrue(Math.abs(rad - 95D) < 1.0E-6D, "medal radiation after 10 ticks " + rad);
        helper.assertFalse(Effects.has(player, MobEffects.POISON), "spider milk left poison active");
        helper.succeed();
    }

    /** Wild Pegasus revives three times, then is gone; the fourth lethal hit kills. */
    public static void whiskeyRevivesThreeTimes(GameTestHelper helper) {
        Player player = player(helper);
        ArmorModEvents.init();
        ItemStack legs = new ItemStack(ModArmorItems.steel_legs.get());
        ArmorModHandler.applyMod(legs, new ItemStack(ModArmorModItems.wild_p.get()));
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        ServerLevel level = (ServerLevel) player.level();
        for (int i = 0; i < 3; i++) {
            Effects.clear(player);
            player.invulnerableTime = 0;
            Compat.hurt(player, level, player.damageSources().generic(), 1000F);
            helper.assertTrue(player.isAlive() && player.getHealth() == player.getMaxHealth(),
                    "revive " + (i + 1) + " failed, health " + player.getHealth());
        }
        helper.assertTrue(ArmorModHandler.pryMod(player.getItemBySlot(EquipmentSlot.LEGS), ArmorModHandler.extra).isEmpty(),
                "the whiskey was not used up after three revives");
        helper.succeed();
    }

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        return player;
    }
}
