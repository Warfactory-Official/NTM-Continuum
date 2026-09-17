package com.hbm.items.weapon.sedna.factory;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.ModDamageSources.DamageClass;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Submunitions a warhead throws rather than a gun. 1.7 kept these in the {@code @Deprecated} BulletConfiguration
 * registry (BulletConfigSyncingUtil) alongside gun ammo; sedna's BulletConfig replaces that registry, and warhead
 * payloads get their own factory because nothing about them is loadable into a magazine.
 */
public final class XFactoryWarhead {

    /** 1.7 GunEnergyFactory.getTurbineConfig - the turbine warhead's blade shower. */
    public static BulletConfig warhead_turbine;

    /** 1.7 rolled dmgMin 100..dmgMax 150 per blade; sedna's config carries a multiplier, so the base rides here. */
    public static final float TURBINE_BLADE_DAMAGE = 125F;

    private XFactoryWarhead() {
    }

    public static void init() {

        // 1.7: velocity 1, no gravity, maxAge 200, destroysBlocks, no ricochet.
        // STYLE_BLADE was a renderer choice with no sedna equivalent.
        warhead_turbine = new BulletConfig()
                .setVel(1F).setGrav(0F).setLife(200).setSpread(0F)
                .setRicochetCount(0).setupDamageClass(DamageClass.PHYSICAL)
                .setOnImpact((blade, mop) -> {
                    if(!(mop instanceof BlockHitResult hit)) return;
                    // 1.7 destroysBlocks: anything up to hardness 120 is cut clean through
                    BlockState state = blade.level().getBlockState(hit.getBlockPos());
                    if(state.getDestroySpeed(blade.level(), hit.getBlockPos()) <= 120F) {
                        blade.level().destroyBlock(hit.getBlockPos(), false);
                    }
                });
    }
}
