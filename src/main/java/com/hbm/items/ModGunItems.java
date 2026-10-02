package com.hbm.items;

import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunFactory;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.factory.XFactory22lr;
import com.hbm.items.weapon.sedna.factory.XFactory357;
import com.hbm.items.weapon.sedna.factory.XFactory9mm;
import com.hbm.items.weapon.sedna.factory.XFactory44;
import com.hbm.items.weapon.sedna.factory.XFactory556mm;
import com.hbm.items.weapon.sedna.factory.XFactoryBlackPowder;
import com.hbm.items.weapon.sedna.factory.XFactory10ga;
import com.hbm.items.weapon.sedna.factory.XFactory12ga;
import com.hbm.items.weapon.sedna.factory.XFactory35800;
import com.hbm.items.weapon.sedna.factory.XFactory40mm;
import com.hbm.items.weapon.sedna.factory.XFactory45;
import com.hbm.items.weapon.sedna.factory.XFactory75Bolt;
import com.hbm.items.weapon.sedna.factory.XFactory762mm;
import com.hbm.items.weapon.sedna.factory.BallisticGunCatalog;
import com.hbm.items.weapon.sedna.factory.SpecialGunCatalog;
import com.hbm.items.weapon.sedna.factory.Orchestras;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.items.weapon.sedna.factory.XFactoryTool;
import com.hbm.items.weapon.sedna.factory.XFactoryPA;
import com.hbm.items.weapon.sedna.factory.NtmEnergyGunCatalog;
import com.hbm.items.weapon.sedna.factory.NtmToolGunCatalog;
import com.hbm.items.weapon.sedna.impl.ItemGunChargeThrower;
import com.hbm.items.weapon.sedna.impl.ItemGunChemthrower;
import com.hbm.items.weapon.sedna.impl.ItemGunDrill;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.items.weapon.GunB92;
import com.hbm.items.weapon.GunB92Cell;
import com.hbm.sound.ModSounds;
import com.hbm.sound.ModGunSounds;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.Item;

/**
 * The Sedna guns.
 *
 * <p>Entries are ports of upstream's own declarations, stat for stat — {@code gun_debug} from
 * {@code GunFactory}, the revolver from {@code XFactory357}, and the automatic/akimbo examples from
 * the per-calibre factories. They are the proving pair
 * for the gun engine: the debug gun is the smallest complete weapon in the mod (one receiver, one
 * round), and the revolver is the reference implementation of the standard fire path, so anything that
 * works for it works for the other sixty-odd guns as data.
 *
 * <p>The bullet configs a magazine names must exist first, so {@link GunFactory#init()} runs before the
 * suppliers here are resolved. Registry suppliers are only called during registration, which is well
 * after mod bootstrap calls it.
 *
     * <p>Smoke and the dynamic weapon-mod/name-mutation system remain deferred. The server-safe orchestra
     * timelines are attached below; their client loop/casing/muzzle effects are intentionally separate.
     * The item JSONs point at the shared OBJ renderer instead of inventing sprite placeholders.
 */
public final class ModGunItems {

    private ModGunItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(ITEMS);

    static {
        // GunCatalogue touches this class before registry flush; sound declarations remain deferred.
        ModGunSounds.init();
    }

    /** The source debug gun: two receivers, two trigger routes, and no frills. */
    public static final RegistryObject<ItemGunBaseNT> gun_debug = REG.item("gun_debug", props ->
            new ItemGunBaseNT(props, WeaponQuality.DEBUG, new GunConfig()
                    .dura(600F).draw(15).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(10F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 12).addConfigs(GunFactory.ammo_debug))
                            .offset(0.75, -0.0625, -0.3125D)
                            .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE),
                    new Receiver(1)
                            .dmg(5F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(1, 12).addConfigs(GunFactory.ammo_debug_shot))
                            .offset(0.75, -0.0625, -0.3125D)
                            .canFire(Lego.LAMBDA_SECOND_CAN_FIRE).fire(Lego.LAMBDA_SECOND_FIRE))
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                    .ps((stack, ctx) -> Lego.clickReceiver(stack, ctx, 1))
                    .pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .pt(Lego.LAMBDA_TOGGLE_AIM)
                     .decider(GunFactory.LAMBDA_DEBUG_DECIDER)
                       .orchestra(Orchestras.DEBUG_ORCHESTRA)), p -> p);

    /** Original-hbm's separately implemented charged B92 energy pistol. */
    public static final RegistryObject<GunB92> gun_b92 = REG.item("gun_b92", GunB92::new, p -> p);
    public static final RegistryObject<GunB92Cell> gun_b92_ammo = REG.item("gun_b92_ammo",
            props -> new GunB92Cell(props), p -> p).transform(Presets.sprite("hbm:items/gun_b92_ammo_alt"));

    /** The break-action .357 — upstream's reference weapon for the standard configuration. */
    public static final RegistryObject<ItemGunBaseNT> gun_light_revolver = REG.item("gun_light_revolver", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(7.5F).delay(11).reload(55).jam(45)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactory357.all()))
                            .offset(0.75, -0.0625, -0.3125D)
                             .setupStandardFire().recoil(XFactory357.LAMBDA_RECOIL_ATLAS))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_ATLAS))
                .setDefaultAmmo(ModAmmoItems.m357_sp, 12), p -> p);

    /** Atlas: the higher-damage, otherwise mechanically identical break-action revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_light_revolver_atlas = REG.item("gun_light_revolver_atlas", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE, new GunConfig()
                    .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(12.5F).delay(16).reload(55).jam(45)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactory357.all()))
                            .offset(0.75, -0.0625, -0.3125D)
                             .setupStandardFire().recoil(XFactory357.LAMBDA_RECOIL_ATLAS))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_ATLAS))
                .setDefaultAmmo(ModAmmoItems.m357_jhp, 12), p -> p);

    /** Day And Night: two independently triggered and persisted legendary revolvers. */
    public static final RegistryObject<ItemGunBaseNT> gun_light_revolver_dani = REG.item("gun_light_revolver_dani", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY,
                    new GunConfig().dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE)
                            .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                            .rec(new Receiver(0)
                                    .dmg(15F).spreadHipfire(0F).delay(11).reload(55).jam(45)
                                    .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.1F)
                                    .mag(new MagazineFullReload(0, 6).addConfigs(XFactory357.all()))
                                    .offset(0.75, -0.0625, 0.3125D)
                                    .setupStandardFire().recoil(XFactory357.LAMBDA_RECOIL_DANI))
                            .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)
                              .orchestra(Orchestras.ORCHESTRA_DANI),
                    new GunConfig().dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE)
                            .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                            .rec(new Receiver(0)
                                    .dmg(15F).spreadHipfire(0F).delay(11).reload(55).jam(45)
                                    .sound(ModSounds.FIRE_PISTOL, 1.0F, 0.9F)
                                    .mag(new MagazineFullReload(1, 6).addConfigs(XFactory357.all()))
                                    .offset(0.75, -0.0625, -0.3125D)
                                    .setupStandardFire().recoil(XFactory357.LAMBDA_RECOIL_DANI))
                            .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)
                              .orchestra(Orchestras.ORCHESTRA_DANI))
                .setDefaultAmmo(ModAmmoItems.m357_express, 24), p -> p);

    /** Six-shot Pepperbox accepting the four upstream stone/iron black-powder loads. */
    public static final RegistryObject<ItemGunBaseNT> gun_pepperbox = REG.item("gun_pepperbox", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(5F).delay(27).reload(67).jam(58)
                            .sound(ModGunSounds.FIRE_BLACK_POWDER, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactoryBlackPowder.all()))
                            .offset(0.75, -0.0625, -0.1875D)
                             .setupStandardFire().recoil(XFactoryBlackPowder.LAMBDA_RECOIL_PEPPERBOX))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_PEPPERBOX))
                .setDefaultAmmo(ModAmmoItems.stone, 12), p -> p);

    /** Unmodified H&K G3 path: automatic/semi-auto selector and thirty-round 5.56 mm magazine. */
    public static final RegistryObject<ItemGunBaseNT> gun_g3 = REG.item("gun_g3", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(3_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(5F).delay(2).auto(true).dry(15).spread(0F).reload(50).jam(47)
                            .sound(ModGunSounds.FIRE_ASSAULT, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory556mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory556mm.LAMBDA_RECOIL_G3))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_G3)
                       .ps(Lego.LAMBDA_STANDARD_CLICK_SECONDARY))
                .setDefaultAmmo(ModAmmoItems.r556_sp, 30), p -> p);

    // The remaining ordinary factory declarations are owned by the real ballistic catalogue.
    public static final RegistryObject<ItemGunBaseNT> gun_g3_zebra = BallisticGunCatalog.gun_g3_zebra;
    public static final RegistryObject<ItemGunBaseNT> gun_stg77 = BallisticGunCatalog.gun_stg77;
    public static final RegistryObject<ItemGunBaseNT> gun_carbine = BallisticGunCatalog.gun_carbine;
    public static final RegistryObject<ItemGunBaseNT> gun_mas36 = BallisticGunCatalog.gun_mas36;
    public static final RegistryObject<ItemGunBaseNT> gun_minigun = BallisticGunCatalog.gun_minigun;
    public static final RegistryObject<ItemGunBaseNT> gun_minigun_lacunae = BallisticGunCatalog.gun_minigun_lacunae;
    public static final RegistryObject<ItemGunBaseNT> gun_minigun_dual = BallisticGunCatalog.gun_minigun_dual;
    public static final RegistryObject<ItemGunBaseNT> gun_m2 = BallisticGunCatalog.gun_m2;
    public static final RegistryObject<ItemGunBaseNT> gun_maresleg = BallisticGunCatalog.gun_maresleg;
    public static final RegistryObject<ItemGunBaseNT> gun_maresleg_akimbo = BallisticGunCatalog.gun_maresleg_akimbo;
    public static final RegistryObject<ItemGunBaseNT> gun_maresleg_broken = BallisticGunCatalog.gun_maresleg_broken;
    public static final RegistryObject<ItemGunBaseNT> gun_liberator = BallisticGunCatalog.gun_liberator;
    public static final RegistryObject<ItemGunBaseNT> gun_spas12 = BallisticGunCatalog.gun_spas12;
    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun = BallisticGunCatalog.gun_autoshotgun;
    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun_shredder = BallisticGunCatalog.gun_autoshotgun_shredder;
    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun_sexy = BallisticGunCatalog.gun_autoshotgun_sexy;
    public static final RegistryObject<ItemGunBaseNT> gun_flaregun = BallisticGunCatalog.gun_flaregun;
    public static final RegistryObject<ItemGunBaseNT> gun_congolake = BallisticGunCatalog.gun_congolake;
    public static final RegistryObject<ItemGunBaseNT> gun_mk108 = BallisticGunCatalog.gun_mk108;
    public static final RegistryObject<ItemGunBaseNT> gun_amat = BallisticGunCatalog.gun_amat;
    public static final RegistryObject<ItemGunBaseNT> gun_amat_subtlety = BallisticGunCatalog.gun_amat_subtlety;
    public static final RegistryObject<ItemGunBaseNT> gun_amat_penance = BallisticGunCatalog.gun_amat_penance;
    public static final RegistryObject<ItemGunBaseNT> gun_bolter = BallisticGunCatalog.gun_bolter;
    public static final RegistryObject<ItemGunBaseNT> gun_double_barrel = BallisticGunCatalog.gun_double_barrel;
    public static final RegistryObject<ItemGunBaseNT> gun_double_barrel_sacred_dragon =
            BallisticGunCatalog.gun_double_barrel_sacred_dragon;
    public static final RegistryObject<ItemGunBaseNT> gun_autoshotgun_heretic = BallisticGunCatalog.gun_autoshotgun_heretic;
    public static final RegistryObject<ItemGunBaseNT> gun_aberrator = BallisticGunCatalog.gun_aberrator;
    public static final RegistryObject<ItemGunBaseNT> gun_aberrator_eott = BallisticGunCatalog.gun_aberrator_eott;

    public static final RegistryObject<ItemGunBaseNT> gun_panzerschreck = SpecialGunCatalog.gun_panzerschreck;
    public static final RegistryObject<ItemGunBaseNT> gun_stinger = SpecialGunCatalog.gun_stinger;
    public static final RegistryObject<ItemGunBaseNT> gun_quadro = SpecialGunCatalog.gun_quadro;
    public static final RegistryObject<ItemGunBaseNT> gun_missile_launcher = SpecialGunCatalog.gun_missile_launcher;
    public static final RegistryObject<ItemGunBaseNT> gun_fatman = SpecialGunCatalog.gun_fatman;
    public static final RegistryObject<ItemGunBaseNT> gun_flamer = SpecialGunCatalog.gun_flamer;
    public static final RegistryObject<ItemGunBaseNT> gun_flamer_topaz = SpecialGunCatalog.gun_flamer_topaz;
    public static final RegistryObject<ItemGunBaseNT> gun_flamer_daybreaker = SpecialGunCatalog.gun_flamer_daybreaker;

    // The energy/accelerator/Folly factories expose the same source aliases through their own catalog.
    public static final RegistryObject<ItemGunBaseNT> gun_tesla_cannon = NtmEnergyGunCatalog.gun_tesla_cannon;
    public static final RegistryObject<ItemGunBaseNT> gun_laser_pistol = NtmEnergyGunCatalog.gun_laser_pistol;
    public static final RegistryObject<ItemGunBaseNT> gun_laser_pistol_pew_pew = NtmEnergyGunCatalog.gun_laser_pistol_pew_pew;
    public static final RegistryObject<ItemGunBaseNT> gun_laser_pistol_morning_glory = NtmEnergyGunCatalog.gun_laser_pistol_morning_glory;
    public static final RegistryObject<ItemGunBaseNT> gun_lasrifle = NtmEnergyGunCatalog.gun_lasrifle;
    public static final RegistryObject<ItemGunBaseNT> gun_tau = NtmEnergyGunCatalog.gun_tau;
    public static final RegistryObject<ItemGunBaseNT> gun_coilgun = NtmEnergyGunCatalog.gun_coilgun;
    public static final RegistryObject<ItemGunBaseNT> gun_n_i_4_n_i = NtmEnergyGunCatalog.gun_n_i_4_n_i;
    public static final RegistryObject<ItemGunBaseNT> gun_folly = NtmEnergyGunCatalog.gun_folly;
    public static final RegistryObject<ItemGunBaseNT> gun_fireext = NtmToolGunCatalog.gun_fireext;
    public static final RegistryObject<ItemGunDrill> gun_drill = NtmToolGunCatalog.gun_drill;
    public static final RegistryObject<XFactoryPA.ItemGunPA> gun_pa_melee = NtmToolGunCatalog.gun_pa_melee;
    public static final RegistryObject<XFactoryPA.ItemGunPA> gun_pa_ranged = NtmToolGunCatalog.gun_pa_ranged;

    /** Real factory-backed utility guns; fire extinguisher, drill, and PA remain dependency-bound. */
     public static final RegistryObject<ItemGunBaseNT> gun_charge_thrower =
             com.hbm.items.weapon.sedna.factory.NtmToolGunCatalog.gun_charge_thrower;
    public static final RegistryObject<ItemGunBaseNT> gun_chemthrower = REG.item("gun_chemthrower", props ->
            new ItemGunChemthrower(props, WeaponQuality.A_SIDE,
                    XFactoryFlamer.chemthrowerConfig().orchestra(Orchestras.ORCHESTRA_CHEMTHROWER)), p -> p);

    /** The automatic .22 submachine gun, stat-for-stat from upstream's Gen-3 declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_am180 = REG.item("gun_am180", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(177 * 25).draw(15).inspect(38).crosshair(Crosshair.L_CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(2F).delay(1).dry(10).auto(true).spread(0.01F).reload(66).jam(30)
                            .sound(ModSounds.FIRE_GREASE_GUN::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 177).addConfigs(XFactory22lr.all()))
                            .offset(1, -0.0625 * 1.5, -0.1875D)
                            .setupStandardFire().recoil(XFactory22lr.LAMBDA_RECOIL_AM180))
                       .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_AM180))
                .setDefaultAmmo(ModAmmoItems.p22_sp, 35), p -> p);

    /** The M3 Grease Gun, ported from upstream's Gen-3 9 mm declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_greasegun = REG.item("gun_greasegun", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(3F).delay(4).dry(40).auto(true).spread(0.015F).reload(60).jam(55)
                            .sound(ModSounds.FIRE_GREASE_GUN::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory9mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_GREASEGUN))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_GREASEGUN))
                .setDefaultAmmo(ModAmmoItems.p9_sp, 30), p -> p);

    /** The Comically Long Pistol, ported from upstream's Gen-3 9 mm declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_lag = REG.item("gun_lag", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(1_700).draw(7).inspect(31).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(25F).delay(4).dry(10).spread(0.005F).reload(53).jam(44)
                            .sound(ModSounds.FIRE_PISTOL::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 17).addConfigs(XFactory9mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().fire(XFactory9mm.LAMBDA_FIRE_LAG)
                            .recoil(XFactory9mm.LAMBDA_RECOIL_LAG))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_LAG))
                .setDefaultAmmo(ModAmmoItems.p9_jhp, 17), p -> p);

    /** The Uzi, ported from upstream's Gen-3 9 mm declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_uzi = REG.item("gun_uzi", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(3F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50)
                            .sound(ModSounds.FIRE_UZI::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory9mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_UZI))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_UZI))
                .setDefaultAmmo(ModAmmoItems.p9_sp, 30), p -> p);

    /** Two independently animated Uzis, each with its own thirty-round magazine. */
    public static final RegistryObject<ItemGunBaseNT> gun_uzi_akimbo = REG.item("gun_uzi_akimbo", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE,
                    new GunConfig().dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                            .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                            .rec(new Receiver(0)
                                    .dmg(3F).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F)
                                    .reload(55).jam(50).sound(ModSounds.FIRE_UZI::get, 1.0F, 1.0F)
                                     .mag(new MagazineFullReload(0, 30).addConfigs(XFactory9mm.all()))
                                     .offset(1, -0.0625 * 2.5, 0.375D)
                                     .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_UZI))
                            .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)
                             .orchestra(Orchestras.ORCHESTRA_UZI_AKIMBO),
                    new GunConfig().dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                            .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                            .rec(new Receiver(0)
                                    .dmg(3F).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F)
                                    .reload(55).jam(50).sound(ModSounds.FIRE_UZI::get, 1.0F, 1.0F)
                                     .mag(new MagazineFullReload(1, 30).addConfigs(XFactory9mm.all()))
                                     .offset(1, -0.0625 * 2.5, -0.375D)
                                     .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_UZI))
                            .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(XFactory9mm.LAMBDA_SECOND_UZI)
                              .orchestra(Orchestras.ORCHESTRA_UZI_AKIMBO))
                .setDefaultAmmo(ModAmmoItems.p9_sp, 60), p -> p);

    /** The semi-automatic .22 target pistol. */
    public static final RegistryObject<ItemGunBaseNT> gun_star_f = REG.item("gun_star_f", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(starReceiver(0, -0.1875D))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_STAR_F))
                .setDefaultAmmo(ModAmmoItems.p22_sp, 15), p -> p);

    /** Two target pistols in one stack, with independent magazines and triggers. */
    public static final RegistryObject<ItemGunBaseNT> gun_star_f_akimbo = REG.item("gun_star_f_akimbo", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE,
                    new GunConfig()
                            .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                            .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                            .rec(starReceiver(0, 0.25D))
                            .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)
                             .orchestra(Orchestras.ORCHESTRA_STAR_F_AKIMBO),
                    new GunConfig()
                            .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                            .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                            .rec(starReceiver(1, -0.25D))
                            .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)
                               .orchestra(Orchestras.ORCHESTRA_STAR_F_AKIMBO))
                .setDefaultAmmo(ModAmmoItems.p22_sp, 30), p -> p);

    /** The tube-fed .44 lever-action rifle. */
    public static final RegistryObject<ItemGunBaseNT> gun_henry = REG.item("gun_henry", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(10F).delay(20).reload(25, 11, 14, 8).jam(45)
                            .sound(ModSounds.FIRE_RIFLE::get, 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 14).addConfigs(XFactory44.all()))
                            .offset(0.75, -0.0625, -0.1875D)
                             .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_HENRY))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_HENRY))
                .setDefaultAmmo(ModAmmoItems.m44_sp, 14), p -> p);

    /** Lincoln's higher-damage Henry variant. */
    public static final RegistryObject<ItemGunBaseNT> gun_henry_lincoln = REG.item("gun_henry_lincoln", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE, new GunConfig()
                    .dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(20F).spreadHipfire(0F).delay(20).reload(25, 11, 14, 8).jam(45)
                            .sound(ModSounds.FIRE_RIFLE::get, 1.0F, 1.25F)
                            .mag(new MagazineSingleReload(0, 14).addConfigs(XFactory44.all()))
                            .offset(0.75, -0.0625, -0.1875D)
                             .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_HENRY))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_HENRY))
                .setDefaultAmmo(ModAmmoItems.m44_jhp, 14), p -> p);

    /** The ordinary six-shot .44 heavy revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_heavy_revolver = REG.item("gun_heavy_revolver", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(600).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(15F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactory44.all()))
                            .offset(0.75, -0.0625, -0.3125D)
                             .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_NOPIP))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_NOPIP))
                .setDefaultAmmo(ModAmmoItems.m44_sp, 12), p -> p);

    /** Little Macintosh, the scoped legendary heavy revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_heavy_revolver_lilmac = REG.item("gun_heavy_revolver_lilmac", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY, new GunConfig()
                    .dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .scopeTexture(dev.golem.core.Ids.of(Constants.MOD_ID, "textures/misc/scope_44.png"))
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(30F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 1.0F)
                             .mag(new MagazineFullReload(0, 6).addConfigs(
                                     XFactory44.m44_equestrian_pip, XFactory44.m44_bp, XFactory44.m44_sp,
                                     XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express))
                             .offset(0.75, -0.0625, -0.3125D)
                              .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_NOPIP))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_NOPIP))
                .setDefaultAmmo(ModAmmoItems.m44_jhp, 12), p -> p);

    /** Protege, the torpedo-impact legendary heavy revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_heavy_revolver_protege = REG.item("gun_heavy_revolver_protege", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY, new GunConfig()
                    .dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(30F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 0.8F)
                             .mag(new MagazineFullReload(0, 6).addConfigs(
                                     XFactory44.m44_equestrian_mn7, XFactory44.m44_bp, XFactory44.m44_sp,
                                     XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express))
                             .offset(0.75, -0.0625, -0.3125D)
                              .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_NOPIP))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_NOPIP))
                .setDefaultAmmo(ModAmmoItems.m44_jhp, 12), p -> p);

    /** Hangman, an eight-round inspection-heavy legendary rifle. */
    public static final RegistryObject<ItemGunBaseNT> gun_hangman = REG.item("gun_hangman", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY, new GunConfig()
                    .dura(600).draw(10).inspect(31).inspectCancel(false).crosshair(Crosshair.CIRCLE)
                    .smoke(Lego.LAMBDA_STANDARD_SMOKE)
                    .rec(new Receiver(0)
                            .dmg(25F).delay(10).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 8).addConfigs(XFactory44.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                     .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_HANGMAN))
                      .setupStandardConfiguration().orchestra(Orchestras.ORCHESTRA_HANGMAN)
                     .ps(XFactory44.SMACK_A_FUCKER))
                .setDefaultAmmo(ModAmmoItems.m44_fmj, 16), p -> p);

    private static Receiver starReceiver(int index, double side) {
        return new Receiver(index)
                .dmg(12.5F).delay(5).dry(17).spread(0.01F).reload(40).jam(32)
                .sound(ModSounds.FIRE_PISTOL_LIGHT::get, 1.0F, 1.0F)
                .mag(new MagazineFullReload(index, 15).addConfigs(XFactory22lr.all()))
                .offset(1, -0.0625 * 1.5, side)
                .setupStandardFire().recoil(XFactory22lr.LAMBDA_RECOIL_STAR_F);
    }

    static {
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_debug);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_b92);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_b92_ammo);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_light_revolver);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_light_revolver_atlas);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_light_revolver_dani);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_pepperbox);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_g3);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_am180);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_greasegun);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_lag);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_uzi);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_uzi_akimbo);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_star_f);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_star_f_akimbo);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_henry);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_henry_lincoln);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_heavy_revolver);
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_heavy_revolver_lilmac);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_heavy_revolver_protege);
          com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_hangman);
          com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_chemthrower);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_g3_zebra);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_stg77);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_carbine);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_mas36);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_minigun);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_minigun_lacunae);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_minigun_dual);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_m2);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_maresleg);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_maresleg_akimbo);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_maresleg_broken);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_liberator);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_spas12);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_autoshotgun);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_autoshotgun_shredder);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_autoshotgun_sexy);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_flaregun);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_congolake);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_mk108);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_amat);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_amat_subtlety);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_amat_penance);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_bolter);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_double_barrel);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_double_barrel_sacred_dragon);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_autoshotgun_heretic);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_aberrator);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_aberrator_eott);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_panzerschreck);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_stinger);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_quadro);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_missile_launcher);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_fatman);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_flamer);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_flamer_topaz);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_flamer_daybreaker);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_tesla_cannon);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_laser_pistol);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_laser_pistol_pew_pew);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_laser_pistol_morning_glory);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_lasrifle);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_tau);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_coilgun);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_n_i_4_n_i);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_folly);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_fireext);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_drill);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_pa_melee);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_pa_ranged);
         com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_charge_thrower);
      }

    public static void init() {}
}
