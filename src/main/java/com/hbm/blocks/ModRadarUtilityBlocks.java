package com.hbm.blocks;

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
import com.hbm.creativetabs.ModTabs;
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
 */
public final class ModRadarUtilityBlocks {

    private ModRadarUtilityBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    private static UnaryOperator<BlockBehaviour.Properties> iron(float hardness, float resistance) {
        return p -> p.strength(hardness, resistance).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    /** Renderer-drawn: the block model is empty, so nothing may cull or suffocate against it. */
    private static UnaryOperator<BlockBehaviour.Properties> rendered(float hardness, float resistance) {
        return p -> iron(hardness, resistance).apply(p).noOcclusion()
                .isSuffocating((s, w, pos) -> false).isViewBlocking((s, w, pos) -> false);
    }

    public static final RegistryObject<MachineRadar> machine_radar =
            BLOCKS.registerBlock("machine_radar", MachineRadar::new, rendered(5.0F, 10.0F));
    public static final RegistryObject<MachineRadarLarge> machine_radar_large =
            BLOCKS.registerBlock("machine_radar_large", MachineRadarLarge::new, rendered(5.0F, 10.0F));
    public static final RegistryObject<MachineRadarScreen> radar_screen =
            BLOCKS.registerBlock("radar_screen", MachineRadarScreen::new, rendered(5.0F, 10.0F));
    public static final RegistryObject<MachineForceField> machine_forcefield =
            BLOCKS.registerBlock("machine_forcefield", MachineForceField::new, rendered(5.0F, 100.0F));
    public static final RegistryObject<MachineTeleporter> machine_teleporter =
            BLOCKS.registerBlock("machine_teleporter", MachineTeleporter::new, iron(5.0F, 10.0F));
    public static final RegistryObject<CompatBlock> teleanchor =
            BLOCKS.registerBlock("teleanchor", CompatBlock::new, iron(5.0F, 10.0F));
    public static final RegistryObject<MachineOrbus> machine_orbus =
            BLOCKS.registerBlock("machine_orbus", MachineOrbus::new, rendered(5.0F, 10.0F));
    public static final RegistryObject<BlockSeal> seal_controller =
            BLOCKS.registerBlock("seal_controller", BlockSeal::new, iron(10.0F, 100.0F));
    /** 1.7: hardness and resistance {@code Float.POSITIVE_INFINITY} - unbreakable and blast-proof. */
    public static final RegistryObject<BlockHatch> seal_hatch =
            BLOCKS.registerBlock("seal_hatch", BlockHatch::new,
                    p -> p.strength(-1.0F, 3600000.0F).sound(SoundType.METAL));
    public static final RegistryObject<SoyuzCapsule> soyuz_capsule =
            BLOCKS.registerBlock("soyuz_capsule", SoyuzCapsule::new, rendered(5.0F, 10.0F));

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
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block, Compat::blockDescription);
        if (tab != null) ModTabs.addTo(tab, item);
    }

    public static void init() {}
}
