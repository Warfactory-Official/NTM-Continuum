package com.hbm.blocks;

import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.blocks.machine.MachineSatDock;
import com.hbm.blocks.machine.MachineSatLinker;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;

/**
 * The satellite family's blocks, as 1.7 {@code ModBlocks} declared them: both {@code Material.iron}, hardness 5,
 * resistance 10, on the missile tab. {@code dummy_plate_cargo} is folded into {@code sat_dock}'s own cells.
 * Models, blockstates, loot and mining tags are stated on each registration (Reg; emitted by Dejson at build
 * time); the cargo pad's OBJ item definition is typed data in {@code SatelliteDeclarations}.
 */
public final class ModSatelliteBlocks {

    private ModSatelliteBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    public static final RegistryObject<MachineSatLinker> machine_satlinker =
            REG.block("machine_satlinker", MachineSatLinker::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
                    // 1.7 MachineSatLinker.getIcon: machine_satlinker_top on top AND bottom, _side around
                    .transform(Presets.cubeBottomTop("hbm:blocks/machine_satlinker_top", "hbm:blocks/machine_satlinker_side",
                            "hbm:blocks/machine_satlinker_top"))
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE);
    /** Renderer-drawn: the block model is empty, so nothing may cull or suffocate against it. */
    public static final RegistryObject<MachineSatDock> sat_dock =
            REG.block("sat_dock", MachineSatDock::new,
                    p -> p.strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()
                            .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false))
                    .transform(Presets.rendered(Variants.product(Variants.uniform("facing", "north", "east", "south", "west"),
                            Variants.uniform("cell", "false", "true"))))
                    .loot(Loot.self())
                    .tag(BlockTag.PICKAXE);

    static {
        item("machine_satlinker", machine_satlinker);
        item("sat_dock", sat_dock);
    }

    private static void item(String name, RegistryObject<? extends Block> block) {
        REG.blockItem(block, Compat::blockDescription).tab(HbmTab.MISSILES);
    }

    public static void init() {}
}
