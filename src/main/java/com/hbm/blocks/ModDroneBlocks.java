package com.hbm.blocks;

import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
import com.hbm.creativetabs.ModTabs;
import com.hbm.items.block.DescBlockItem;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.world.Compat;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import com.hbm.blocks.network.DroneCrate;
import com.hbm.blocks.network.DroneDock;
import com.hbm.blocks.network.DroneWaypoint;
import com.hbm.blocks.network.DroneWaypointRequest;

/** Feature-local block and block-item registry for the delivery drone blocks; 1.7: every one 0.1/10, machine tab. */
public final class ModDroneBlocks {
    private ModDroneBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);

    public static final RegistryObject<DroneWaypoint> DRONE_WAYPOINT = BLOCKS.registerBlock(
            "drone_waypoint", DroneWaypoint::new,
            p -> p.strength(0.1F, 10.0F).sound(SoundType.WOOD).noCollision().noOcclusion());
    public static final RegistryObject<DroneCrate> DRONE_CRATE = BLOCKS.registerBlock(
            "drone_crate", DroneCrate::new,
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()); // 1.7 Material.iron
    public static final RegistryObject<DroneWaypointRequest> DRONE_WAYPOINT_REQUEST = BLOCKS.registerBlock(
            "drone_waypoint_request", DroneWaypointRequest::new,
            p -> p.strength(0.1F, 10.0F).sound(SoundType.WOOD).noCollision().noOcclusion());
    public static final RegistryObject<DroneDock> DRONE_DOCK = BLOCKS.registerBlock(
            "drone_dock", p -> new DroneDock(p, DroneDock.Type.DOCK),
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()); // 1.7 Material.iron
    public static final RegistryObject<DroneDock> DRONE_CRATE_PROVIDER = BLOCKS.registerBlock(
            "drone_crate_provider", p -> new DroneDock(p, DroneDock.Type.PROVIDER),
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()); // 1.7 Material.iron
    public static final RegistryObject<DroneDock> DRONE_CRATE_REQUESTER = BLOCKS.registerBlock(
            "drone_crate_requester", p -> new DroneDock(p, DroneDock.Type.REQUESTER),
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()); // 1.7 Material.iron

    static {
        descItem("drone_waypoint", DRONE_WAYPOINT, 5);
        descItem("drone_crate", DRONE_CRATE, 3);
        tabItem("drone_waypoint_request", DRONE_WAYPOINT_REQUEST);
        descItem("drone_dock", DRONE_DOCK, 3);
        descItem("drone_crate_provider", DRONE_CRATE_PROVIDER, 2);
        descItem("drone_crate_requester", DRONE_CRATE_REQUESTER, 2);
    }

    /** 1.7 ITooltipProvider blocks: a block item carrying the description lines. */
    private static void descItem(String name, RegistryObject<? extends Block> block, int lines) {
        RegistryObject<DescBlockItem> item = ITEMS.registerItem(name,
                p -> new DescBlockItem(block.get(), p, name, lines), Compat::blockDescription);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    private static void tabItem(String name, RegistryObject<? extends Block> block) {
        RegistryObject<BlockItem> item = ITEMS.registerBlockItem(name, block);
        ModTabs.addTo(HbmTab.MACHINES, item);
    }

    public static void init() {}
}
