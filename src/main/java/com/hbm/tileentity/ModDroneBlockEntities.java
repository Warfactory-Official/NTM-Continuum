package com.hbm.tileentity;

import com.hbm.Constants;
import dev.golem.core.registry.BlockEntities;
import dev.golem.core.registry.DeferredRegister;
import dev.golem.core.registry.RegistryObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.blocks.ModDroneBlocks;
import com.hbm.tileentity.network.TileEntityDroneCrate;
import com.hbm.tileentity.network.TileEntityDroneDock;
import com.hbm.tileentity.network.TileEntityDroneProvider;
import com.hbm.tileentity.network.TileEntityDroneRequester;
import com.hbm.tileentity.network.TileEntityDroneWaypoint;
import com.hbm.tileentity.network.TileEntityDroneWaypointRequest;

/** Feature-local block-entity types for the delivery drone blocks. */
public final class ModDroneBlockEntities {
    private ModDroneBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Constants.MOD_ID);

    public static final RegistryObject<BlockEntityType<TileEntityDroneCrate>> DRONE_CRATE =
            BLOCK_ENTITIES.register("drone_crate", () -> BlockEntities.serverTicking(
                    TileEntityDroneCrate::new,
                    (level, pos, state, be) -> be.serverTick(), ModDroneBlocks.DRONE_CRATE.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDroneDock>> DRONE_DOCK =
            BLOCK_ENTITIES.register("drone_dock", () -> BlockEntities.serverTicking(
                    TileEntityDroneDock::new,
                    (level, pos, state, be) -> be.serverTick(), ModDroneBlocks.DRONE_DOCK.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDroneProvider>> DRONE_PROVIDER =
            BLOCK_ENTITIES.register("drone_crate_provider", () -> BlockEntities.serverTicking(
                    TileEntityDroneProvider::new,
                    (level, pos, state, be) -> be.serverTick(), ModDroneBlocks.DRONE_CRATE_PROVIDER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDroneRequester>> DRONE_REQUESTER =
            BLOCK_ENTITIES.register("drone_crate_requester", () -> BlockEntities.serverTicking(
                    TileEntityDroneRequester::new,
                    (level, pos, state, be) -> be.serverTick(), ModDroneBlocks.DRONE_CRATE_REQUESTER.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDroneWaypoint>> DRONE_WAYPOINT =
            BLOCK_ENTITIES.register("drone_waypoint", () -> BlockEntities.ticking(
                    TileEntityDroneWaypoint::new,
                    (level, pos, state, be) -> be.clientTick(),
                    (level, pos, state, be) -> be.serverTick(), ModDroneBlocks.DRONE_WAYPOINT.get()));
    public static final RegistryObject<BlockEntityType<TileEntityDroneWaypointRequest>> DRONE_WAYPOINT_REQUEST =
            BLOCK_ENTITIES.register("drone_waypoint_request", () -> BlockEntities.serverTicking(
                    TileEntityDroneWaypointRequest::new,
                    (level, pos, state, be) -> be.serverTick(), ModDroneBlocks.DRONE_WAYPOINT_REQUEST.get()));

    public static void init() {}
}
