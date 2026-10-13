package com.hbm.gametest;

import com.hbm.Constants;
import com.hbm.client.render.weapon.DaniStaticLayout;
import com.hbm.client.render.weapon.G3StaticLayout;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.handler.HbmKeybinds.EnumKeybind;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.ModGunItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryBlackPowder;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import dev.golem.core.gametest.GameTestHelpers;
import dev.golem.core.gametest.GameTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** Focused behavior checks for the ordinary Gen-3 firearm continuation. */
public final class Gen3SednaGameTests {

    private Gen3SednaGameTests() {}

    public static final GameTests.Suite TESTS = GameTests.suite(Constants.MOD_ID);
    public static final GameTests.Function AMMO_IDENTITY =
            TESTS.function("gen3_sedna_ammo_identity", 20, () -> Gen3SednaGameTests::ammoIdentity);
    public static final GameTests.Function GUN_CONFIGS =
            TESTS.function("gen3_sedna_gun_configs", 20, () -> Gen3SednaGameTests::gunConfigs);
    public static final GameTests.Function DANI_STATE =
            TESTS.function("gen3_sedna_dani_state", 20, () -> Gen3SednaGameTests::daniStatePersistsIndependently);
    public static final GameTests.Function EMPTY_FIRE =
            TESTS.function("gen3_sedna_empty_fire", 100, () -> Gen3SednaGameTests::emptyGunsDoNotFire);
    public static final GameTests.Function G3_MODE =
            TESTS.function("gen3_sedna_g3_mode", 100, () -> Gen3SednaGameTests::g3TriggerMode);
    public static final GameTests.Function DANI_STATIC_LAYOUT =
            TESTS.function("gen3_sedna_dani_static_layout", 20, () -> Gen3SednaGameTests::daniStaticLayout);
    public static final GameTests.Function G3_STATIC_LAYOUT =
            TESTS.function("gen3_sedna_g3_static_layout", 20, () -> Gen3SednaGameTests::g3StaticLayout);
    public static final GameTests.Function G3_FIRE =
            TESTS.function("gen3_sedna_g3_loaded_fire", 100, () -> Gen3SednaGameTests::g3LoadedFireAndRefire);

    public static void init() {}

    public static void ammoIdentity(GameTestHelper helper) {
        BulletConfig[] blackPowder = XFactoryBlackPowder.all();
        BulletConfig[] r556 = XFactory556mm.all();
        helper.assertTrue(blackPowder.length == 4 && r556.length == 4,
                "the Pepperbox and G3 ammo families must be complete");
        for (BulletConfig config : blackPowder) {
            helper.assertTrue(config.id != null && BulletConfigs.get(config.id) == config && config.blackPowder,
                    "black-powder config identity/flag mismatch: " + config.id);
        }
        for (BulletConfig config : r556) {
            helper.assertTrue(config.id != null && BulletConfigs.get(config.id) == config,
                    "5.56 config identity mismatch: " + config.id);
        }
        helper.assertTrue(blackPowder[0].ammoItem() == ModAmmoItems.stone.get()
                        && blackPowder[1].ammoItem() == ModAmmoItems.stone_ap.get()
                        && blackPowder[2].ammoItem() == ModAmmoItems.stone_iron.get()
                        && blackPowder[3].ammoItem() == ModAmmoItems.stone_shot.get(),
                "Pepperbox accepted-ammo item order must match upstream");
        helper.assertTrue(r556[0].ammoItem() == ModAmmoItems.r556_sp.get()
                        && r556[1].ammoItem() == ModAmmoItems.r556_fmj.get()
                        && r556[2].ammoItem() == ModAmmoItems.r556_jhp.get()
                        && r556[3].ammoItem() == ModAmmoItems.r556_ap.get(),
                "G3 accepted-ammo item order must match upstream");
        helper.assertTrue(XFactoryBlackPowder.shot.projectilesMin == 6
                        && XFactoryBlackPowder.shot.projectilesMax == 6
                        && XFactoryBlackPowder.shot.damageMult == 1F / 6F,
                "Pepperbox shot must remain a six-projectile load");
        helper.assertTrue(XFactory556mm.r556_ap.doesPenetrate
                        && !XFactory556mm.r556_ap.damageFalloffByPen
                        && XFactory556mm.r556_ap.armorThresholdNegation == 10F,
                "5.56 AP stats must match upstream");
        helper.succeed();
    }

    public static void gunConfigs(GameTestHelper helper) {
        assertReceiver(helper, ModGunItems.gun_light_revolver_atlas.get(), 0,
                12.5F, 11, 11, 55, 45, 6, // 1.7.10 delay 11 (16 was 1.12.2)
                XFactory357.m357_bp, false);
        assertReceiver(helper, ModGunItems.gun_pepperbox.get(), 0,
                5F, 27, 27, 67, 58, 6, XFactoryBlackPowder.stone, false);
        assertReceiver(helper, ModGunItems.gun_g3.get(), 0,
                5F, 2, 15, 50, 47, 30, XFactory556mm.r556_sp, true);
        helper.assertTrue(ModGunItems.gun_light_revolver.get().defaultAmmo.get() == ModAmmoItems.m357_sp.get()
                        && ModGunItems.gun_light_revolver.get().defaultAmmoAmount == 12
                        && ModGunItems.gun_g3.get().defaultAmmo.get() == ModAmmoItems.r556_sp.get()
                        && ModGunItems.gun_g3.get().defaultAmmoAmount == 30
                        && ModGunItems.gun_fatman.get().defaultAmmo.get() == ModAmmoItems.nuke_standard.get()
                        && ModGunItems.gun_fatman.get().isDefaultExpensive,
                "source default ammunition must remain attached to the registered gun item");
        ItemStack stinger = new ItemStack(ModGunItems.gun_stinger.get());
        helper.assertTrue(ModGunItems.gun_stinger.get().getConfig(stinger, 0).getPressSecondary(stinger) != null
                        && ModGunItems.gun_stinger.get().getConfig(stinger, 0).getReleaseSecondary(stinger) != null,
                "Stinger lock-on must remain an explicit source secondary handler");

        ItemStack dani = new ItemStack(ModGunItems.gun_light_revolver_dani.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) dani.getItem();
        helper.assertTrue(gun.isValidConfigIndex(0) && gun.isValidConfigIndex(1),
                "Day And Night must expose two independent configs");
        for (int index = 0; index < 2; index++) {
            Receiver receiver = gun.getConfig(dani, index).getReceivers(dani)[0];
            helper.assertTrue(receiver.getBaseDamage(dani) == 15F
                            && receiver.getDelayAfterFire(dani) == 11
                            && receiver.getHipfireSpread(dani) == 0F
                            && receiver.getMagazine(dani).getCapacity(dani) == 6,
                    "Day And Night receiver " + index + " must match upstream");
        }
        helper.succeed();
    }

    private static void assertReceiver(GameTestHelper helper, ItemGunBaseNT gun, int configIndex,
            float damage, int fire, int dry, int reload, int jam, int capacity,
            BulletConfig firstAccepted, boolean automatic) {
        ItemStack stack = new ItemStack(gun);
        Receiver receiver = gun.getConfig(stack, configIndex).getReceivers(stack)[0];
        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) receiver.getMagazine(stack);
        helper.assertTrue(receiver.getBaseDamage(stack) == damage
                        && receiver.getDelayAfterFire(stack) == fire
                        && receiver.getDelayAfterDryFire(stack) == dry
                        && receiver.getReloadBeginDuration(stack) == reload
                        && receiver.getReloadCycleDuration(stack) == reload
                        && receiver.getJamDuration(stack) == jam
                        && magazine.getCapacity(stack) == capacity
                        && magazine.acceptedBullets.get(0) == firstAccepted
                        && receiver.getRefireOnHold(stack) == automatic,
                "receiver stats or accepted-ammo order differ from upstream for " + gun);
    }

    public static void daniStatePersistsIndependently(GameTestHelper helper) {
        ItemStack stack = new ItemStack(ModGunItems.gun_light_revolver_dani.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        MagazineSingleTypeBase left = (MagazineSingleTypeBase) gun.getPrimaryMagazine(stack, 0);
        MagazineSingleTypeBase right = (MagazineSingleTypeBase) gun.getPrimaryMagazine(stack, 1);
        left.setType(stack, XFactory357.m357_jhp);
        right.setType(stack, XFactory357.m357_ap);
        left.setAmount(stack, 2);
        right.setAmount(stack, 5);
        ItemGunBaseNT.setWear(stack, 0, 12F);
        ItemGunBaseNT.setWear(stack, 1, 34F);

        ItemStack copy = stack.copy();
        helper.assertTrue(MagazineSingleTypeBase.getMagCount(copy, 0) == 2
                        && MagazineSingleTypeBase.getMagCount(copy, 1) == 5
                        && gun.getPrimaryMagazine(copy, 0).getType(copy, null)
                                == XFactory357.m357_jhp
                        && gun.getPrimaryMagazine(copy, 1).getType(copy, null)
                                == XFactory357.m357_ap
                        && ItemGunBaseNT.getWear(copy, 0) == 12F
                        && ItemGunBaseNT.getWear(copy, 1) == 34F,
                "both Day And Night receivers must survive copy without sharing state");
        helper.succeed();
    }

    public static void daniStaticLayout(GameTestHelper helper) {
        List<DaniStaticLayout.Draw> expectedThirdPersonRight = List.of(
                DaniStaticLayout.Draw.THIRD_PERSON_RIGHT_LUNAR,
                DaniStaticLayout.Draw.THIRD_PERSON_LEFT_LUNAR);
        List<DaniStaticLayout.Draw> expectedThirdPersonLeft = List.of(
                DaniStaticLayout.Draw.THIRD_PERSON_LEFT_LUNAR,
                DaniStaticLayout.Draw.THIRD_PERSON_RIGHT_LUNAR);
        helper.assertTrue(DaniStaticLayout.forContext(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)
                        .equals(expectedThirdPersonRight)
                        && DaniStaticLayout.forContext(ItemDisplayContext.THIRD_PERSON_LEFT_HAND)
                        .equals(expectedThirdPersonLeft),
                "Day And Night must submit one lunar revolver for each third-person hand");
        helper.assertTrue(DaniStaticLayout.forContext(ItemDisplayContext.GUI).equals(List.of(
                        DaniStaticLayout.Draw.GUI_CELESTIAL, DaniStaticLayout.Draw.GUI_LUNAR))
                        && DaniStaticLayout.forContext(ItemDisplayContext.GROUND)
                        .equals(List.of(DaniStaticLayout.Draw.CELESTIAL))
                        && DaniStaticLayout.forContext(ItemDisplayContext.HEAD)
                        .equals(List.of(DaniStaticLayout.Draw.LUNAR)),
                "Day And Night static context decisions must match upstream renderInv/renderOther/renderEquipped");
        helper.succeed();
    }

    public static void g3StaticLayout(GameTestHelper helper) {
        helper.assertTrue(G3StaticLayout.PARTS.equals(List.of(
                        "Rifle", "Stock", "Magazine", "Flash_Hider",
                        "Guide_And_Bolt", "Handle", "Trigger"))
                        && !G3StaticLayout.PARTS.contains("Plug"),
                "G3 renderOther must omit the first-person-only animated Plug");
        helper.succeed();
    }

    public static void emptyGunsDoNotFire(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        for (ItemGunBaseNT gun : List.of(ModGunItems.gun_light_revolver_atlas.get(),
                ModGunItems.gun_light_revolver_dani.get(), ModGunItems.gun_pepperbox.get(),
                ModGunItems.gun_g3.get())) {
            ItemStack stack = new ItemStack(gun);
            player.setItemSlot(EquipmentSlot.MAINHAND, stack);
            for (int i = 0; i < 40; i++) gun.serverTick(stack, helper.getLevel(), player, true);
            int before = helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                    player.getBoundingBox().inflate(64)).size();
            gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
            int after = helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                    player.getBoundingBox().inflate(64)).size();
            helper.assertTrue(after == before && MagazineSingleTypeBase.getMagCount(stack, 0) == 0,
                    "empty gun fired or consumed negative ammo: " + gun);
        }
        helper.succeed();
    }

    public static void g3TriggerMode(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModGunItems.gun_g3.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        for (int i = 0; i < 40; i++) gun.serverTick(stack, helper.getLevel(), player, true);

        helper.assertTrue(ItemGunBaseNT.getMode(stack, 0) == 0, "G3 must start in automatic mode");
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_SECONDARY, true);
        helper.assertTrue(ItemGunBaseNT.getMode(stack, 0) == 1,
                "secondary trigger must switch G3 to semi-automatic mode");
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_SECONDARY, false);
        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_SECONDARY, true);
        helper.assertTrue(ItemGunBaseNT.getMode(stack, 0) == 0,
                "a second secondary press must switch G3 back to automatic mode");
        helper.succeed();
    }

    public static void g3LoadedFireAndRefire(GameTestHelper helper) {
        Player player = GameTestHelpers.mockPlayer(helper, GameType.SURVIVAL);
        BlockPos origin = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(origin.getX() + 0.5D, origin.getY(), origin.getZ() + 0.5D);
        ItemStack stack = new ItemStack(ModGunItems.gun_g3.get());
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        player.setItemSlot(EquipmentSlot.MAINHAND, stack);
        for (int i = 0; i < 40; i++) gun.serverTick(stack, helper.getLevel(), player, true);

        MagazineSingleTypeBase magazine = (MagazineSingleTypeBase) gun.getPrimaryMagazine(stack, 0);
        magazine.setType(stack, XFactory556mm.r556_sp);
        magazine.setAmount(stack, 4);
        int before = helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                player.getBoundingBox().inflate(64D)).size();

        gun.handleKeybind(player, player.getInventory(), stack, EnumKeybind.GUN_PRIMARY, true);
        List<EntityBulletBaseMK4> firstShot = helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                player.getBoundingBox().inflate(64D));
        helper.assertTrue(firstShot.size() == before + 1
                        && firstShot.get(firstShot.size() - 1).config == XFactory556mm.r556_sp,
                "Loaded G3 trigger did not spawn one soft-point projectile");
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 3,
                "Loaded G3 trigger did not consume exactly one round");

        gun.serverTick(stack, helper.getLevel(), player, true);
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 3,
                "G3 refired before its exact two-tick cooldown elapsed");
        gun.serverTick(stack, helper.getLevel(), player, true);
        List<EntityBulletBaseMK4> refired = helper.getLevel().getEntitiesOfClass(EntityBulletBaseMK4.class,
                player.getBoundingBox().inflate(64D));
        helper.assertTrue(magazine.getAmount(stack, player.getInventory()) == 2
                        && refired.size() == before + 2
                        && refired.get(refired.size() - 1).config == XFactory556mm.r556_sp,
                "Held automatic G3 did not refire one projectile after cooldown");
        helper.succeed();
    }
}
