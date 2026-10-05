package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.ModAmmoItems;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.BulletConfigs;
import com.hbm.particle.helper.CasingRegistry;

/**
 * Declares the bullet configs, then hands off to the per-calibre factories.
 *
 * <p>Ported from 1.12.2 {@code GunFactory}. Upstream also registers the ammo <em>items</em> here; in
 * this port those are ordinary registry entries in {@link ModAmmoItems}, so this class is left with the
 * configs and the init order.
 *
 * <p>Order matters in one direction only: a bullet config must exist before a gun's magazine names it,
 * which is why every calibre's {@code init()} runs before the guns are built.
 */
public class GunFactory {

    /** The debug gun's round: barely any spread, and it bounces off almost anything. */
    public static BulletConfig ammo_debug;
    /** The source debug gun's six-pellet secondary receiver load. */
    public static BulletConfig ammo_debug_shot;
    private static boolean initialized;

    private GunFactory() {}

    public static synchronized void init() {
        if (initialized) return;
        ammo_debug = BulletConfigs.register("ammo_debug", new BulletConfig()
                .setItem(ModAmmoItems.ammo_debug)
                .setSpread(0.01F).setRicochetAngle(45));
        ammo_debug_shot = BulletConfigs.register("ammo_debug_shot", new BulletConfig()
                .setItem(ModAmmoItems.ammo_debug)
                .setSpread(0.05F).setProjectiles(6).setRicochetAngle(45));

        XFactoryBlackPowder.init();
        XFactory357.init();
        XFactory44.init();
        XFactory9mm.init();
        XFactory12ga.init();
        XFactory40mm.init();
        XFactory762mm.init();
        XFactory22lr.init();
        XFactoryFlamer.init();
        XFactoryRocket.init();
        XFactory556mm.init();
        XFactory50.init();
        XFactoryEnergy.init();
        XFactoryAccelerator.init();
        XFactoryCatapult.init();
        XFactory75Bolt.init();
        XFactoryFolly.init();
        XFactoryTurret.init();
        // The bullets the mask man, UFO and Balls-O-Tron fire; without this every NPC shot has a null config.
        XFactoryNPC.init();
        XFactoryWarhead.init();
        XFactory10ga.init();
        XFactory35800.init();
        XFactory45.init();
        XFactoryTool.init();
        XFactoryDrill.init();
        XFactoryPA.init();
        // 1.7 TileEntityBatterySocket.discharge (a static BulletConfig of the battery socket, History §19ek-hooks)
        com.hbm.tileentity.machine.storage.TileEntityBatterySocket.registerDischarge();
        CasingRegistry.install();
        initialized = true;
    }

    /** The source debug gun has two independently fired receivers on one stack. */
    public static final java.util.function.BiConsumer<net.minecraft.world.item.ItemStack,
            com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext> LAMBDA_DEBUG_DECIDER = (stack, ctx) -> {
        int index = ctx.configIndex;
        com.hbm.items.weapon.sedna.ItemGunBaseNT.GunState lastState =
                com.hbm.items.weapon.sedna.ItemGunBaseNT.getState(stack, index);
        GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
        GunStateDecider.deciderStandardClearJam(stack, lastState, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 1, index);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> com.hbm.items.weapon.sedna.ItemGunBaseNT.getPrimary(stack, index)
                        && com.hbm.items.weapon.sedna.ItemGunBaseNT.getMode(stack, index) == 0);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 1, index,
                () -> com.hbm.items.weapon.sedna.ItemGunBaseNT.getSecondary(stack, index)
                        && com.hbm.items.weapon.sedna.ItemGunBaseNT.getMode(stack, index) == 0);
    };

    /** Canonical old-HBM order. Ammo registration flattens these metadata values into item ids. */
    public enum EnumAmmo {
        STONE, STONE_AP, STONE_IRON, STONE_SHOT,
        M357_BP, M357_SP, M357_FMJ, M357_JHP, M357_AP, M357_EXPRESS,
        M44_BP, M44_SP, M44_FMJ, M44_JHP, M44_AP, M44_EXPRESS,
        P22_SP, P22_FMJ, P22_JHP, P22_AP,
        P9_SP, P9_FMJ, P9_JHP, P9_AP,
        R556_SP, R556_FMJ, R556_JHP, R556_AP,
        R762_SP, R762_FMJ, R762_JHP, R762_AP, R762_DU,
        BMG50_SP, BMG50_FMJ, BMG50_JHP, BMG50_AP, BMG50_DU,
        B75, B75_INC, B75_EXP,
        G12_BP, G12_BP_MAGNUM, G12_BP_SLUG, G12, G12_SLUG, G12_FLECHETTE,
        G12_MAGNUM, G12_EXPLOSIVE, G12_PHOSPHORUS,
        G26_FLARE, G26_FLARE_SUPPLY, G26_FLARE_WEAPON,
        G40_HE, G40_HEAT, G40_DEMO, G40_INC, G40_PHOSPHORUS,
        ROCKET_HE, ROCKET_HEAT, ROCKET_DEMO, ROCKET_INC, ROCKET_PHOSPHORUS,
        FLAME_DIESEL, FLAME_GAS, FLAME_NAPALM, FLAME_BALEFIRE,
        CAPACITOR, CAPACITOR_OVERCHARGE, CAPACITOR_IR, TAU_URANIUM,
        COIL_TUNGSTEN, COIL_FERROURANIUM,
        NUKE_STANDARD, NUKE_DEMO, NUKE_HIGH, NUKE_TOTS, NUKE_HIVE,
        G10, G10_SHRAPNEL, G10_DU, G10_SLUG,
        R762_HE, BMG50_HE, G10_EXPLOSIVE,
        P45_SP, P45_FMJ, P45_JHP, P45_AP, P45_DU,
        CT_HOOK, CT_MORTAR, CT_MORTAR_CHARGE, NUKE_BALEFIRE, BMG50_SM;
        public static final EnumAmmo[] VALUES = values();
        /** Creative/reload presentation order from original-hbm; omitted secret and unsupported entries stay registered. */
        public static final EnumAmmo[] order = {
            STONE, STONE_AP, STONE_IRON, STONE_SHOT,
            M357_BP, M357_SP, M357_FMJ, M357_JHP, M357_AP, M357_EXPRESS,
            M44_BP, M44_SP, M44_FMJ, M44_JHP, M44_AP, M44_EXPRESS,
            P22_SP, P22_FMJ, P22_JHP, P22_AP, P9_SP, P9_FMJ, P9_JHP, P9_AP,
            P45_SP, P45_FMJ, P45_JHP, P45_AP, P45_DU,
            R556_SP, R556_FMJ, R556_JHP, R556_AP,
            R762_SP, R762_FMJ, R762_JHP, R762_AP, R762_DU, R762_HE,
            BMG50_SP, BMG50_FMJ, BMG50_JHP, BMG50_AP, BMG50_DU, BMG50_SM, BMG50_HE,
            B75, B75_INC, B75_EXP,
            G12_BP, G12_BP_MAGNUM, G12_BP_SLUG, G12, G12_SLUG, G12_FLECHETTE,
            G12_MAGNUM, G12_EXPLOSIVE, G12_PHOSPHORUS,
            G10, G10_SHRAPNEL, G10_DU, G10_SLUG, G10_EXPLOSIVE,
            G26_FLARE, G26_FLARE_SUPPLY, G26_FLARE_WEAPON,
            G40_HE, G40_HEAT, G40_DEMO, G40_INC, G40_PHOSPHORUS,
            ROCKET_HE, ROCKET_HEAT, ROCKET_DEMO, ROCKET_INC, ROCKET_PHOSPHORUS,
            FLAME_DIESEL, FLAME_GAS, FLAME_NAPALM, FLAME_BALEFIRE,
            CAPACITOR, CAPACITOR_OVERCHARGE, CAPACITOR_IR, TAU_URANIUM,
            COIL_TUNGSTEN, COIL_FERROURANIUM,
            NUKE_STANDARD, NUKE_DEMO, NUKE_HIGH, NUKE_TOTS, NUKE_HIVE, NUKE_BALEFIRE,
            CT_HOOK, CT_MORTAR, CT_MORTAR_CHARGE
        };
        public EnumAmmo[] getOrder() { return order; }
    }

    public enum EnumAmmoSecret {
        FOLLY_SM, FOLLY_NUKE, M44_EQUESTRIAN, G12_EQUESTRIAN, BMG50_EQUESTRIAN,
        P35_800, BMG50_BLACK, P35_800_BL;
        public static final EnumAmmoSecret[] VALUES = values();
    }

    public enum EnumModTest {
        FIRERATE, DAMAGE, MULTI, OVERRIDE_2_5, OVERRIDE_5, OVERRIDE_7_5, OVERRIDE_10,
        OVERRIDE_12_5, OVERRIDE_15, OVERRIDE_20;
        public static final EnumModTest[] VALUES = values();
    }

    public enum EnumModGeneric {
        IRON_DAMAGE, IRON_DURA, STEEL_DAMAGE, STEEL_DURA, DURA_DAMAGE, DURA_DURA,
        DESH_DAMAGE, DESH_DURA, WSTEEL_DAMAGE, WSTEEL_DURA, FERRO_DAMAGE, FERRO_DURA,
        TCALLOY_DAMAGE, TCALLOY_DURA, BIGMT_DAMAGE, BIGMT_DURA, BRONZE_DAMAGE, BRONZE_DURA;
        public static final EnumModGeneric[] VALUES = values();
    }

    public enum EnumModSpecial {
        SILENCER, SCOPE, SAW, GREASEGUN, SLOWDOWN, SPEEDUP, CHOKE, SPEEDLOADER,
        FURNITURE_GREEN, FURNITURE_BLACK, BAYONET, STACK_MAG, SKIN_SATURNITE,
        LAS_SHOTGUN, LAS_CAPACITOR, LAS_AUTO, NICKEL, DOUBLOONS, DRILL_HSS,
        DRILL_WEAPONSTEEL, DRILL_TCALLOY, DRILL_SATURNITE, ENGINE_DIESEL,
        ENGINE_AVIATION, ENGINE_ELECTRIC, ENGINE_TURBO, MAGNET, SIFTER, CANISTERS;
        public static final EnumModSpecial[] VALUES = values();
    }

    public enum EnumModCaliber {
        P9, P45, P22, M357, M44, R556, R762, BMG50;
        public static final EnumModCaliber[] VALUES = values();
    }
}
