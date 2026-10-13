package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.blocks.bomb.CompactLauncher;
import com.hbm.blocks.bomb.LaunchPadLarge;
import com.hbm.blocks.bomb.LaunchPadRusted;
import com.hbm.blocks.bomb.LaunchTable;
import com.hbm.blocks.machine.MachineMissileAssembly;
import com.hbm.blocks.machine.struct.BlockStruct;
import com.hbm.creativetabs.HbmTab;
import com.hbm.tileentity.ModCustomMissileBlockEntities;
import com.hbm.tileentity.machine.TileEntityMultiblock;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/**
 * The custom missile system's blocks and the two erector/silo pads, as 1.7 {@code ModBlocks} declared them: all
 * {@code Material.iron}, hardness 5, resistance 10, on the missile tab.
 *
 * <p>Not registered: {@code dummy_plate_compact_launcher}, {@code dummy_port_compact_launcher},
 * {@code dummy_plate_launch_table} and {@code dummy_port_launch_table}. They were upstream's dummy blocks for the
 * two launchers and are folded into the launchers' own cells here, as every other port multiblock folds its
 * dummies (see {@link CompactLauncher} and {@link LaunchTable}).
 */
public final class ModCustomMissileBlocks {

    private ModCustomMissileBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    private static final UnaryOperator<BlockBehaviour.Properties> IRON =
            p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    /** Renderer-drawn machines: the block model is empty, so nothing may cull or suffocate against it. */
    private static final UnaryOperator<BlockBehaviour.Properties> RENDERED =
            p -> IRON.apply(p).noOcclusion().isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);

    /** The launchers and pads: folded multiblocks drawn by their renderers, an empty placeholder blockstate. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"), Variants.uniform("cell", "false", "true")));

    public static final RegistryObject<BlockStruct> struct_launcher_core = REG.block("struct_launcher_core",
                    p -> new BlockStruct(p, TileEntityMultiblock::new, () -> ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE.get()),
                    p -> IRON.apply(p).noOcclusion())
            .transform(Presets.cubeAll().noItem())
            .transform(Presets.shares("hbm:block/struct_launcher_core"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockStruct> struct_launcher_core_large = REG.block("struct_launcher_core_large",
                    p -> new BlockStruct(p, TileEntityMultiblock::new, () -> ModCustomMissileBlockEntities.STRUCT_LAUNCHER_CORE_LARGE.get()),
                    p -> IRON.apply(p).noOcclusion())
            .transform(Presets.cubeAll().noItem())
            .transform(Presets.shares("hbm:block/struct_launcher_core_large"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);

    public static final RegistryObject<MachineMissileAssembly> machine_missile_assembly =
            REG.block("machine_missile_assembly", MachineMissileAssembly::new, RENDERED)
                    .transform(Presets.rendered(Variants.uniform("facing", "north", "east", "south", "west")))
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE);
    // 1.7 getItemDropped: the launchers give back their core component; the rusted pad gives nothing.
    public static final RegistryObject<CompactLauncher> compact_launcher =
            REG.block("compact_launcher", CompactLauncher::new, RENDERED)
                    .transform(FOLDED)
                    .loot(Loot.only("hbm:struct_launcher_core"))
                    .tag(BlockTag.PICKAXE);
    public static final RegistryObject<LaunchTable> launch_table =
            REG.block("launch_table", LaunchTable::new, RENDERED)
                    .transform(FOLDED)
                    .loot(Loot.only("hbm:struct_launcher_core_large"))
                    .tag(BlockTag.PICKAXE);
    /** The item is the typed silo OBJ definition in {@code CustomMissileDeclarations}. */
    public static final RegistryObject<LaunchPadRusted> launch_pad_rusted =
            REG.block("launch_pad_rusted", LaunchPadRusted::new, RENDERED)
                    .transform(FOLDED)
                    .loot(Loot.nothing())
                    .tag(BlockTag.PICKAXE);
    /** The item is the typed pad + erector OBJ definition in {@code CustomMissileDeclarations}. */
    public static final RegistryObject<LaunchPadLarge> launch_pad_large =
            REG.block("launch_pad_large", LaunchPadLarge::new, RENDERED)
                    .transform(FOLDED)
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE);

    static {
        // 1.7 ModBlocks registration order on the missile tab. The two launchers and the assembly station had render
        // type -1, but ItemRenderLibrary registered an OBJ item renderer for each, so their items are the typed OBJ
        // definitions in CustomMissileDeclarations, not the block's flat icon.
        REG.blockItem(struct_launcher_core, Compat::blockDescription).tab(HbmTab.MISSILES);
        REG.blockItem(struct_launcher_core_large, Compat::blockDescription).tab(HbmTab.MISSILES);
        REG.blockItem(machine_missile_assembly, Compat::blockDescription).tab(HbmTab.MISSILES);
        REG.blockItem(launch_pad_rusted, Compat::blockDescription).tab(HbmTab.MISSILES);
        REG.blockItem(launch_pad_large, Compat::blockDescription).tab(HbmTab.MISSILES);
        REG.blockItem(compact_launcher, Compat::blockDescription).tab(HbmTab.MISSILES);
        REG.blockItem(launch_table, Compat::blockDescription).tab(HbmTab.MISSILES);
    }

    public static void init() {}
}
