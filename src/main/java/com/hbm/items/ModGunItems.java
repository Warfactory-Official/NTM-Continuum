package com.hbm.items;

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
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.sound.ModSounds;
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
     * <p>Deferred, and simply not set on these configs: {@code smoke}, {@code orchestra}, and the dynamic
     * weapon-mod/name-mutation system. Smoke and orchestra are client presentation; the latter two are
     * absent from the shared port. The item JSONs point at the shared OBJ renderer instead of inventing
     * sprite placeholders.
 */
public final class ModGunItems {

    private ModGunItems() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** The smallest complete gun in the mod: one receiver, one round, no frills. */
    public static final RegistryObject<ItemGunBaseNT> gun_debug = ITEMS.registerItem("gun_debug", props ->
            new ItemGunBaseNT(props, WeaponQuality.DEBUG, new GunConfig()
                    .dura(600F).draw(15).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .rec(new Receiver(0)
                            .dmg(10F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 12).addConfigs(GunFactory.ammo_debug))
                            .offset(0.75, -0.0625, -0.3125D)
                            .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE))
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .pt(Lego.LAMBDA_TOGGLE_AIM)
                    .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)));

    /** The break-action .357 — upstream's reference weapon for the standard configuration. */
    public static final RegistryObject<ItemGunBaseNT> gun_light_revolver = ITEMS.registerItem("gun_light_revolver", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(7.5F).delay(16).reload(55).jam(45)
                            .sound(ModSounds.FIRE_PISTOL, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactory357.all()))
                            .offset(0.75, -0.0625, -0.3125D)
                            .setupStandardFire().recoil(XFactory357.LAMBDA_RECOIL_ATLAS))
                     .setupStandardConfiguration()));

    /** The automatic .22 submachine gun, stat-for-stat from upstream's Gen-3 declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_am180 = ITEMS.registerItem("gun_am180", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(177 * 25).draw(15).inspect(38).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(2F).delay(1).dry(10).auto(true).spread(0.02F).reload(66).jam(30)
                            .sound(ModSounds.FIRE_GREASE_GUN::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 177).addConfigs(XFactory22lr.all()))
                            .offset(1, -0.0625 * 1.5, -0.1875D)
                            .setupStandardFire().recoil(XFactory22lr.LAMBDA_RECOIL_AM180))
                     .setupStandardConfiguration()));

    /** The M3 Grease Gun, ported from upstream's Gen-3 9 mm declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_greasegun = ITEMS.registerItem("gun_greasegun", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(3F).delay(4).dry(40).auto(true).spread(0.015F).reload(60).jam(55)
                            .sound(ModSounds.FIRE_GREASE_GUN::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory9mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_GREASEGUN))
                    .setupStandardConfiguration()));

    /** The Comically Long Pistol, ported from upstream's Gen-3 9 mm declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_lag = ITEMS.registerItem("gun_lag", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(1_700).draw(7).inspect(31).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(25F).delay(4).dry(10).spread(0.005F).reload(53).jam(44)
                            .sound(ModSounds.FIRE_PISTOL::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 17).addConfigs(XFactory9mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_LAG))
                    .setupStandardConfiguration()));

    /** The Uzi, ported from upstream's Gen-3 9 mm declaration. */
    public static final RegistryObject<ItemGunBaseNT> gun_uzi = ITEMS.registerItem("gun_uzi", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(3F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50)
                            .sound(ModSounds.FIRE_UZI::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactory9mm.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_UZI))
                    .setupStandardConfiguration()));

    /** Two independently animated Uzis, each with its own thirty-round magazine. */
    public static final RegistryObject<ItemGunBaseNT> gun_uzi_akimbo = ITEMS.registerItem("gun_uzi_akimbo", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE,
                    new GunConfig().dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                            .rec(new Receiver(0)
                                    .dmg(3F).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F)
                                    .reload(55).jam(50).sound(ModSounds.FIRE_UZI::get, 1.0F, 1.0F)
                                     .mag(new MagazineFullReload(0, 30).addConfigs(XFactory9mm.all()))
                                     .offset(1, -0.0625 * 2.5, 0.375D)
                                     .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_UZI))
                            .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                            .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER),
                    new GunConfig().dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                            .rec(new Receiver(0)
                                    .dmg(3F).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F)
                                    .reload(55).jam(50).sound(ModSounds.FIRE_UZI::get, 1.0F, 1.0F)
                                     .mag(new MagazineFullReload(1, 30).addConfigs(XFactory9mm.all()))
                                     .offset(1, -0.0625 * 2.5, -0.375D)
                                     .setupStandardFire().recoil(XFactory9mm.LAMBDA_RECOIL_UZI))
                            .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                            .decider(XFactory9mm.LAMBDA_SECOND_UZI)));

    /** The semi-automatic .22 target pistol. */
    public static final RegistryObject<ItemGunBaseNT> gun_star_f = ITEMS.registerItem("gun_star_f", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                    .rec(starReceiver(0, -0.1875D))
                    .setupStandardConfiguration()));

    /** Two target pistols in one stack, with independent magazines and triggers. */
    public static final RegistryObject<ItemGunBaseNT> gun_star_f_akimbo = ITEMS.registerItem("gun_star_f_akimbo", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE,
                    new GunConfig()
                            .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                            .rec(starReceiver(0, 0.25D))
                            .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                            .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER),
                    new GunConfig()
                            .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                            .rec(starReceiver(1, -0.25D))
                            .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                             .decider(com.hbm.items.weapon.sedna.factory.GunStateDecider.LAMBDA_STANDARD_DECIDER)));

    /** The tube-fed .44 lever-action rifle. */
    public static final RegistryObject<ItemGunBaseNT> gun_henry = ITEMS.registerItem("gun_henry", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(10F).delay(20).reload(25, 11, 14, 8).jam(45)
                            .sound(ModSounds.FIRE_RIFLE::get, 1.0F, 1.0F)
                            .mag(new MagazineSingleReload(0, 14).addConfigs(XFactory44.all()))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_HENRY))
                    .setupStandardConfiguration()));

    /** Lincoln's higher-damage Henry variant. */
    public static final RegistryObject<ItemGunBaseNT> gun_henry_lincoln = ITEMS.registerItem("gun_henry_lincoln", props ->
            new ItemGunBaseNT(props, WeaponQuality.B_SIDE, new GunConfig()
                    .dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(20F).spreadHipfire(0F).delay(20).reload(25, 11, 14, 8).jam(45)
                            .sound(ModSounds.FIRE_RIFLE::get, 1.0F, 1.25F)
                            .mag(new MagazineSingleReload(0, 14).addConfigs(XFactory44.all()))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_HENRY))
                    .setupStandardConfiguration()));

    /** The ordinary six-shot .44 heavy revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_heavy_revolver = ITEMS.registerItem("gun_heavy_revolver", props ->
            new ItemGunBaseNT(props, WeaponQuality.A_SIDE, new GunConfig()
                    .dura(600).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .rec(new Receiver(0)
                            .dmg(15F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 6).addConfigs(XFactory44.all()))
                            .offset(0.75, -0.0625, -0.3125D)
                            .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_NOPIP))
                    .setupStandardConfiguration()));

    /** Little Macintosh, the scoped legendary heavy revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_heavy_revolver_lilmac = ITEMS.registerItem("gun_heavy_revolver_lilmac", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY, new GunConfig()
                    .dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .rec(new Receiver(0)
                            .dmg(30F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 1.0F)
                             .mag(new MagazineFullReload(0, 6).addConfigs(
                                     XFactory44.m44_equestrian_pip, XFactory44.m44_bp, XFactory44.m44_sp,
                                     XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express))
                             .offset(0.75, -0.0625, -0.3125D)
                             .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_NOPIP))
                    .setupStandardConfiguration()));

    /** Protege, the torpedo-impact legendary heavy revolver. */
    public static final RegistryObject<ItemGunBaseNT> gun_heavy_revolver_protege = ITEMS.registerItem("gun_heavy_revolver_protege", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY, new GunConfig()
                    .dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                    .rec(new Receiver(0)
                            .dmg(30F).delay(14).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 0.8F)
                             .mag(new MagazineFullReload(0, 6).addConfigs(
                                     XFactory44.m44_equestrian_mn7, XFactory44.m44_bp, XFactory44.m44_sp,
                                     XFactory44.m44_fmj, XFactory44.m44_jhp, XFactory44.m44_ap, XFactory44.m44_express))
                             .offset(0.75, -0.0625, -0.3125D)
                             .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_NOPIP))
                    .setupStandardConfiguration()));

    /** Hangman, an eight-round inspection-heavy legendary rifle. */
    public static final RegistryObject<ItemGunBaseNT> gun_hangman = ITEMS.registerItem("gun_hangman", props ->
            new ItemGunBaseNT(props, WeaponQuality.LEGENDARY, new GunConfig()
                    .dura(600).draw(10).inspect(31).inspectCancel(false).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(25F).delay(10).reload(46).jam(23)
                            .sound(ModSounds.SHOOT_44::get, 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 8).addConfigs(XFactory44.all()))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactory44.LAMBDA_RECOIL_HANGMAN))
                    .setupStandardConfiguration()
                    .ps(XFactory44.SMACK_A_FUCKER)));

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
        com.hbm.creativetabs.ModTabs.addTo(com.hbm.creativetabs.HbmTab.WEAPONS, gun_light_revolver);
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
    }

    public static void init() {}
}
