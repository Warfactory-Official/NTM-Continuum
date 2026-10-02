package com.hbm.blocks.turret;

import com.hbm.registration.BlockTag;
import com.hbm.Constants;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.creativetabs.HbmTab;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.level.block.SoundType;

/** The turret pad cell and the turrets that stand on it. */
public final class ModTurretBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    // The turrets' item definitions are typed OBJ data (ObjItemDeclarations).
    /** A turret core: drawn by its renderer, turned four ways; drops itself. */
    private static final Preset TURRET = Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west"))
            .and(handle -> handle.loot(Loot.self()));

    /** Shared by every turret, and never given to a player: it has no block item and no loot. */
    public static final RegistryObject<BlockTurretPad> turret_pad = REG.block(
            "turret_pad", BlockTurretPad::new,
            p -> BlockMultiblockCell.rigid(p).strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                    .isSuffocating((state, world, pos) -> false)
                    .isViewBlocking((state, world, pos) -> false))
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    /** Shared by the artillery guns, and likewise never given to a player. */
    public static final RegistryObject<BlockTurretEmplacement> turret_emplacement = REG.block(
            "turret_emplacement", BlockTurretEmplacement::new,
            p -> BlockMultiblockCell.rigid(p).strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion())
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretChekhov> turret_chekhov = REG.block(
            "turret_chekhov", TurretChekhov::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretFriendly> turret_friendly = REG.block(
            "turret_friendly", TurretFriendly::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretSentry> turret_sentry = REG.block(
            "turret_sentry", TurretSentry::new, ModTurretBlocks::turretProperties)
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretSentryDamaged> turret_sentry_damaged = REG.block(
            "turret_sentry_damaged", TurretSentryDamaged::new, ModTurretBlocks::turretProperties)
            .transform(Presets.rendered(Variants.simple()))
            .loot(Loot.nothing())
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretHoward> turret_howard = REG.block(
            "turret_howard", TurretHoward::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretHowardDamaged> turret_howard_damaged = REG.block(
            "turret_howard_damaged", TurretHowardDamaged::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretTauon> turret_tauon = REG.block(
            "turret_tauon", TurretTauon::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretMaxwell> turret_maxwell = REG.block(
            "turret_maxwell", TurretMaxwell::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretJeremy> turret_jeremy = REG.block(
            "turret_jeremy", TurretJeremy::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretRichard> turret_richard = REG.block(
            "turret_richard", TurretRichard::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretFritz> turret_fritz = REG.block(
            "turret_fritz", TurretFritz::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretHIMARS> turret_himars = REG.block(
            "turret_himars", TurretHIMARS::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

    public static final RegistryObject<TurretArty> turret_arty = REG.block(
            "turret_arty", TurretArty::new, ModTurretBlocks::turretProperties)
            .transform(TURRET)
            .tag(BlockTag.PICKAXE)
            .tag(BlockTag.NEEDS_IRON_TOOL);

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
        REG.blockItem(turret).tab(HbmTab.WEAPONS);
    }

    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties turretProperties(
            net.minecraft.world.level.block.state.BlockBehaviour.Properties p) {
        return p.strength(5.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                .isSuffocating((state, world, pos) -> false)
                .isViewBlocking((state, world, pos) -> false);
    }

    public static void init() {}
}
