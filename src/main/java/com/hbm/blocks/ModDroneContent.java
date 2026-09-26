package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.api.tab.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.tileentity.network.RequestNetwork;
import dev.golem.core.data.GolemComponent;
import dev.golem.core.net.GolemNetwork;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.BlockPos;
import com.hbm.inventory.ModDroneMenus;
import com.hbm.items.tool.ItemDroneLinker;
import com.hbm.packet.DroneLinePayload;
import com.hbm.tileentity.ModDroneBlockEntities;

/**
 * One common bootstrap hook for the delivery drone blocks' feature-local registries, the linker item, and the
 * request network's server-tick sweep (1.7 {@code ModEventHandler} calling {@code RequestNetwork.updateEntries}).
 */
public final class ModDroneContent {
    private ModDroneContent() {}

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    /** 1.7 {@code ModItems.drone_linker}: stack size 1, consumables tab. */
    public static final RegistryObject<ItemDroneLinker> DRONE_LINKER =
            ITEMS.registerItem("drone_linker", ItemDroneLinker::new, p -> p.stacksTo(1));

    /** The linker's remembered stop; 1.7 kept it as x/y/z on the stack's NBT. */
    public static final GolemComponent<BlockPos> LINKER_POSITION =
            GolemComponent.of(Constants.MOD_ID, "drone_linker_position", BlockPos.CODEC);

    static {
        ModTabs.addTo(HbmTab.CONSUMABLES, DRONE_LINKER);
    }

    private static boolean ticking;

    public static void init() {
        ModDroneBlocks.init();
        ModDroneBlockEntities.init();
        ModDroneMenus.init();
        DroneDeclarations.declare();
        GolemNetwork.declarePlayToClient(DroneLinePayload.CODEC);
        if (!ticking) {
            ticking = true;
            Lifecycle.onServerTick(RequestNetwork::updateEntries);
        }
    }
}
