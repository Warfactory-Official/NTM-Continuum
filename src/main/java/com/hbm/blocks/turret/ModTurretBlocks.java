package com.hbm.blocks.turret;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;

/** The turret pad cell and the turrets that stand on it. */
public final class ModTurretBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** Shared by every turret, and never given to a player: it has no block item and no loot. */
    public static final RegistryObject<BlockTurretPad> turret_pad = BLOCKS.registerBlock(
            "turret_pad", BlockTurretPad::new,
            p -> p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false));

    public static final RegistryObject<TurretChekhov> turret_chekhov = BLOCKS.registerBlock(
            "turret_chekhov", TurretChekhov::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretFriendly> turret_friendly = BLOCKS.registerBlock(
            "turret_friendly", TurretFriendly::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretSentry> turret_sentry = BLOCKS.registerBlock(
            "turret_sentry", TurretSentry::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretSentryDamaged> turret_sentry_damaged = BLOCKS.registerBlock(
            "turret_sentry_damaged", TurretSentryDamaged::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretHoward> turret_howard = BLOCKS.registerBlock(
            "turret_howard", TurretHoward::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretHowardDamaged> turret_howard_damaged = BLOCKS.registerBlock(
            "turret_howard_damaged", TurretHowardDamaged::new, ModTurretBlocks::turretProperties);

    static {
        blockItem("turret_chekhov", turret_chekhov);
        blockItem("turret_friendly", turret_friendly);
        blockItem("turret_sentry", turret_sentry);
        blockItem("turret_sentry_damaged", turret_sentry_damaged);
        blockItem("turret_howard", turret_howard);
        blockItem("turret_howard_damaged", turret_howard_damaged);
    }

    private ModTurretBlocks() {}

    private static void blockItem(String name, RegistryObject<? extends net.minecraft.world.level.block.Block> turret) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, turret);
        ModTabs.addTo(HbmTab.WEAPONS, item);
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties turretProperties(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                .isSuffocating((state, world, pos) -> false)
                .isViewBlocking((state, world, pos) -> false);
    }

    public static void init() {}
}
