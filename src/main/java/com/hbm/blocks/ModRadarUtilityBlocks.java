package com.hbm.blocks;

import dev.golem.core.registry.BlockTag;
import dev.golem.core.registry.Loot;
import dev.golem.core.registry.Preset;
import com.hbm.registration.CarriedDrops;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.blocks.machine.BlockHatch;
import com.hbm.blocks.machine.BlockSeal;
import com.hbm.blocks.machine.MachineForceField;
import com.hbm.blocks.machine.MachineOrbus;
import com.hbm.blocks.machine.MachineRadar;
import com.hbm.blocks.machine.MachineRadarLarge;
import com.hbm.blocks.machine.MachineRadarScreen;
import com.hbm.blocks.machine.MachineTeleporter;
import com.hbm.blocks.machine.SoyuzCapsule;
import com.hbm.creativetabs.HbmTab;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import dev.golem.core.world.Compat;
import dev.golem.core.world.CompatBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.UnaryOperator;

/**
 * The radar and utility machines' blocks, as 1.7 {@code ModBlocks} declared them: all {@code Material.iron}.
 * The radars, the screen, the forcefield and the capsule sit on the missile tab; the teleporter, the anchor, the
 * orbus and the seal controller on the machine tab; the hatch on no tab at all (only a controller places it).
 *
 * <p>Models, blockstates, loot and mining tags are stated on each registration (Reg; emitted by Dejson at build
 * time). The renderer-drawn blocks carry the empty placeholder model; their items are 1.7's
 * {@code ItemRenderBase} entries, typed OBJ item data in {@code RadarUtilityDeclarations}.
 */
public final class ModRadarUtilityBlocks {

    private ModRadarUtilityBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** Drops itself, pickaxe-mined. */
    private static final Preset SELF_PICKAXE = handle -> handle.loot(Loot.self()).tag(BlockTag.PICKAXE);
    /** Renderer-drawn folded machine: every state on the shared empty placeholder. */
    private static final Preset FOLDED = Presets.rendered(Variants.product(
            Variants.uniform("facing", "north", "east", "south", "west"),
            Variants.uniform("cell", "false", "true")));

    private static UnaryOperator<BlockBehaviour.Properties> iron(float hardness, float resistance) {
        return p -> p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Renderer-drawn: the block model is empty, so nothing may cull or suffocate against it. */
    private static UnaryOperator<BlockBehaviour.Properties> rendered(float hardness, float resistance) {
        return p -> iron(hardness, resistance).apply(p).noOcclusion()
                .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);
    }

    public static final RegistryObject<MachineRadar> machine_radar =
            REG.block("machine_radar", MachineRadar::new, rendered(5.0F, 6.0F))
                    .transform(Presets.rendered(Variants.simple()).and(SELF_PICKAXE));
    public static final RegistryObject<MachineRadarLarge> machine_radar_large =
            REG.block("machine_radar_large", MachineRadarLarge::new, rendered(5.0F, 6.0F))
                    .transform(FOLDED.and(SELF_PICKAXE));
    public static final RegistryObject<MachineRadarScreen> radar_screen =
            REG.block("radar_screen", MachineRadarScreen::new, rendered(5.0F, 6.0F))
                    .transform(FOLDED.and(SELF_PICKAXE));
    public static final RegistryObject<MachineForceField> machine_forcefield =
            REG.block("machine_forcefield", MachineForceField::new, rendered(5.0F, 60.0F))
                    .transform(Presets.rendered(Variants.simple()).and(SELF_PICKAXE));
    public static final RegistryObject<MachineTeleporter> machine_teleporter =
            REG.block("machine_teleporter", MachineTeleporter::new, iron(5.0F, 6.0F))
                    // 1.7 icons: teleporter_top/_bottom/_side
                    .transform(Presets.cubeBottomTop("hbm:blocks/teleporter_bottom", "hbm:blocks/teleporter_side",
                            "hbm:blocks/teleporter_top").and(SELF_PICKAXE));
    public static final RegistryObject<CompatBlock> teleanchor =
            REG.block("teleanchor", CompatBlock::new, iron(5.0F, 6.0F))
                    // the anchor's bottom is its side
                    .transform(Presets.cubeBottomTop("hbm:blocks/tele_anchor_side", "hbm:blocks/tele_anchor_side",
                            "hbm:blocks/tele_anchor_top").and(SELF_PICKAXE));
    public static final RegistryObject<MachineOrbus> machine_orbus =
            REG.block("machine_orbus", MachineOrbus::new, rendered(5.0F, 6.0F))
                    .transform(FOLDED)
                    .loot(CarriedDrops.ALWAYS)
                    .tag(BlockTag.PICKAXE);
    public static final RegistryObject<BlockSeal> seal_controller =
            REG.block("seal_controller", BlockSeal::new, iron(10.0F, 60.0F))
                    // 1.7 BlockSeal.getIcon: seal_frame everywhere but the one face that looks at the placer
                    .transform(Presets.orientable("hbm:blocks/seal_controller", "hbm:blocks/seal_frame", "hbm:blocks/seal_frame")
                            .states(Variants.product(Variants.facing(), Variants.uniform("powered", "false", "true")))
                            .and(SELF_PICKAXE));
    /** 1.7: hardness and resistance {@code Float.POSITIVE_INFINITY} - unbreakable and blast-proof. */
    public static final RegistryObject<BlockHatch> seal_hatch =
            REG.block("seal_hatch", BlockHatch::new,
                    p -> p.strength(-1.0F, Float.POSITIVE_INFINITY).sound(SoundType.METAL))
                    .transform(Presets.cubeAll("hbm:blocks/seal_hatch_3"))
                    .loot(Loot.nothing());
    public static final RegistryObject<SoyuzCapsule> soyuz_capsule =
            REG.block("soyuz_capsule", SoyuzCapsule::new, rendered(5.0F, 6.0F))
                    .transform(Presets.rendered(Variants.uniform("rusted", "false", "true")).and(SELF_PICKAXE));

    static {
        item("machine_radar", machine_radar, HbmTab.MISSILES);
        item("machine_radar_large", machine_radar_large, HbmTab.MISSILES);
        item("radar_screen", radar_screen, HbmTab.MISSILES);
        item("machine_forcefield", machine_forcefield, HbmTab.MISSILES);
        item("soyuz_capsule", soyuz_capsule, HbmTab.MISSILES);
        item("machine_teleporter", machine_teleporter, HbmTab.MACHINES);
        item("teleanchor", teleanchor, HbmTab.MACHINES);
        item("machine_orbus", machine_orbus, HbmTab.MACHINES);
        item("seal_controller", seal_controller, HbmTab.MACHINES);
        item("seal_hatch", seal_hatch, null);
    }

    private static void item(String name, RegistryObject<? extends Block> block, HbmTab tab) {
        Reg.Handle<BlockItem> item = REG.blockItem(block, Compat::blockDescription);
        if (tab != null) item.tab(tab);
    }

    public static void init() {}
}
