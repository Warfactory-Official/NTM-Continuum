package com.hbm.compat.opencomputers;

//? if opencomputers {
/*import com.hbm.compat.computercraft.Peripherals;
import com.hbm.compat.computercraft.SnapshotPeripheral;
import dev.golem.core.platform.Lifecycle;
import dev.golem.core.platform.NeoForgeEntrypoint;
import li.cil.oc.api.Driver;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/^*
 * Registry and lifecycle manager for OpenComputers: Rebooted peripherals and capabilities.
 ^/
public final class OpenComputersPeripherals {

    public static final BlockCapability<SidedEnvironment, Direction> SIDED_ENVIRONMENT =
            BlockCapability.createSided(dev.golem.core.Ids.of(OpenComputersCompat.MOD_ID, "sided_environment"), SidedEnvironment.class);

    public static final BlockCapability<Environment, Direction> ENVIRONMENT =
            BlockCapability.createSided(dev.golem.core.Ids.of(OpenComputersCompat.MOD_ID, "environment"), Environment.class);

    private static final Map<BlockEntity, OpenComputersEnvironment> BY_MACHINE = new IdentityHashMap<>();
    private static final List<OpenComputersEnvironment> LIVE = new ArrayList<>();

    private OpenComputersPeripherals() {}

    static void init() {
        // Register side-aware block driver for Adapter blocks
        Driver.add(new HbmDriverBlock());

        // Register NeoForge block capabilities for native OpenComputers cable connections
        //? if neoforge {
        NeoForgeEntrypoint.modBus().addListener((RegisterCapabilitiesEvent event) -> {
            Block[] blocks = BuiltInRegistries.BLOCK.stream()
                    .filter(Peripherals::carries)
                    .toArray(Block[]::new);
            if (blocks.length == 0) return;

            event.registerBlock(SIDED_ENVIRONMENT, (level, pos, state, be, side) ->
                    level instanceof ServerLevel server ? at(server, pos, state, be, side) : null, blocks);

            event.registerBlock(ENVIRONMENT, (level, pos, state, be, side) ->
                    level instanceof ServerLevel server ? at(server, pos, state, be, side) : null, blocks);
        });
        //?}

        Lifecycle.onServerStopping(server -> {
            for (OpenComputersEnvironment env : LIVE) {
                if (env.node() != null) {
                    env.node().remove();
                }
            }
            BY_MACHINE.clear();
            LIVE.clear();
        });
    }

    /^* The SidedEnvironment at {@code pos} through {@code side}, or null if unreachable. ^/
    public static @Nullable OpenComputersEnvironment at(ServerLevel level, BlockPos pos, BlockState state,
                                                        @Nullable BlockEntity be, @Nullable Direction side) {
        SnapshotPeripheral<?> peripheral = (SnapshotPeripheral<?>) Peripherals.at(level, pos, state, be, side);
        if (peripheral == null) return null;

        BlockEntity owner = (BlockEntity) peripheral.getTarget();
        if (owner == null || owner.isRemoved()) return null;

        return of(owner, peripheral);
    }

    /^* Finds the underlying SnapshotPeripheral if reachable at {@code pos} through {@code side}. ^/
    public static @Nullable SnapshotPeripheral<?> findPeripheral(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        BlockState state = level.getBlockState(pos);
        BlockEntity be = level.getBlockEntity(pos);
        return (SnapshotPeripheral<?>) Peripherals.at(level, pos, state, be, side);
    }

    /^* Whether an OpenComputers cable can connect at {@code pos} through {@code side}. ^/
    public static boolean canConnectAt(Level level, BlockPos pos, @Nullable Direction side) {
        if (level instanceof ServerLevel server) {
            return findPeripheral(server, pos, side) != null;
        }
        return Peripherals.carries(level.getBlockState(pos).getBlock());
    }

    private static OpenComputersEnvironment of(BlockEntity owner, SnapshotPeripheral<?> peripheral) {
        OpenComputersEnvironment env = BY_MACHINE.get(owner);
        if (env != null && !env.target().isRemoved()) {
            return env;
        }
        if (env != null && env.node() != null) {
            env.node().remove();
            LIVE.remove(env);
        }
        env = new OpenComputersEnvironment(owner, peripheral);
        BY_MACHINE.put(owner, env);
        LIVE.add(env);
        return env;
    }
}
*///?}
