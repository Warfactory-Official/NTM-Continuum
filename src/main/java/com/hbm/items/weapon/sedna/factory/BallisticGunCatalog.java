package com.hbm.items.weapon.sedna.factory;

import com.hbm.config.RunningConfig.ConfigWrapper;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.ItemEnums.EnumCasingType;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.factory.Orchestras;
import com.hbm.sound.ModGunSounds;
import com.hbm.sound.ModSounds;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;

/**
 * Registry declarations for the conventional Gen-3 guns.
 *
 * <p>The original factories constructed {@code ModItems} fields during bootstrap. New Minecraft
 * requires those fields to be deferred registry entries, so the factory-owned declarations live here
 * instead of coupling the calibre code to the shared item catalogue. Every config below is copied from
 * 1.7; absent engine surfaces are not represented as a different weapon.
 */
public final class BallisticGunCatalog {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_BMG_SMOKE =
            (stack, ctx) -> Lego.handleStandardSmoke(ctx.entity, stack, 2000, 0.05D, 1.1D, ctx.hand, 0);

    public static final RegistryObject<ItemGunBaseNT> gun_g3_zebra = gun("gun_g3_zebra", WeaponQuality.B_SIDE,
            ModAmmoItems.r556_jhp, 30,
            () -> new GunConfig().dura(6_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .scopeTexture(Ids.of(Constants.MOD_ID, "textures/misc/scope_bolt.png"))
                    .rec(new Receiver(0).dmg(XFactory556mm.GUN_ZEBRA_DAMAGE).delay(2).dry(15).auto(true).spreadHipfire(0.01F).reload(50).jam(47)
                            .sound(ModGunSounds.FIRE_SILENCED::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory556mm.incendiary()))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125),
                                     (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.125))))
                     .setupStandardConfiguration().ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY));

    public static final RegistryObject<ItemGunBaseNT> gun_stg77 = gun("gun_stg77", WeaponQuality.A_SIDE,
            ModAmmoItems.r556_fmj, 30,
            () -> new GunConfig().dura(3_000).draw(10).inspect(125).crosshair(Crosshair.CIRCLE)
                    .scopeTexture(Ids.of(Constants.MOD_ID, "textures/misc/scope_bolt.png"))
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory556mm.GUN_STG77_DAMAGE).delay(2).dry(15).auto(true).reload(46).jam(0)
                            .sound(ModGunSounds.FIRE_ASSAULT::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory556mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> { })) // 1.7 LAMBDA_RECOIL_STG is empty
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                    .pr(Lego.LAMBDA_STANDARD_RELOAD).pt(Lego.LAMBDA_TOGGLE_AIM)
                    .decider((stack, ctx) -> {
                        int index = ctx.configIndex;
                        ItemGunBaseNT.GunState last = ItemGunBaseNT.getState(stack, index);
                        GunStateDecider.deciderStandardFinishDraw(stack, last, index);
                        GunStateDecider.deciderStandardClearJam(stack, last, index);
                        GunStateDecider.deciderStandardReload(stack, ctx, last, 0, index);
                        GunStateDecider.deciderAutoRefire(stack, ctx, last, 0, index,
                                () -> ItemGunBaseNT.getSecondary(stack, index));
                    }));

    public static final RegistryObject<ItemGunBaseNT> gun_carbine = gun("gun_carbine", WeaponQuality.A_SIDE,
            ModAmmoItems.r762_sp, 14,
            () -> new GunConfig().dura(3_000).draw(10).inspect(31).reloadSequential(true)
                    .smoke(XFactory762mm.LAMBDA_SMOKE)
                    .crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(XFactory762mm.GUN_CARBINE_DAMAGE).delay(5).dry(15).spread(0F).reload(30, 0, 15, 0).jam(60)
                            .sound(ModGunSounds.FIRE_BLACK_POWDER::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 14).addConfigs(XFactory762mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_mas36 = gun("gun_mas36", WeaponQuality.LEGENDARY,
            ModAmmoItems.r762_ap, 14,
            () -> new GunConfig().dura(5_000).draw(20).inspect(31).reloadSequential(true)
                    .smoke(XFactory762mm.LAMBDA_SMOKE)
                    .crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(XFactory762mm.GUN_SOUTH_STAR_DAMAGE).delay(25).dry(25).spread(0F).reload(43).jam(43)
                            .sound(ModSounds.FIRE_RIFLE_HEAVY::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 7).addConfigs(XFactory762mm.all()))
                            .offset(1, -0.0625 * 1.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(5F,
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_minigun = gun("gun_minigun", WeaponQuality.A_SIDE,
            ModAmmoItems.r762_fmj, 30,
            () -> new GunConfig().dura(50_000).draw(20).inspect(20)
                    .smoke(XFactory762mm.LAMBDA_SMOKE)
                    .crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0).dmg(XFactory762mm.GUN_MINIGUN_DAMAGE).delay(1).auto(true).dry(15).spread(0.01F)
                             .sound(ModGunSounds.FIRE_MINIGUN::get, 1F, 1F)
                            .mag(new MagazineBelt().addConfigs(XFactory762mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                                     (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                      .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_minigun_lacunae = gun("gun_minigun_lacunae",
            WeaponQuality.LEGENDARY, ModAmmoItems.capacitor, 15,
            () -> new GunConfig().dura(50_000).draw(20).inspect(20)
                    .crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0).dmg(XFactory762mm.GUN_LACUNAE_DAMAGE).delay(1).auto(true).dry(15).reload(15).spread(0.01F)
                             .sound(ModGunSounds.FIRE_LASER_GATLING::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 200).addConfigs(XFactory762mm.energy_lacunae,
                                    XFactory762mm.energy_lacunae_overcharge, XFactory762mm.energy_lacunae_ir))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil(XFactory762mm.LAMBDA_RECOIL_LACUNAE))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_minigun_dual = gun("gun_minigun_dual", WeaponQuality.DEBUG,
            ModAmmoItems.r762_sp, 50,
            () -> minigunConfig(0.25D, XFactory762mm.GUN_MINIGUN_DUAL_LEFT_DAMAGE),
                    () -> minigunConfig(-0.25D, XFactory762mm.GUN_MINIGUN_DUAL_RIGHT_DAMAGE)
                    .pp(null).ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(XFactory762mm.LAMBDA_SECOND_MINIGUN));

    public static final RegistryObject<ItemGunBaseNT> gun_m2 = gun("gun_m2", WeaponQuality.A_SIDE,
            ModAmmoItems.bmg50_fmj, 25,
            () -> new GunConfig().dura(3_000).draw(10).inspect(31)
                    .crosshair(Crosshair.L_CIRCLE)
                    .smoke(LAMBDA_BMG_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory50.GUN_M2_DAMAGE).delay(2).dry(10).auto(true).spread(0.005F)
                            // 1.7 NTMSounds.TURRET_50BMG ("hbm:turret.chekhov_fire"); belt feeds six .50 types, no starmetal.
                            .sound(ModSounds.TURRET_CHEKHOV_FIRE::get, 1F, 1F)
                            .mag(new MagazineBelt().addConfigs(XFactory50.bmg50_sp, XFactory50.bmg50_fmj,
                                    XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_he))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_maresleg = gun("gun_maresleg", WeaponQuality.A_SIDE,
            ModAmmoItems.g12, 12,
            () -> shotgun(600F, XFactory12ga.GUN_MARES_LEG_DAMAGE, 20, 22, 10, 13, 0, 6, XFactory12ga.all(), 24)
                    .reloadSequential(true).crosshair(Crosshair.L_CIRCLE));

    public static final RegistryObject<ItemGunBaseNT> gun_maresleg_akimbo = gun("gun_maresleg_akimbo", WeaponQuality.B_SIDE,
            ModAmmoItems.g12, 24,
            () -> shotgun(600F, XFactory12ga.GUN_MARES_LEG_AKIMBO_LEFT_DAMAGE, 20, 22, 10, 13, 0, 0, 6, XFactory12ga.all(), 0F, 1.35F, 0.1875D, 24,
                    0, 5, 39, ModGunSounds.FIRE_SHOTGUN::get, XFactory12ga.LAMBDA_RECOIL_MARESLEG)
                    .reloadSequential(true).crosshair(Crosshair.L_CIRCLE).pt(null)
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
            () -> shotgun(600F, XFactory12ga.GUN_MARES_LEG_AKIMBO_RIGHT_DAMAGE, 20, 22, 10, 13, 0, 0, 6, XFactory12ga.all(), 0F, 1.35F, -0.1875D, 24,
                    1, 5, 39, ModGunSounds.FIRE_SHOTGUN::get, XFactory12ga.LAMBDA_RECOIL_MARESLEG)
                    .reloadSequential(true).crosshair(Crosshair.L_CIRCLE).pt(null).pp(null)
                    .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));

    public static final RegistryObject<ItemGunBaseNT> gun_maresleg_broken = gun("gun_maresleg_broken", WeaponQuality.LEGENDARY,
            ModAmmoItems.g12_magnum, 24,
            () -> new GunConfig().dura(0).draw(5).inspect(39).reloadSequential(true)
                    .crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory12ga.GUN_BROKEN_DAMAGE).spreadAmmo(1.15F).delay(20).reload(22, 10, 13, 0).jam(24)
                            .sound(ModGunSounds.FIRE_SHOTGUN::get, 1F, 1F)
                            .mag(new MagazineSingleReload(0, 6).addConfigs(XFactory12ga.g12_equestrian_tkr,
                                    XFactory12ga.g12_bp, XFactory12ga.g12_bp_magnum, XFactory12ga.g12_bp_slug,
                                    XFactory12ga.g12, XFactory12ga.g12_slug, XFactory12ga.g12_flechette,
                                    XFactory12ga.g12_magnum, XFactory12ga.g12_explosive, XFactory12ga.g12_phosphorus))
                            .offset(0.75, -0.0625, -0.1875).canFire(Lego.LAMBDA_STANDARD_CAN_FIRE)
                            .fire(Lego.LAMBDA_NOWEAR_FIRE)
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_liberator = gun("gun_liberator", WeaponQuality.A_SIDE,
            ModAmmoItems.g12, 12,
            () -> {
                GunConfig config = shotgun(200F, XFactory12ga.GUN_LIBERATOR_DAMAGE, 20, 25, 15, 7, 0, 0, 4, XFactory12ga.all(), 0.025F, 1F, -0.1875D, 45,
                        0, 20, 21, ModGunSounds.FIRE_LIBERATOR::get, XFactory12ga.LAMBDA_RECOIL_LIBERATOR)
                        .reloadSequential(true)
                        .crosshair(Crosshair.L_CIRCLE);
                // 1.7 XFactory12ga: .rounds(4) - one trigger pull fires all four barrels.
                config.getReceivers(ItemStack.EMPTY)[0].rounds(4);
                return config;
            });

    public static final RegistryObject<ItemGunBaseNT> gun_spas12 = gun("gun_spas12", WeaponQuality.A_SIDE,
            ModAmmoItems.g12, 16,
            () -> shotgun(600F, XFactory12ga.GUN_SPAS_DAMAGE, 20, 10, 10, 10, 0, 5, 8, XFactory12ga.all(), 0F, 1F, -0.1875D, 36,
                    0, 20, 39, ModGunSounds.FIRE_SPAS::get, XFactory12ga.LAMBDA_RECOIL_MARESLEG)
                    .reloadSequential(true)
                    .reloadChangeType(true).crosshair(Crosshair.L_CIRCLE)
                    .ps(XFactory12ga.LAMBDA_SPAS_SECONDARY).pt(null));

    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun = gun("gun_autoshotgun", WeaponQuality.A_SIDE,
            ModAmmoItems.g12, 20,
            () -> new GunConfig().dura(2_000).draw(10).inspect(33).reloadSequential(true)
                    .crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory12ga.GUN_AUTOSHOTGUN_DAMAGE).delay(10).auto(true).autoAfterDry(true).dryfireAfterAuto(true)
                    .reload(44).jam(19).sound(ModGunSounds.FIRE_SHREDDER::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 20).addConfigs(XFactory12ga.all()))
                            .offset(0.75, -0.125, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5) + 1.5F,
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                     .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun_shredder = gun(
            "gun_autoshotgun_shredder", WeaponQuality.B_SIDE, ModAmmoItems.g12, 20,
             () -> new GunConfig().dura(2_000).draw(10).inspect(33).reloadSequential(true)
                    .crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory12ga.GUN_SHREDDER_DAMAGE).delay(10).auto(true).autoAfterDry(true).dryfireAfterAuto(true)
                            .reload(44).jam(19).sound(ModGunSounds.FIRE_SHREDDER::get, 1F, 1F)
                            .mag(new MagazineBelt().addConfigs(XFactory12ga.g12_shredder,
                                    XFactory12ga.g12_shredder_slug, XFactory12ga.g12_shredder_flechette,
                                    XFactory12ga.g12_shredder_magnum, XFactory12ga.g12_shredder_explosive,
                                    XFactory12ga.g12_shredder_phosphorus))
                            .offset(0.75, -0.125, -0.25D).setupStandardFire()
                            .recoil(XFactory12ga.LAMBDA_RECOIL_AUTOSHOTGUN))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun_sexy = gun("gun_autoshotgun_sexy", WeaponQuality.LEGENDARY,
            ModAmmoItems.g12_magnum, 50,
            () -> new GunConfig().dura(5_000).draw(20).inspect(65).inspectCancel(false).reloadSequential(true)
                    .crosshair(Crosshair.L_CIRCLE).hideCrosshair(false)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory12ga.GUN_SEXY_DAMAGE).delay(4).auto(true).dryfireAfterAuto(true).reload(110).jam(19)
                            .sound(ModGunSounds.FIRE_SHREDDER::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 100).addConfigs(XFactory12ga.g12_equestrian_bj,
                                    XFactory12ga.g12_bp, XFactory12ga.g12_bp_magnum, XFactory12ga.g12_bp_slug,
                                    XFactory12ga.g12, XFactory12ga.g12_slug, XFactory12ga.g12_flechette,
                                    XFactory12ga.g12_magnum, XFactory12ga.g12_explosive, XFactory12ga.g12_phosphorus))
                            .offset(0.75, -0.125, -0.25D).canFire(Lego.LAMBDA_STANDARD_CAN_FIRE)
                            .fire(Lego.LAMBDA_NOWEAR_FIRE)
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_flaregun = gun("gun_flaregun", WeaponQuality.A_SIDE,
            ModAmmoItems.g26_flare, 3,
            () -> new GunConfig().dura(100).draw(7).inspect(39)
                    .crosshair(Crosshair.L_CIRCUMFLEX)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory40mm.GUN_FLARE_DAMAGE).delay(20).reload(28).jam(33)
                            .sound(ModGunSounds.FIRE_UNDERBARREL::get, 1F, 1F)
                            .mag(new MagazineSingleReload(0, 1).addConfigs(XFactory40mm.g26_flare,
                                    XFactory40mm.g26_flare_supply, XFactory40mm.g26_flare_weapon))
                            .offset(0.75, -0.0625, -0.1875D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_congolake = gun("gun_congolake", WeaponQuality.A_SIDE,
            ModAmmoItems.g40_he, 8,
            () -> new GunConfig().dura(400).draw(7).inspect(39).reloadSequential(true).reloadChangeType(true)
                    .crosshair(Crosshair.L_CIRCUMFLEX)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory40mm.GUN_CONGO_LAKE_DAMAGE).delay(24).reload(16, 16, 16, 0).jam(0)
                            .sound(ModGunSounds.FIRE_CONGO::get, 1F, 1F)
                            .mag(new MagazineSingleReload(0, 4).addConfigs(XFactory40mm.all40()))
                            .offset(0.75, -0.0625, -0.1875D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_mk108 = gun("gun_mk108", WeaponQuality.A_SIDE,
            ModAmmoItems.g40_he, 50,
            () -> new GunConfig().dura(5_000).draw(20).inspect(65)
                    .crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
                    .rec(new Receiver(0).dmg(XFactory40mm.GUN_MK108_DAMAGE).delay(10).auto(true).dryfireAfterAuto(true).reload(135).jam(25)
                            .sound(ModGunSounds.FIRE_MK108::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory40mm.all40()))
                            .offset(0.75, -0.125, -0.125D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1F) + 1F,
                                    (float) ctx.getPlayer().getRandom().nextGaussian())))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_amat = gun("gun_amat", WeaponQuality.A_SIDE,
            ModAmmoItems.bmg50_sp, 7,
            () -> bmgGun(350F, XFactory50.GUN_ANTI_MATERIEL_DAMAGE, new BulletConfig[] {
                    XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp, XFactory50.bmg50_ap,
                    XFactory50.bmg50_du, XFactory50.bmg50_sm, XFactory50.bmg50_he}, 7, ModGunSounds.FIRE_AMAT::get)
                    .scopeTexture(Ids.of(Constants.MOD_ID, "textures/misc/scope_amat.png")));
    public static final RegistryObject<ItemGunBaseNT> gun_amat_subtlety = gun("gun_amat_subtlety", WeaponQuality.LEGENDARY,
            ModAmmoItems.bmg50_jhp, 7,
            () -> bmgGun(1_000F, XFactory50.GUN_SUBTLETY_DAMAGE, new BulletConfig[] {
                    XFactory50.bmg50_equestrian, XFactory50.bmg50_sp, XFactory50.bmg50_fmj,
                    XFactory50.bmg50_jhp, XFactory50.bmg50_ap, XFactory50.bmg50_du,
                    XFactory50.bmg50_sm, XFactory50.bmg50_he}, 7, ModGunSounds.FIRE_AMAT::get)
                    .scopeTexture(Ids.of(Constants.MOD_ID, "textures/misc/scope_amat.png")));
    public static final RegistryObject<ItemGunBaseNT> gun_amat_penance = gun("gun_amat_penance", WeaponQuality.LEGENDARY,
            ModAmmoItems.bmg50_jhp, 7,
            () -> {
                GunConfig config = bmgGun(5_000F, XFactory50.GUN_PENANCE_DAMAGE, new BulletConfig[] {
                        XFactory50.bmg50_sp, XFactory50.bmg50_fmj, XFactory50.bmg50_jhp,
                        XFactory50.bmg50_ap, XFactory50.bmg50_du, XFactory50.bmg50_sm,
                        XFactory50.bmg50_he, XFactory50.bmg50_black}, 7, ModGunSounds.FIRE_SILENCER::get)
                        .scopeTexture(Ids.of(Constants.MOD_ID, "textures/misc/scope_penance.png"))
                        .thermalSights(true);
                // 1.7 XFactory50: the Penance has no hipfire spread penalty (the other two have 0.05).
                config.getReceivers(ItemStack.EMPTY)[0].spreadHipfire(0F);
                return config;
            });

    public static final RegistryObject<ItemGunBaseNT> gun_bolter = gun("gun_bolter", WeaponQuality.SPECIAL,
            ModAmmoItems.b75, 15,
            () -> new GunConfig().dura(3_000).draw(20).inspect(31)
                    .crosshair(Crosshair.L_CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory75Bolt.GUN_BOLTER_DAMAGE).delay(2).auto(true).spread(0.005F).reload(40).jam(55)
                            .sound(ModGunSounds.FIRE_BLACK_POWDER::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory75Bolt.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D).setupStandardFire()
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5),
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_double_barrel = gun("gun_double_barrel", WeaponQuality.SPECIAL,
            ModAmmoItems.g10, 6,
            () -> doubleBarrel(1_000F, XFactory10ga.GUN_DOUBLE_BARREL_DAMAGE, 0.9F, XFactory10ga.all()));
    public static final RegistryObject<ItemGunBaseNT> gun_double_barrel_sacred_dragon = gun(
            "gun_double_barrel_sacred_dragon", WeaponQuality.B_SIDE, ModAmmoItems.g10_du, 6,
            () -> doubleBarrel(6_000F, XFactory10ga.GUN_SACRED_DRAGON_DAMAGE, 0.9F, XFactory10ga.all(), 1.35F));
    /** 1.12.2's occult (jade) shotgun, {@link XFactory10ga#jadeConfig}. */
    public static final RegistryObject<ItemGunBaseNT> gun_jshotgun = gun("gun_jshotgun", WeaponQuality.A_SIDE,
            XFactory10ga::jadeConfig);
    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun_heretic = gun(
            "gun_autoshotgun_heretic", WeaponQuality.DEBUG, ModAmmoItems.g10, 50,
            () -> new GunConfig().draw(20).inspect(65).inspectCancel(false).reloadSequential(true)
                    .crosshair(Crosshair.L_CIRCLE).hideCrosshair(false)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(XFactory10ga.GUN_HERETIC_DAMAGE).delay(3).auto(true).dryfireAfterAuto(true).reload(110).jam(19)
                            .sound(ModGunSounds.FIRE_SHREDDER::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 250).addConfigs(XFactory10ga.all()))
                            .offset(0.75, -0.125, -0.25D).canFire(Lego.LAMBDA_STANDARD_CAN_FIRE)
                            .fire(Lego.LAMBDA_NOWEAR_FIRE)
                            .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                                    (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                    .setupStandardConfiguration());

    public static final RegistryObject<ItemGunBaseNT> gun_aberrator = gun("gun_aberrator", WeaponQuality.SECRET,
            () -> beamGun(0, -0.1875D, false, XFactory35800.GUN_ABERRATOR_DAMAGE).setupStandardConfiguration());
    /**
     * 1.7 {@code XFactory35800}: two independently triggered aberrators, each with its own five-round magazine
     * (indices 0 and 1), no hipfire spread penalty and no aim toggle; the left one (config 0) sits at +0.1875.
     */
    public static final RegistryObject<ItemGunBaseNT> gun_aberrator_eott = gun("gun_aberrator_eott", WeaponQuality.SECRET,
            () -> beamGun(0, 0.1875D, true, XFactory35800.GUN_EOTT_LEFT_DAMAGE).pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
            () -> beamGun(1, -0.1875D, true, XFactory35800.GUN_EOTT_RIGHT_DAMAGE).ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));

    private BallisticGunCatalog() {}

    private static boolean initialized;

    /** Explicit bootstrap hook for loaders that want to touch the catalogue directly. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        for (RegistryObject<ItemGunBaseNT> gun : new RegistryObject[] {
                gun_g3_zebra, gun_stg77, gun_carbine, gun_mas36, gun_minigun, gun_minigun_lacunae, gun_minigun_dual, gun_m2,
                gun_maresleg, gun_maresleg_akimbo,
                gun_maresleg_broken, gun_liberator, gun_spas12, gun_autoshotgun, gun_autoshotgun_shredder, gun_autoshotgun_sexy,
                gun_flaregun, gun_congolake, gun_mk108, gun_amat, gun_amat_subtlety, gun_amat_penance,
                gun_bolter, gun_double_barrel, gun_double_barrel_sacred_dragon, gun_autoshotgun_heretic,
                gun_jshotgun}) {
            ModTabs.addTo(HbmTab.WEAPONS, gun);
        }
    }

    public static Supplier<Item> ammo(String id) {
        return ModAmmoItems.ammo(id);
    }

    private static RegistryObject<ItemGunBaseNT> gun(String id, WeaponQuality quality,
            Supplier<GunConfig>... configs) {
        return REG.item(id, props -> {
            installCasings();
            GunConfig[] values = new GunConfig[configs.length];
            for (int i = 0; i < configs.length; i++) {
                values[i] = configs[i].get().orchestra(orchestraFor(id));
            }
            return new ItemGunBaseNT(props, quality, values).setNameMutator(nameMutatorFor(id));
        }, p -> p);
    }

    private static RegistryObject<ItemGunBaseNT> gun(String id, WeaponQuality quality,
            Supplier<? extends Item> defaultAmmo, int defaultAmount, Supplier<GunConfig>... configs) {
        return REG.item(id, props -> {
            installCasings();
            GunConfig[] values = new GunConfig[configs.length];
            for (int i = 0; i < configs.length; i++) {
                values[i] = configs[i].get().orchestra(orchestraFor(id));
            }
            return new ItemGunBaseNT(props, quality, values)
                    .setDefaultAmmo(defaultAmmo, defaultAmount).setNameMutator(nameMutatorFor(id));
        }, p -> p);
    }

    /** Binds the shared casing identities after the casing registry has flushed, before gun construction. */
    private static void installCasings() {
        setCasing(EnumCasingType.SMALL, 16, XFactory357.m357_bp);
        setCasing(EnumCasingType.SMALL, 8, XFactory357.m357_sp,
                XFactory357.m357_fmj,
                XFactory357.m357_jhp,
                XFactory357.m357_express);
        setCasing(EnumCasingType.SMALL_STEEL, 8, XFactory357.m357_ap);
        setCasing(EnumCasingType.SMALL, 12, XFactory44.m44_bp);
        setCasing(EnumCasingType.SMALL, 6, XFactory44.m44_sp,
                XFactory44.m44_fmj,
                XFactory44.m44_jhp,
                XFactory44.m44_express);
        setCasing(EnumCasingType.SMALL_STEEL, 6, XFactory44.m44_ap);
        setCasing(EnumCasingType.SMALL, 12, XFactory9mm.p9_sp,
                XFactory9mm.p9_fmj,
                XFactory9mm.p9_jhp);
        setCasing(EnumCasingType.SMALL_STEEL, 12, XFactory9mm.p9_ap);
        setCasing(EnumCasingType.SMALL, 24, XFactory22lr.p22_sp,
                XFactory22lr.p22_fmj,
                XFactory22lr.p22_jhp);
        setCasing(EnumCasingType.SMALL_STEEL, 24, XFactory22lr.p22_ap);
        setCasing(EnumCasingType.SMALL, 8, XFactory556mm.r556_sp, XFactory556mm.r556_fmj,
                XFactory556mm.r556_jhp, XFactory556mm.r556_inc_sp, XFactory556mm.r556_inc_fmj,
                XFactory556mm.r556_inc_jhp);
        setCasing(EnumCasingType.SMALL_STEEL, 8, XFactory556mm.r556_ap, XFactory556mm.r556_inc_ap);
        setCasing(EnumCasingType.SMALL, 8, XFactory45.p45_sp, XFactory45.p45_fmj, XFactory45.p45_jhp);
        setCasing(EnumCasingType.SMALL_STEEL, 8, XFactory45.p45_ap, XFactory45.p45_du);
        setCasing(EnumCasingType.SMALL, 6, XFactory762mm.r762_sp, XFactory762mm.r762_fmj,
                XFactory762mm.r762_jhp);
        setCasing(EnumCasingType.SMALL_STEEL, 6, XFactory762mm.r762_ap, XFactory762mm.r762_du,
                XFactory762mm.r762_he);
        setCasing(EnumCasingType.SHOTSHELL, 12, XFactory12ga.g12_bp, XFactory12ga.g12_bp_magnum,
                XFactory12ga.g12_bp_slug);
        setCasing(EnumCasingType.BUCKSHOT, 6, XFactory12ga.g12, XFactory12ga.g12_slug,
                XFactory12ga.g12_flechette);
        setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 6, XFactory12ga.g12_magnum,
                XFactory12ga.g12_explosive, XFactory12ga.g12_phosphorus);
        setCasing(EnumCasingType.LARGE, 4, XFactory40mm.g26_flare, XFactory40mm.g26_flare_supply,
                XFactory40mm.g26_flare_weapon, XFactory40mm.g40_he, XFactory40mm.g40_heat,
                XFactory40mm.g40_demo, XFactory40mm.g40_inc, XFactory40mm.g40_phosphorus);
        setCasing(EnumCasingType.LARGE, 12, XFactory50.bmg50_sp, XFactory50.bmg50_fmj,
                XFactory50.bmg50_jhp);
        setCasing(EnumCasingType.LARGE_STEEL, 12, XFactory50.bmg50_ap, XFactory50.bmg50_du,
                XFactory50.bmg50_he);
        setCasing(EnumCasingType.LARGE_STEEL, 6, XFactory50.bmg50_sm);
        setCasing(EnumCasingType.BUCKSHOT_ADVANCED, 4, XFactory10ga.g10, XFactory10ga.g10_shrapnel,
                XFactory10ga.g10_du, XFactory10ga.g10_slug, XFactory10ga.g10_explosive);
    }

    private static void setCasing(EnumCasingType type, int amount,
            BulletConfig... configs) {
        for (BulletConfig config : configs) config.setCasing(type, amount);
    }

    private static BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext>
            orchestraFor(String id) {
        return switch (id) {
            case "gun_g3_zebra" -> Orchestras.ORCHESTRA_G3;
            case "gun_stg77" -> Orchestras.ORCHESTRA_STG77;
            case "gun_carbine" -> Orchestras.ORCHESTRA_CARBINE;
            case "gun_mas36" -> Orchestras.ORCHESTRA_MAS36;
            case "gun_minigun", "gun_minigun_lacunae" -> Orchestras.ORCHESTRA_MINIGUN;
            case "gun_minigun_dual" -> Orchestras.ORCHESTRA_MINIGUN_DUAL;
            case "gun_maresleg" -> Orchestras.ORCHESTRA_MARESLEG;
            case "gun_maresleg_akimbo" -> Orchestras.ORCHESTRA_MARESLEG_AKIMBO;
            case "gun_maresleg_broken" -> Orchestras.ORCHESTRA_MARESLEG_SHORT;
            case "gun_liberator" -> Orchestras.ORCHESTRA_LIBERATOR;
            case "gun_spas12" -> Orchestras.ORCHESTRA_SPAS;
            case "gun_autoshotgun", "gun_autoshotgun_shredder", "gun_autoshotgun_sexy" ->
                    Orchestras.ORCHESTRA_SHREDDER;
            case "gun_autoshotgun_heretic" -> Orchestras.ORCHESTRA_SHREDDER_SEXY;
            case "gun_flaregun" -> Orchestras.ORCHESTRA_FLAREGUN;
            case "gun_congolake" -> Orchestras.ORCHESTRA_CONGOLAKE;
            case "gun_mk108" -> Orchestras.ORCHESTRA_MK108;
            case "gun_amat", "gun_amat_subtlety", "gun_amat_penance" -> Orchestras.ORCHESTRA_AMAT;
            case "gun_m2" -> Orchestras.ORCHESTRA_M2;
            case "gun_bolter" -> Orchestras.ORCHESTRA_BOLTER;
            case "gun_double_barrel", "gun_double_barrel_sacred_dragon" -> Orchestras.ORCHESTRA_DOUBLE_BARREL;
            case "gun_aberrator", "gun_aberrator_eott" -> Orchestras.ORCHESTRA_ABERRATOR;
            default -> (stack, ctx) -> { };
        };
    }

    /** The 1.7 {@code setNameMutator} bindings of the catalogue's guns. */
    private static Function<ItemStack, String> nameMutatorFor(String id) {
        return switch (id) {
            case "gun_g3_zebra" -> XFactory556mm.LAMBDA_NAME_G3;
            case "gun_maresleg" -> XFactory12ga.LAMBDA_NAME_MARESLEG;
            default -> null;
        };
    }

    private static GunConfig shotgun(float durability, ConfigWrapper<Float> damage, int delay, int reloadBegin,
            int reloadCycle, int reloadEnd, int reloadPost, int capacity,
            BulletConfig[] rounds) {
        return shotgun(durability, damage, delay, reloadBegin, reloadCycle, reloadEnd, reloadPost,
                0, capacity, rounds, 0.025F, 1F, -0.1875D, 0, 0, 10, 39,
                ModGunSounds.FIRE_SHOTGUN::get, XFactory12ga.LAMBDA_RECOIL_MARESLEG);
    }

    private static GunConfig shotgun(float durability, ConfigWrapper<Float> damage, int delay, int reloadBegin,
            int reloadCycle, int reloadEnd, int reloadPost, int capacity,
            BulletConfig[] rounds, int jam) {
        return shotgun(durability, damage, delay, reloadBegin, reloadCycle, reloadEnd, reloadPost,
                0, capacity, rounds, 0.025F, 1F, -0.1875D, jam, 0, 10, 39,
                ModGunSounds.FIRE_SHOTGUN::get, XFactory12ga.LAMBDA_RECOIL_MARESLEG);
    }

    private static GunConfig shotgun(float durability, ConfigWrapper<Float> damage, int delay, int reloadBegin,
            int reloadCycle, int reloadEnd, int reloadPost, int reloadPre, int capacity,
            BulletConfig[] rounds, float hipfireSpread,
            float ammoSpread, double side, int jam, int magazineIndex, int draw, int inspect,
            Supplier<SoundEvent> sound, BiConsumer<ItemStack,
                    ItemGunBaseNT.LambdaContext> recoil) {
        return new GunConfig().dura(durability).draw(draw).inspect(inspect)
                .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(new Receiver(0).dmg(damage).delay(delay)
                        .reload(reloadPre, reloadBegin, reloadCycle, reloadEnd, reloadPost)
                        .spreadHipfire(hipfireSpread).spreadAmmo(ammoSpread).jam(jam)
                        .sound(sound, 1F, 1F)
                        .mag(new MagazineSingleReload(magazineIndex, capacity).addConfigs(rounds))
                        .offset(0.75, -0.0625, side).setupStandardFire()
                        .recoil(recoil))
                .setupStandardConfiguration();
    }

    private static GunConfig doubleBarrel(float durability, ConfigWrapper<Float> damage, float pitch,
            BulletConfig[] rounds) {
        return doubleBarrel(durability, damage, pitch, rounds, 1F);
    }

    private static GunConfig doubleBarrel(float durability, ConfigWrapper<Float> damage, float pitch,
            BulletConfig[] rounds, float ammoSpread) {
        return new GunConfig().dura(durability).draw(10).inspect(39)
                .crosshair(Crosshair.L_CIRCLE).smoke(XFactory762mm.LAMBDA_SMOKE)
                .rec(new Receiver(0).dmg(damage).rounds(2).delay(10).reload(41).reloadOnEmpty(true)
                        .spreadAmmo(ammoSpread)
                        .sound(ModGunSounds.FIRE_SHOTGUN::get, 1F, pitch)
                        .mag(new MagazineFullReload(0, 2).addConfigs(rounds))
                        .offset(0.75, -0.0625, -0.1875D).setupStandardFire()
                        .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                                (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5))))
                .setupStandardConfiguration().ps(XFactory10ga.LAMBDA_DOUBLE_SECONDARY);
    }

    private static GunConfig minigunConfig(double side, ConfigWrapper<Float> damage) {
        return new GunConfig().dura(50_000).draw(20).inspect(20)
                .crosshair(Crosshair.L_CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0).dmg(damage).delay(1).auto(true).dry(15).spread(0.01F)
                         .sound(ModGunSounds.FIRE_MINIGUN::get, 1F, 1F)
                        .mag(new MagazineBelt().addConfigs(XFactory762mm.all()))
                        .offset(1, -0.0625 * 2.5, side).setupStandardFire()
                        .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(
                                (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5),
                                (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5))))
                .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER);
    }

    private static GunConfig bmgGun(float durability, ConfigWrapper<Float> damage,
            BulletConfig[] rounds, int capacity,
            Supplier<SoundEvent> sound) {
        return new GunConfig().dura(durability).draw(20).inspect(50)
                .crosshair(Crosshair.CIRCLE).smoke(LAMBDA_BMG_SMOKE)
                .rec(new Receiver(0).dmg(damage).delay(25).dry(25).spreadHipfire(0.05F).reload(51).jam(43)
                        .sound(sound, 1F, 1F).mag(new MagazineFullReload(0, capacity).addConfigs(rounds))
                        .offset(1, -0.0625 * 1.5, -0.25D).setupStandardFire()
                        .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(12.5F,
                                (float) (ctx.getPlayer().getRandom().nextGaussian() * 1F))))
                .setupStandardConfiguration();
    }

    /** 1.7 aberrator receiver: the beam goes through the standard no-wear fire, so firing never adds wear. */
    private static GunConfig beamGun(int magIndex, double side, boolean akimbo, ConfigWrapper<Float> damage) {
        Receiver receiver = new Receiver(0).dmg(damage).delay(13).dry(21).reload(51)
                .sound(ModGunSounds.FIRE_ABERRATOR::get, 1F, 1F)
                .mag(new MagazineFullReload(magIndex, 5).addConfigs(XFactory35800.p35800,
                        XFactory35800.p35800_bl))
                .offset(0.75, -0.0625 * 1.5, side)
                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE)
                .recoil((stack, ctx) -> ItemGunBaseNT.setupRecoil(10F,
                        (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5)));
        if (akimbo) receiver.spreadHipfire(0F);
        return new GunConfig().dura(2_000).draw(10).inspect(26)
                .crosshair(Crosshair.CIRCLE).smoke(Lego.LAMBDA_STANDARD_SMOKE)
                .rec(receiver);
    }

}
