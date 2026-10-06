package com.hbm.gametest;

import com.hbm.handler.EntityEffectHandler;
import com.hbm.handler.HazmatRegistry;
import com.hbm.items.ModArmorItems;
import com.hbm.items.gear.ArmorFSB;
import com.hbm.items.gear.ArmorFSBPowered;
import com.hbm.items.gear.IArmorDisableModel.EnumPlayerPart;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.ArmorRegistry.HazardClass;
import dev.golem.core.render.Worn;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/**
 * GameTests for the power-armour stack.
 *
 * <p>These exist because "the armour does nothing" is not one failure — the set bonus, the charge drain,
 * the configuration pass and the worn model each fail independently and look identical from inside the
 * game. Each test pins exactly one link in the chain so a break localises itself instead of needing a
 * playtest to bisect.
 */
public final class ArmorGameTests {

    private ArmorGameTests() {}

    /** A mock player standing in the structure. See {@code GunGameTests.player} for why the pos is set. */
    private static Player player(GameTestHelper helper) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        return player;
    }

    /** Dresses the player in the full T-51b set and returns the chestplate stack. */
    private static ItemStack suitUp(Player player) {
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModArmorItems.t51_helmet.get()));
        ItemStack chest = new ItemStack(ModArmorItems.t51_plate.get());
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModArmorItems.t51_legs.get()));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModArmorItems.t51_boots.get()));
        return chest;
    }

    /**
     * The configuration pass ran at all.
     *
     * <p>Everything else depends on this: {@code ModArmorItems.configure()} is what installs the effects,
     * the hazard classes, the radiation resistance, the hidden hat and the model name. If it never runs —
     * or runs before the items exist — the suit registers fine, wears fine, and does absolutely nothing.
     */
    public static void configured(GameTestHelper helper) {
        ArmorFSB helmet = (ArmorFSB) ModArmorItems.t51_helmet.get();

        helper.assertTrue(!helmet.effects.isEmpty(),
                "T-51b helmet has no effects: ModArmorItems.configure() did not run");
        helper.assertTrue(helmet.hardLanding, "hard landing not configured");
        helper.assertTrue(helmet.geigerSound, "geiger sound not configured");
        helper.assertTrue(helmet.vats, "VATS not configured");
        helper.assertTrue("t51".equals(helmet.armorModel()),
                "helmet's worn model is " + helmet.armorModel() + ", expected t51 — the suit would be invisible");

        // cloneStats must have carried the configuration onto the other three pieces.
        ArmorFSB plate = (ArmorFSB) ModArmorItems.t51_plate.get();
        helper.assertTrue(!plate.effects.isEmpty(), "chestplate did not receive cloneStats");
        helper.assertTrue("t51".equals(plate.armorModel()), "chestplate has no worn model");
        helper.succeed();
    }

    /** The full set is recognised, and a missing piece breaks it. */
    public static void setDetection(GameTestHelper helper) {
        Player player = player(helper);
        suitUp(player);

        helper.assertTrue(ArmorFSB.hasFSBArmor(player), "full T-51b set not detected by hasFSBArmor");
        helper.assertTrue(ArmorFSB.hasFSBArmorIgnoreCharge(player), "full set not detected ignoring charge");

        player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        helper.assertFalse(ArmorFSB.hasFSBArmor(player), "set still detected with the boots removed");
        helper.succeed();
    }

    /** Wearing the set applies the chestplate's effects, via the same tick path the game uses. */
    public static void setBonusApplies(GameTestHelper helper) {
        Player player = player(helper);
        suitUp(player);

        helper.assertTrue(player.getActiveEffects().isEmpty(), "player started with effects");
        EntityEffectHandler.onUpdate(player);

        helper.assertTrue(!player.getActiveEffects().isEmpty(),
                "no effect applied after a tick in a full set — the armour tick is not reaching ArmorFSB");
        helper.succeed();
    }

    /** A fresh suit reads as full, and the idle drain actually spends charge. */
    public static void chargeDrains(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack chest = suitUp(player);

        ArmorFSBPowered powered = (ArmorFSBPowered) chest.getItem();
        long max = powered.getMaxCharge(chest);
        helper.assertTrue(max == 1_000_000L, "max charge is " + max + ", expected 1,000,000");
        helper.assertTrue(powered.getCharge(chest) == max, "a fresh suit did not read as full");

        // 20 ticks of a complete set: drain is 5 HE/t, so exactly 100 HE.
        for (int i = 0; i < 20; i++) EntityEffectHandler.onUpdate(player);

        long spent = max - powered.getCharge(chest);
        helper.assertTrue(spent == 20L * powered.drain,
                "20 ticks drained " + spent + " HE, expected " + (20L * powered.drain));
        helper.succeed();
    }

    /** A flat suit keeps rendering but stops working — upstream's isArmorEnabled gate. */
    public static void flatSuitDisablesBonuses(GameTestHelper helper) {
        Player player = player(helper);
        ItemStack chest = suitUp(player);

        ((ArmorFSBPowered) chest.getItem()).setCharge(chest, 0L);

        helper.assertFalse(ArmorFSB.hasFSBArmor(player), "a flat suit still counts as a working set");
        helper.assertTrue(ArmorFSB.hasFSBArmorIgnoreCharge(player),
                "a flat suit stopped counting as worn — it would vanish instead of going dark");
        helper.succeed();
    }

    /** The radiation resistance and hazard classes reached their registries. */
    public static void protectionRegistered(GameTestHelper helper) {
        double helmetRes = HazmatRegistry.getResistance(new ItemStack(ModArmorItems.t51_helmet.get()));
        double chestRes = HazmatRegistry.getResistance(new ItemStack(ModArmorItems.t51_plate.get()));

        // setRadResist(1.0) split by the slot weights: helmet .2, chest .4.
        helper.assertTrue(Math.abs(helmetRes - 0.2D) < 1e-6,
                "helmet radiation resistance is " + helmetRes + ", expected 0.2");
        helper.assertTrue(Math.abs(chestRes - 0.4D) < 1e-6,
                "chest radiation resistance is " + chestRes + ", expected 0.4");

        var classes = ArmorRegistry.getProtectionFromItem(new ItemStack(ModArmorItems.t51_helmet.get()));
        helper.assertTrue(classes.contains(HazardClass.PARTICLE_FINE),
                "helmet does not protect against fine particles — FULL_NO_LIGHT was not registered");
        helper.succeed();
    }

    /**
     * The set bonus's effect is applied with its status <b>icon</b> showing.
     *
     * <p>Upstream hides the particles only. Modern Minecraft's five-argument {@code MobEffectInstance}
     * ties {@code showIcon} to {@code visible}, so the literal transcription hides the icon too — and an
     * effect with no particles and no icon is indistinguishable, in game, from one that never applied.
     * That is exactly what the first playtest reported as "strength doesn't apply".
     */
    public static void setBonusEffectIsVisible(GameTestHelper helper) {
        Player player = player(helper);
        suitUp(player);
        EntityEffectHandler.onUpdate(player);

        var effects = player.getActiveEffects();
        helper.assertTrue(!effects.isEmpty(), "no effect applied in a full set");

        for (var effect : effects) {
            helper.assertTrue(effect.showIcon(),
                    "the set-bonus effect has no status icon — it looks like nothing happened");
            helper.assertFalse(effect.isVisible(),
                    "the set-bonus effect shows particles; upstream suppresses them");
        }
        helper.succeed();
    }

    /**
     * The hard landing fires for a player who <b>may fly</b>, i.e. in creative.
     *
     * <p>{@code Player.causeFallDamage} returns early whenever the player may fly and never calls
     * {@code super}, so a hook on {@code LivingEntity.causeFallDamage} never runs in creative — which is
     * how most testing is done, and how this shipped broken. Upstream covers the same branch with
     * {@code PlayerFlyableFallEvent} alongside {@code LivingFallEvent}.
     *
     * <p>This drives {@code causeFallDamage} on a <b>creative</b> player rather than calling
     * {@code handleFall} directly, because the routing is the thing that broke — the shockwave itself was
     * always correct. A test that called {@code handleFall} would have passed throughout the bug.
     */
    public static void hardLandingThrowsEntities(GameTestHelper helper) {
        Player player = dev.golem.core.gametest.GameTestHelpers.mockPlayer(helper, GameType.CREATIVE);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        player.getAbilities().mayfly = true;
        suitUp(player);

        helper.assertTrue(player.getAbilities().mayfly,
                "the test player cannot fly, so it does not exercise the creative fall branch");

        net.minecraft.world.entity.LivingEntity victim =
                helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 1, 1));
        victim.setPos(player.getX() + 1.0D, player.getY(), player.getZ());
        victim.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);

        float health = victim.getHealth();
        // The shockwave finds its victims with a level query. On 26.1 a plot's entity section can still be
        // hidden from such queries on the test's first tick (a freshly loaded chunk): the pig exists but is not
        // returned yet, and the landing would hit nothing. Land once the query sees the pig (History 19es-final).
        net.minecraft.world.phys.AABB around = player.getBoundingBox().inflate(3, 0, 3);
        boolean[] landed = {false};
        helper.succeedWhen(() -> {
            if (!landed[0]) {
                helper.assertTrue(helper.getLevel().getEntities(player, around).contains(victim),
                        "the pig is not visible to level queries yet");
                landed[0] = true;
                // The real entry point. Player.causeFallDamage returns early for a flying player and never calls
                // super, which is why the hook cannot live on LivingEntity.
                dev.golem.core.world.Compat.causeFallDamage(player, 20F, 1F, player.damageSources().fall());
            }
            helper.assertTrue(victim.getDeltaMovement().horizontalDistanceSqr() > 0.0D,
                    "the hard landing did not throw a nearby entity — the creative fall path is not routed");
            helper.assertTrue(victim.getHealth() < health,
                    "the hard landing did no damage: " + victim.getHealth() + " vs " + health);
        });
    }

    /** A fall below the threshold does nothing at all — upstream's {@code fallDistance > 10} gate. */
    public static void shortFallDoesNothing(GameTestHelper helper) {
        Player player = player(helper);
        suitUp(player);

        net.minecraft.world.entity.LivingEntity victim =
                helper.spawn(net.minecraft.world.entity.EntityType.PIG, new BlockPos(1, 1, 1));
        victim.setPos(player.getX() + 1.0D, player.getY(), player.getZ());
        float health = victim.getHealth();

        ArmorFSB.handleFall(player, 5F);

        helper.assertTrue(victim.getHealth() == health,
                "a 5-block fall triggered the shockwave; the threshold is 10");
        helper.succeed();
    }

    /** The helmet asks for the hat layer to be hidden, which is what the render mixin reads. */
    public static void hidesHatLayer(GameTestHelper helper) {
        ItemStack helmet = new ItemStack(ModArmorItems.t51_helmet.get());
        Worn worn = new Worn(helmet, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);

        helper.assertTrue(
                ((ArmorFSB) helmet.getItem()).disablesPart(worn, helmet, EnumPlayerPart.HAT),
                "the helmet does not hide the hat layer");
        helper.assertFalse(
                ((ArmorFSB) helmet.getItem()).disablesPart(worn, helmet, EnumPlayerPart.BODY),
                "the helmet hides the body, which it should not");
        helper.succeed();
    }
}
