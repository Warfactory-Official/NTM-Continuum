package com.hbm.gametest;

import com.hbm.handler.ArmorDashHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.ModArmorSetItems;
import com.hbm.items.armor.ArmorEuphemium;
import com.hbm.items.armor.ArmorTrenchmaster;
import com.hbm.items.armor.ArmorFSB;
import com.hbm.items.armor.ArmorFSBPowered;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.world.Effects;
import dev.golem.core.world.StepHeight;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/**
 * GameTests for the final armour sets (part A): each suit's configuration pass, and the behaviours that are
 * this family's own - euphemium's god-mode tick, the Trenchmaster's gun hooks and unbreakability, the
 * bismuth dash stamina, Fau's digamma protection and the step-size bonus. Each assertion is a value from
 * the 1.7 declaration, not "something happened".
 */
public final class ArmorSetGameTests {

    private ArmorSetGameTests() {}

    private static Player player(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }

    private static void wear(Player player, Item helmet, Item plate, Item legs, Item boots) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(plate));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(legs));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(boots));
    }

    /** Every modelled suit was configured and carries its worn model onto all four pieces. */
    public static void configured(GameTestHelper helper) {
        ArmorFSB cmb = ModArmorSetItems.cmb_boots.get();
        helper.assertTrue(cmb.effects.size() == 3, "cmb boots carry " + cmb.effects.size() + " effects, expected 3");
        ArmorFSB bismuth = ModArmorSetItems.bismuth_legs.get();
        helper.assertTrue(bismuth.dashCount == 3, "bismuth dash count " + bismuth.dashCount + ", expected 3");
        helper.assertTrue(bismuth.effects.size() == 4, "bismuth effects " + bismuth.effects.size() + ", expected 4");
        helper.assertTrue(ModArmorSetItems.hev_plate.get().customGeiger, "HEV lost its geiger HUD flag");
        helper.assertTrue(ModArmorSetItems.fau_boots.get().thermal, "Fau lost thermal sight");
        helper.assertTrue(ModArmorSetItems.trenchmaster_boots.get().stepSize == 1, "Trenchmaster step size not 1");
        helper.assertTrue(ModArmorSetItems.ajro_legs.get().vats && ModArmorSetItems.ajro_legs.get().hardLanding,
                "AJRO lost VATS/hard landing");
        String[][] models = {
                {"bismuth", "bismuth_boots"}, {"ajr", "ajr_plate"}, {"ajro", "ajro_boots"}, {"hev", "hev_legs"},
                {"fau", "fau_plate"}, {"trenchmaster", "trenchmaster_legs"}};
        ArmorFSB[] pieces = {ModArmorSetItems.bismuth_boots.get(), ModArmorSetItems.ajr_plate.get(),
                ModArmorSetItems.ajro_boots.get(), ModArmorSetItems.hev_legs.get(), ModArmorSetItems.fau_plate.get(),
                ModArmorSetItems.trenchmaster_legs.get()};
        for (int i = 0; i < pieces.length; i++) {
            helper.assertTrue(models[i][0].equals(pieces[i].armorModel()),
                    models[i][1] + " worn model is " + pieces[i].armorModel() + ", expected " + models[i][0]);
        }
        // HazmatRegistry: cmb 1.3 split by the chest weight .4
        double chest = HazmatRegistry.getResistance(new ItemStack(ModArmorSetItems.cmb_plate.get()));
        helper.assertTrue(Math.abs(chest - 0.52D) < 1e-6, "cmb chest rad resistance " + chest + ", expected 0.52");
        helper.succeed();
    }

    /** The AJR drains 25 HE/t, the HEV and Fau suits have no idle drain. */
    public static void powerDrain(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorSetItems.ajr_helmet.get(), ModArmorSetItems.ajr_plate.get(),
                ModArmorSetItems.ajr_legs.get(), ModArmorSetItems.ajr_boots.get());
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        ArmorFSBPowered ajr = (ArmorFSBPowered) chest.getItem();
        helper.assertTrue(ajr.getMaxCharge(chest) == 2_500_000L, "AJR capacity " + ajr.getMaxCharge(chest));
        for (int i = 0; i < 20; i++) EntityEffectHandler.onUpdate(player);
        long spent = 2_500_000L - ajr.getCharge(chest);
        helper.assertTrue(spent == 500L, "20 AJR ticks drained " + spent + " HE, expected 500");

        wear(player, ModArmorSetItems.hev_helmet.get(), ModArmorSetItems.hev_plate.get(),
                ModArmorSetItems.hev_legs.get(), ModArmorSetItems.hev_boots.get());
        ItemStack hev = player.getItemBySlot(EquipmentSlot.CHEST);
        for (int i = 0; i < 20; i++) EntityEffectHandler.onUpdate(player);
        ArmorFSBPowered hevItem = (ArmorFSBPowered) hev.getItem();
        helper.assertTrue(hevItem.getCharge(hev) == 1_000_000L, "HEV drained without an idle drain");
        helper.assertTrue(Effects.has(player, Effects.speed()), "HEV set gave no speed");
        helper.succeed();
    }

    /** Full euphemium: level-128 regeneration/resistance and the -0.25 fall clamp. */
    public static void euphemium(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorSetItems.euphemium_helmet.get(), ModArmorSetItems.euphemium_plate.get(),
                ModArmorSetItems.euphemium_legs.get(), ModArmorSetItems.euphemium_boots.get());
        helper.assertTrue(ArmorEuphemium.hasFullSet(player), "euphemium set not detected");
        player.setDeltaMovement(new Vec3(0, -1.5D, 0));
        player.fallDistance = 20F;
        EntityEffectHandler.onUpdate(player);
        helper.assertTrue(Effects.has(player, MobEffects.REGENERATION), "no regeneration from euphemium");
        helper.assertTrue(Effects.get(player, Effects.resistance()) != null
                && Effects.get(player, Effects.resistance()).getAmplifier() == 127, "no resistance 127 from euphemium");
        helper.assertTrue(player.getDeltaMovement().y >= -0.25D, "fall speed not clamped: " + player.getDeltaMovement().y);
        helper.assertTrue(player.fallDistance == 0F, "fall distance not cancelled");
        helper.assertFalse(new ItemStack(ModArmorSetItems.euphemium_plate.get()).isDamageableItem(),
                "euphemium is damageable; upstream's setDamage was a no-op");
        helper.succeed();
    }

    /** Trenchmaster: the gun hook sees him, the suit never wears, and he gets his step height. */
    public static void trenchmaster(GameTestHelper helper) {
        Player player = player(helper);
        helper.assertFalse(ArmorTrenchmaster.isTrenchMaster(player), "a naked player is a trenchmaster");
        wear(player, ModArmorSetItems.trenchmaster_helmet.get(), ModArmorSetItems.trenchmaster_plate.get(),
                ModArmorSetItems.trenchmaster_legs.get(), ModArmorSetItems.trenchmaster_boots.get());
        helper.assertTrue(ArmorTrenchmaster.isTrenchMaster(player), "full trenchmaster set not recognised");
        helper.assertFalse(new ItemStack(ModArmorSetItems.trenchmaster_helmet.get()).isDamageableItem(),
                "trenchmaster is damageable; upstream set max damage 0");
        EntityEffectHandler.onUpdate(player);
        double step = StepHeight.get(player);
        helper.assertTrue(Math.abs(step - 1.003D) < 1e-4, "trenchmaster step height " + step + ", expected 1.003");
        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        EntityEffectHandler.onUpdate(player);
        helper.assertTrue(StepHeight.get(player) < 1D, "step height stayed after the set broke: " + StepHeight.get(player));
        helper.succeed();
    }

    /** Bismuth: three dashes of 30 stamina, refilled one point a tick; a dash spends 30. */
    public static void bismuthDash(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorSetItems.bismuth_helmet.get(), ModArmorSetItems.bismuth_plate.get(),
                ModArmorSetItems.bismuth_legs.get(), ModArmorSetItems.bismuth_boots.get());
        for (int i = 0; i < 100; i++) EntityEffectHandler.onUpdate(player);
        ArmorDashHandler.State state = ArmorDashHandler.state(player);
        helper.assertTrue(state.totalDashCount == 3, "bismuth dash count " + state.totalDashCount);
        helper.assertTrue(state.stamina == 90, "stamina after 100 ticks is " + state.stamina + ", expected full 90");
        ArmorDashHandler.setKeyPressed(player, true);
        EntityEffectHandler.onUpdate(player);
        helper.assertTrue(state.dashCooldown == ArmorDashHandler.DASH_COOLDOWN_LENGTH, "dash did not start a cooldown");
        helper.assertTrue(state.stamina == 61, "stamina after a dash is " + state.stamina + ", expected 60 + 1 refill");
        helper.assertTrue(Effects.has(player, Effects.jumpBoost()), "bismuth gave no jump boost");
        helper.succeed();
    }

    /** Fau (and DNT-nano) are the digamma suits; a partial set is not. */
    public static void digamma(GameTestHelper helper) {
        Player player = player(helper);
        wear(player, ModArmorSetItems.fau_helmet.get(), ModArmorSetItems.fau_plate.get(),
                ModArmorSetItems.fau_legs.get(), ModArmorSetItems.fau_boots.get());
        helper.assertTrue(ArmorUtil.checkForDigamma(player), "full Fau set does not protect from digamma");
        helper.assertTrue(ArmorUtil.checkForHaz2(player), "full Fau set is not heavy hazmat");
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        helper.assertFalse(ArmorUtil.checkForDigamma(player), "partial Fau set protects from digamma");
        helper.succeed();
    }
}
