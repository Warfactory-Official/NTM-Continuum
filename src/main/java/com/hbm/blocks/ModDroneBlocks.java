package com.hbm.blocks;

import com.hbm.registration.BlockTag;
import com.hbm.registration.Loot;
import com.hbm.registration.Preset;
import com.hbm.registration.Presets;
import com.hbm.registration.Reg;
import dev.golem.core.dejson.Models;
import dev.golem.core.dejson.Slots;
import dev.golem.core.dejson.Variants;
import com.hbm.Constants;
import com.hbm.creativetabs.HbmTab;
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

/**
 * Feature-local block and block-item registry for the delivery drone blocks; 1.7: every one 0.1/10, machine tab.
 * Models, blockstates and loot are stated on each registration (Reg; emitted by Dejson at build time): the crates
 * are bottom/top/side cubes over the 1.7 sprites; the two waypoints ride the radio torch's element model over
 * their own sprite, mounted on any side, and their items are that flat sprite.
 */
public final class ModDroneBlocks {
    private ModDroneBlocks() {}

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Constants.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Constants.MOD_ID);
    private static final Reg REG = Reg.of(BLOCKS, ITEMS);

    /** A bottom/top/side cube over {@code hbm:blocks/<id>_bottom|_side|_top}; drops itself, pickaxe-mined. */
    private static final Preset CRATE = handle -> handle
            .transform(Presets.cubeBottomTop("hbm:blocks/" + handle.path() + "_bottom", "hbm:blocks/" + handle.path() + "_side",
                    "hbm:blocks/" + handle.path() + "_top"))
            .loot(Loot.self())
            .tag(BlockTag.PICKAXE);
    /** The radio torch's element model over the waypoint's own sprite, mounted on any side; drops itself. */
    private static final Preset WAYPOINT = handle -> handle
            .transform(Presets.shape(Models.renderType("minecraft:cutout", Models.parented("hbm:block/radio_torch_template",
                    Slots.of("torch", "hbm:blocks/" + handle.path())))).states(Variants.facingUp()).noItem())
            .loot(Loot.self());

    public static final RegistryObject<DroneWaypoint> DRONE_WAYPOINT = REG.block(
            "drone_waypoint", DroneWaypoint::new,
            p -> p.strength(0.1F, 10.0F).sound(SoundType.WOOD).noCollision().noOcclusion())
            .transform(WAYPOINT);
    public static final RegistryObject<DroneCrate> DRONE_CRATE = REG.block(
            "drone_crate", DroneCrate::new,
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(CRATE); // 1.7 Material.iron
    public static final RegistryObject<DroneWaypointRequest> DRONE_WAYPOINT_REQUEST = REG.block(
            "drone_waypoint_request", DroneWaypointRequest::new,
            p -> p.strength(0.1F, 10.0F).sound(SoundType.WOOD).noCollision().noOcclusion())
            .transform(WAYPOINT);
    public static final RegistryObject<DroneDock> DRONE_DOCK = REG.block(
            "drone_dock", p -> new DroneDock(p, DroneDock.Type.DOCK),
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(CRATE); // 1.7 Material.iron
    public static final RegistryObject<DroneDock> DRONE_CRATE_PROVIDER = REG.block(
            "drone_crate_provider", p -> new DroneDock(p, DroneDock.Type.PROVIDER),
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(CRATE); // 1.7 Material.iron
    public static final RegistryObject<DroneDock> DRONE_CRATE_REQUESTER = REG.block(
            "drone_crate_requester", p -> new DroneDock(p, DroneDock.Type.REQUESTER),
            p -> p.strength(0.1F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops())
            .transform(CRATE); // 1.7 Material.iron

    static {
        descItem("drone_waypoint", DRONE_WAYPOINT, 5).transform(Presets.sprite("hbm:blocks/drone_waypoint"));
        descItem("drone_crate", DRONE_CRATE, 3);
        tabItem("drone_waypoint_request", DRONE_WAYPOINT_REQUEST).transform(Presets.sprite("hbm:blocks/drone_waypoint_request"));
        descItem("drone_dock", DRONE_DOCK, 3);
        descItem("drone_crate_provider", DRONE_CRATE_PROVIDER, 2);
        descItem("drone_crate_requester", DRONE_CRATE_REQUESTER, 2);
    }

    /** 1.7 ITooltipProvider blocks: a block item carrying the description lines. */
    private static Reg.Handle<DescBlockItem> descItem(String name, RegistryObject<? extends Block> block, int lines) {
        return REG.item(name, p -> new DescBlockItem(block.get(), p, name, lines), Compat::blockDescription)
                .tab(HbmTab.MACHINES);
    }

    private static Reg.Handle<BlockItem> tabItem(String name, RegistryObject<? extends Block> block) {
        return REG.blockItem(block).tab(HbmTab.MACHINES);
    }

    public static void init() {}
}
