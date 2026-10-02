package com.hbm.items.weapon.sedna.factory;

import com.hbm.registration.Reg;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.Crosshair;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import dev.golem.core.Ids;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Registry-owned declarations copied from ntm-next's energy/accelerator/Folly factory registrations.
 * The GEM animation tables are resolved by {@code GunAnimations}; these suppliers retain the source
 * receiver, magazine, damage, reload, callback, and orchestra structure without coupling common code to
 * the client renderer.
 */
public final class NtmEnergyGunCatalog {

    public static final DeferredRegister.Items GUNS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(GUNS);

    private static final net.minecraft.resources.Identifier SCOPE_LUNA =
            Ids.of(Constants.MOD_ID, "textures/misc/scope_luna.png");

    public static final RegistryObject<ItemGunBaseNT> gun_tesla_cannon = gun("gun_tesla_cannon", WeaponQuality.A_SIDE,
            () -> new GunConfig().dura(1_000).draw(10).inspect(33).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(35F).delay(20).spreadHipfire(1.5F).reload(44).jam(19)
                            .sound(com.hbm.sound.ModGunSounds.FIRE_TESLA::get, 1F, 1F)
                            .mag(new MagazineBelt().addConfigs(XFactoryEnergy.energy_tesla,
                                    XFactoryEnergy.energy_tesla_overcharge, XFactoryEnergy.energy_tesla_ir))
                            .offset(0.75, 0, -0.375).offsetScoped(0.75, 0, -0.25)
                            .setupStandardFire().recoil(XFactoryEnergy.LAMBDA_RECOIL_ENERGY))
                      .setupStandardConfiguration(), ModAmmoItems.capacitor, 15);

    public static final RegistryObject<ItemGunBaseNT> gun_laser_pistol = gun("gun_laser_pistol", WeaponQuality.A_SIDE,
            () -> new GunConfig().dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(25F).delay(5).spread(1F).spreadHipfire(1F).reload(45).jam(37)
                            .sound(com.hbm.sound.ModGunSounds.FIRE_LASER_PISTOL::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(XFactoryEnergy.energy_las,
                                    XFactoryEnergy.energy_las_overcharge, XFactoryEnergy.energy_las_ir))
                            .offset(0.75, -0.0625 * 1.5, -0.1875)
                            .setupStandardFire().recoil(XFactoryEnergy.LAMBDA_RECOIL_ENERGY))
                      .setupStandardConfiguration(), ModAmmoItems.capacitor, 15);

    public static final RegistryObject<ItemGunBaseNT> gun_laser_pistol_pew_pew = gun(
            "gun_laser_pistol_pew_pew", WeaponQuality.B_SIDE,
            () -> new GunConfig().dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(30F).rounds(5).delay(10).spread(0.25F).spreadHipfire(1F)
                            .reload(45).jam(37)
                            .sound(com.hbm.sound.ModGunSounds.FIRE_LASER_PISTOL::get, 1F, 0.8F)
                            .mag(new MagazineFullReload(0, 10).addConfigs(XFactoryEnergy.energy_las,
                                    XFactoryEnergy.energy_las_overcharge, XFactoryEnergy.energy_las_ir))
                            .offset(0.75, -0.0625 * 1.5, -0.1875)
                            .setupStandardFire().recoil(XFactoryEnergy.LAMBDA_RECOIL_ENERGY))
                      .setupStandardConfiguration(), ModAmmoItems.capacitor_overcharge, 10);

    public static final RegistryObject<ItemGunBaseNT> gun_laser_pistol_morning_glory = gun(
            "gun_laser_pistol_morning_glory", WeaponQuality.LEGENDARY,
            () -> new GunConfig().dura(1_500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(20F).delay(7).spread(0F).spreadHipfire(0.5F).reload(45).jam(37)
                            .sound(com.hbm.sound.ModGunSounds.FIRE_LASER_PISTOL::get, 1F, 1.1F)
                            .mag(new MagazineFullReload(0, 20).addConfigs(XFactoryEnergy.energy_emerald,
                                    XFactoryEnergy.energy_emerald_overcharge, XFactoryEnergy.energy_emerald_ir))
                            .offset(0.75, -0.0625 * 1.5, -0.1875)
                            .setupStandardFire().recoil(XFactoryEnergy.LAMBDA_RECOIL_ENERGY))
                      .setupStandardConfiguration(), ModAmmoItems.capacitor_overcharge, 20);

    public static final RegistryObject<ItemGunBaseNT> gun_lasrifle = gun("gun_lasrifle", WeaponQuality.A_SIDE,
            () -> new GunConfig().dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                    .scopeTexture(SCOPE_LUNA)
                    .rec(new Receiver(0).dmg(50F).delay(8).spreadHipfire(1F).reload(44).jam(36)
                            .sound(com.hbm.sound.ModGunSounds.FIRE_LASER_RIFLE::get, 1F, 1F)
                            .mag(new MagazineFullReload(0, 24).addConfigs(XFactoryEnergy.energy_las,
                                    XFactoryEnergy.energy_las_overcharge, XFactoryEnergy.energy_las_ir))
                            .offset(0.75, -0.0625 * 1.5, -0.1875)
                            .setupStandardFire().recoil(XFactoryEnergy.LAMBDA_RECOIL_ENERGY))
                      .setupStandardConfiguration(), ModAmmoItems.capacitor, 24);

    public static final RegistryObject<ItemGunBaseNT> gun_tau = gun("gun_tau", WeaponQuality.A_SIDE,
            () -> new GunConfig().dura(6_400).draw(10).inspect(10).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0).dmg(25F).spreadHipfire(0F).delay(4).auto(true).spread(0F)
                            .mag(new MagazineBelt().addConfigs(XFactoryAccelerator.tau_uranium))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(XFactoryAccelerator.LAMBDA_RECOIL_TAU))
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY)
                    .rp(XFactoryAccelerator.LAMBDA_TAU_PRIMARY_RELEASE)
                    .ps(XFactoryAccelerator.LAMBDA_TAU_SECONDARY_PRESS)
                    .rs(XFactoryAccelerator.LAMBDA_TAU_SECONDARY_RELEASE)
                    .pr(Lego.LAMBDA_STANDARD_RELOAD)
                      .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER), ModAmmoItems.tau_uranium, 15);

    public static final RegistryObject<ItemGunBaseNT> gun_coilgun = gun("gun_coilgun", WeaponQuality.SPECIAL,
            () -> new GunConfig().dura(400).draw(5).inspect(39).crosshair(Crosshair.L_CIRCUMFLEX)
                    .rec(new Receiver(0).dmg(35F).delay(5).reload(20).jam(33)
                            .sound(com.hbm.sound.ModGunSounds.FIRE_COIL::get, 1F, 1F)
                            .mag(new MagazineSingleReload(0, 1).addConfigs(XFactoryAccelerator.coil_tungsten,
                                    XFactoryAccelerator.coil_ferrouranium))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(XFactoryAccelerator.LAMBDA_RECOIL_COILGUN))
                      .setupStandardConfiguration(), ModAmmoItems.coil_tungsten, 5);

    public static final RegistryObject<ItemGunBaseNT> gun_n_i_4_n_i = REG.item("gun_n_i_4_n_i",
            props -> new ItemGunNI4NI(props, WeaponQuality.SPECIAL,
                    XFactoryAccelerator.ni4niGunConfig().orchestra(Orchestras.ORCHESTRA_COILGUN)), p -> p);

    public static final RegistryObject<ItemGunBaseNT> gun_folly = REG.item("gun_folly",
            props -> new ItemGunBaseNT(props, WeaponQuality.SECRET,
                    XFactoryFolly.gunConfig().orchestra(Orchestras.ORCHESTRA_FOLLY)), p -> p);

    private static boolean initialized;

    private NtmEnergyGunCatalog() {}

    private static RegistryObject<ItemGunBaseNT> gun(String id, WeaponQuality quality, Supplier<GunConfig> config) {
        return REG.item(id, props -> new ItemGunBaseNT(props, quality,
                config.get().orchestra(orchestraFor(id))), p -> p);
    }

    private static RegistryObject<ItemGunBaseNT> gun(String id, WeaponQuality quality,
            Supplier<GunConfig> config, Supplier<? extends Item> defaultAmmo, int amount) {
        return REG.item(id, props -> new ItemGunBaseNT(props, quality,
                config.get().orchestra(orchestraFor(id))).setDefaultAmmo(defaultAmmo, amount), p -> p);
    }

    private static java.util.function.BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> orchestraFor(String id) {
        return switch (id) {
            case "gun_tesla_cannon" -> Orchestras.ORCHESTRA_TESLA;
            case "gun_laser_pistol", "gun_laser_pistol_pew_pew", "gun_laser_pistol_morning_glory" ->
                    Orchestras.ORCHESTRA_LASER_PISTOL;
            case "gun_lasrifle" -> Orchestras.ORCHESTRA_LASRIFLE;
            case "gun_tau" -> Orchestras.ORCHESTRA_TAU;
            case "gun_coilgun", "gun_n_i_4_n_i" -> Orchestras.ORCHESTRA_COILGUN;
            case "gun_folly" -> Orchestras.ORCHESTRA_FOLLY;
            default -> (stack, ctx) -> { };
        };
    }

    /** Flush/bootstrap hook for this helper; repeated common initialization is harmless. */
    public static synchronized void init() {
        if (initialized) return;
        initialized = true;
        ModTabs.addTo(HbmTab.WEAPONS, gun_tesla_cannon);
        ModTabs.addTo(HbmTab.WEAPONS, gun_laser_pistol);
        ModTabs.addTo(HbmTab.WEAPONS, gun_laser_pistol_pew_pew);
        ModTabs.addTo(HbmTab.WEAPONS, gun_laser_pistol_morning_glory);
        ModTabs.addTo(HbmTab.WEAPONS, gun_lasrifle);
        ModTabs.addTo(HbmTab.WEAPONS, gun_tau);
        ModTabs.addTo(HbmTab.WEAPONS, gun_coilgun);
        ModTabs.addTo(HbmTab.WEAPONS, gun_n_i_4_n_i);
        ModTabs.addTo(HbmTab.WEAPONS, gun_folly);
    }
}
