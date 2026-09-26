package com.hbm.blocks.turret;

import com.hbm.Constants;
import dev.golem.core.multiblock.MultiblockCell;
import com.hbm.api.tab.HbmTab;
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
            p -> MultiblockCell.rigid(p).strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false));

    /** Shared by the artillery guns, and likewise never given to a player. */
    public static final RegistryObject<BlockTurretEmplacement> turret_emplacement = BLOCKS.registerBlock(
            "turret_emplacement", BlockTurretEmplacement::new,
            p -> MultiblockCell.rigid(p).strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion());

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

    public static final RegistryObject<TurretTauon> turret_tauon = BLOCKS.registerBlock(
            "turret_tauon", TurretTauon::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretMaxwell> turret_maxwell = BLOCKS.registerBlock(
            "turret_maxwell", TurretMaxwell::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretJeremy> turret_jeremy = BLOCKS.registerBlock(
            "turret_jeremy", TurretJeremy::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretRichard> turret_richard = BLOCKS.registerBlock(
            "turret_richard", TurretRichard::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretFritz> turret_fritz = BLOCKS.registerBlock(
            "turret_fritz", TurretFritz::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretHIMARS> turret_himars = BLOCKS.registerBlock(
            "turret_himars", TurretHIMARS::new, ModTurretBlocks::turretProperties);

    public static final RegistryObject<TurretArty> turret_arty = BLOCKS.registerBlock(
            "turret_arty", TurretArty::new, ModTurretBlocks::turretProperties);

    static {
        blockItem("turret_chekhov", turret_chekhov);
        blockItem("turret_friendly", turret_friendly);
        blockItem("turret_sentry", turret_sentry);
        blockItem("turret_sentry_damaged", turret_sentry_damaged);
        blockItem("turret_howard", turret_howard);
        blockItem("turret_howard_damaged", turret_howard_damaged);
        blockItem("turret_tauon", turret_tauon);
        blockItem("turret_maxwell", turret_maxwell);
        blockItem("turret_jeremy", turret_jeremy);
        blockItem("turret_richard", turret_richard);
        blockItem("turret_fritz", turret_fritz);
        blockItem("turret_himars", turret_himars);
        blockItem("turret_arty", turret_arty);
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
