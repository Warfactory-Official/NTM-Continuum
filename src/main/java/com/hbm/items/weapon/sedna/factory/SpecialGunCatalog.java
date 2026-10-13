package com.hbm.items.weapon.sedna.factory;

import com.hbm.registration.Reg;
import dev.golem.core.registry.RegistryObject;

import com.hbm.Constants;
import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import dev.golem.core.registry.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;

/**
 * Registry-owned part of the special Sedna guns.
 *
 * <p>The old game kept these rounds on two metadata items.  Their flattened registry identities are
 * declared by {@link com.hbm.items.ModAmmoItems}; this class only owns the subset of gun entries whose
 * projectile path already exists in the common engine.
 */
public final class SpecialGunCatalog {
    public static final DeferredRegister.Items GUNS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(GUNS);

    public static RegistryObject<ItemGunBaseNT> gun_panzerschreck;
    public static RegistryObject<ItemGunBaseNT> gun_stinger;
    public static RegistryObject<ItemGunBaseNT> gun_quadro;
    public static RegistryObject<ItemGunBaseNT> gun_missile_launcher;
    public static RegistryObject<ItemGunBaseNT> gun_fatman;
    public static RegistryObject<ItemGunBaseNT> gun_flamer;
    public static RegistryObject<ItemGunBaseNT> gun_flamer_topaz;
    public static RegistryObject<ItemGunBaseNT> gun_flamer_daybreaker;

    private static boolean gunsRegistered;

    private SpecialGunCatalog() {}

    /** Explicit bootstrap hook; the guard keeps repeated common initialization harmless. */
    public static synchronized void init() {
        registerSupportedGuns();
    }

    /** Compatibility facade for existing factory configs; registration remains in ModAmmoItems. */
    public static Supplier<Item> standardAmmo(String enumName) {
        return ModAmmoItems.standardAmmo(enumName);
    }

    /** Compatibility facade for existing factory configs; registration remains in ModAmmoItems. */
    public static Supplier<Item> secretAmmo(String enumName) {
        return ModAmmoItems.secretAmmo(enumName);
    }

    /**
     * Registers the ordinary projectile-backed special guns. Beam, fluid and PA guns are
     * not made into generic stand-ins here.
     */
    public static synchronized void registerSupportedGuns() {
        if (gunsRegistered) return;
        gunsRegistered = true;

        gun_panzerschreck = REG.item("gun_panzerschreck", p -> new ItemGunBaseNT(p, WeaponQuality.A_SIDE,
                XFactoryRocket.panzerschreckConfig().orchestra(Orchestras.ORCHESTRA_PANERSCHRECK))
                .setDefaultAmmo(ModAmmoItems.rocket_he, 3), p -> p);
        gun_stinger = REG.item("gun_stinger", p -> new ItemGunStinger(p, WeaponQuality.SPECIAL,
                XFactoryRocket.stingerConfig()
                        .orchestra(Orchestras.ORCHESTRA_STINGER))
                .setDefaultAmmo(ModAmmoItems.rocket_heat, 3), p -> p);
        gun_quadro = REG.item("gun_quadro", p -> new ItemGunBaseNT(p, WeaponQuality.A_SIDE,
                XFactoryRocket.quadroConfig().orchestra(Orchestras.ORCHESTRA_QUADRO))
                .setDefaultAmmo(ModAmmoItems.rocket_he, 4), p -> p);
        gun_missile_launcher = REG.item("gun_missile_launcher", p -> new ItemGunBaseNT(p, WeaponQuality.A_SIDE,
                XFactoryRocket.missileLauncherConfig()
                        .orchestra(Orchestras.ORCHESTRA_MISSILE_LAUNCHER))
                .setDefaultAmmo(ModAmmoItems.rocket_heat, 5), p -> p);
        gun_fatman = REG.item("gun_fatman", p -> new ItemGunBaseNT(p, WeaponQuality.A_SIDE,
                XFactoryCatapult.gunConfig().orchestra(Orchestras.ORCHESTRA_FATMAN))
                .setDefaultAmmoExpensive(ModAmmoItems.nuke_standard, 1), p -> p);
        gun_flamer = REG.item("gun_flamer", p -> new ItemGunBaseNT(p, WeaponQuality.A_SIDE,
                XFactoryFlamer.gunConfig(0).orchestra(Orchestras.ORCHESTRA_FLAMER))
                .setDefaultAmmo(ModAmmoItems.flame_diesel, 1), p -> p);
        gun_flamer_topaz = REG.item("gun_flamer_topaz", p -> new ItemGunBaseNT(p, WeaponQuality.B_SIDE,
                XFactoryFlamer.gunConfig(1).orchestra(Orchestras.ORCHESTRA_FLAMER))
                .setDefaultAmmo(ModAmmoItems.flame_diesel, 1), p -> p);
        gun_flamer_daybreaker = REG.item("gun_flamer_daybreaker", p -> new ItemGunBaseNT(p, WeaponQuality.LEGENDARY,
                XFactoryFlamer.gunConfig(2).orchestra(Orchestras.ORCHESTRA_FLAMER_DAYBREAKER))
                .setDefaultAmmo(ModAmmoItems.flame_diesel, 1), p -> p);
    }
}
