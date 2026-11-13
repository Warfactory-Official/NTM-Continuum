package com.hbm.capability;

import com.hbm.Constants;
import com.hbm.api.energymk2.IEnergyConnectorBlock;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.energymk2.PowerNetwork;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.uninos.PortNodes;
import com.hbm.uninos.networkproviders.PowerNetProvider;
import dev.golem.core.world.EnergyExposure;
import dev.golem.core.world.EnergySource;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * HBM's and its addons' machines on the loaders' energy capabilities: a ported machine is open at its power ports in
 * their role ({@link PortNodes#allows}), any other machine exactly where an HBM cable connects
 * ({@link PowerNetwork#handlerAtFace}). All accesses are on the owning server thread.
 */
public final class MachineEnergyExposure implements EnergySource {
    private static final MachineEnergyExposure INSTANCE = new MachineEnergyExposure();
    private static final Map<BlockEntity, WeakReference<MachineEnergyStaging>> STAGING = new WeakHashMap<>();
    private static boolean started;
    private MachineEnergyExposure() {}

    public static void init() {
        if (started) return;
        started = true;
        EnergyExposure.expose(INSTANCE, MachineEnergyExposure::carriers);
    }

    /**
     * Blocks that can be or front an energy machine (block entities, folded cells, connector footprints) of HBM and of
     * every addon built on it: a namespace counts once any of its blocks is one of HBM's machine block types.
     */
    private static Block[] carriers() {
        Set<String> namespaces = new HashSet<>();
        namespaces.add(Constants.MOD_ID);
        for (Block block : BuiltInRegistries.BLOCK) {
            if (block instanceof BlockMultiblockCore || block instanceof BlockMultiblockCell
                    || block instanceof IEnergyConnectorBlock || block instanceof ICapabilityBlock) {
                namespaces.add(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
            }
        }
        return BuiltInRegistries.BLOCK.stream()
                .filter(block -> namespaces.contains(BuiltInRegistries.BLOCK.getKey(block).getNamespace()))
                .filter(block -> block instanceof EntityBlock || block instanceof BlockMultiblockCell
                        || block instanceof IEnergyConnectorBlock)
                .toArray(Block[]::new);
    }

    private static boolean resident(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    /** Whether pos holds or fronts one of our energy machines: HBM's own net and pushes never treat it as foreign. */
    public static boolean isMachine(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof IEnergyHandlerMK2
                || MachineCore.at(level, pos, level.getBlockState(pos)) instanceof IEnergyHandlerMK2;
    }

    private static @Nullable IEnergyHandlerMK2 at(ServerLevel level, BlockPos pos, Direction side, CapRole role) {
        if (!resident(level, pos)) return null;
        var handler = machineAt(level, pos, side, role);
        if (handler == null) return null;
        // a port block (custom machine) stages into the machine it fronts
        if (handler instanceof PortNodes.Delegate delegate && delegate.portOwner() instanceof IEnergyHandlerMK2 owner
                && owner.ownsEnergyBuffer()) handler = owner;
        // a relay without a buffer (the charger) can only take; a bufferless source (creative generator) stays closed
        if (!handler.ownsEnergyBuffer()) return role == CapRole.POWER_IN && handler instanceof IEnergyReceiverMK2 ? handler : null;
        // a buffer outside its own capacity cannot be staged
        var power = handler.getPower();
        return power >= 0 && power <= handler.getMaxPower() ? handler : null;
    }

    /** A ported machine answers at its power ports in their role, any other where an HBM cable connects. */
    private static @Nullable IEnergyHandlerMK2 machineAt(ServerLevel level, BlockPos pos, Direction side, CapRole role) {
        var owner = level.getBlockEntity(pos) instanceof IEnergyHandlerMK2 own ? (BlockEntity) own
                : MachineCore.at(level, pos, level.getBlockState(pos));
        boolean receive = role == CapRole.POWER_IN;
        if (owner instanceof IEnergyHandlerMK2 machine && PortNodes.isPorted(level, machine, true)) {
            if (receive ? !(machine instanceof IEnergyReceiverMK2) : !(machine instanceof IEnergyProviderMK2)) return null;
            return PortNodes.allows(level, pos.relative(side).asLong(), ForgeDirection.getOrientation(side), machine,
                    PowerNetProvider.THE_PROVIDER, receive) ? machine : null;
        }
        if (!(PowerNetwork.handlerAtFace(level, pos, side) instanceof IEnergyHandlerMK2 machine)) return null;
        boolean plays = receive ? machine instanceof IEnergyReceiverMK2 : machine instanceof IEnergyProviderMK2;
        return plays && MachineCaps.accepts((BlockEntity) machine, side, role) ? machine : null;
    }

    @Override
    public @Nullable EnergySource forFace(ServerLevel level, BlockPos pos, @Nullable Direction side) {
        if (side == null) return null;
        var receiver = at(level, pos, side, CapRole.POWER_IN);
        var provider = at(level, pos, side, CapRole.POWER_OUT);
        var machine = receiver != null ? receiver : provider;
        if (machine == null) return null;
        if (receiver != null && provider != null && receiver != provider)
            throw new IllegalStateException("One FE face resolves different HE owners");
        var owner = (BlockEntity) machine;
        var reference = STAGING.get(owner);
        var staging = reference == null ? null : reference.get();
        if (staging == null) STAGING.put(owner, new WeakReference<>(staging = new MachineEnergyStaging(machine)));
        return new Face(owner, machine, staging, level, pos.immutable(), side);
    }

    private static final class Face implements EnergySource {
        private final BlockEntity owner;
        private final IEnergyHandlerMK2 machine;
        private final MachineEnergyStaging staging;
        private final ServerLevel level;
        private final BlockPos pos;
        private final Direction side;
        Face(BlockEntity owner, IEnergyHandlerMK2 machine, MachineEnergyStaging staging,
             ServerLevel level, BlockPos pos, Direction side) {
            this.owner = owner; this.machine = machine; this.staging = staging;
            this.level = level; this.pos = pos; this.side = side;
        }
        private boolean valid() { return !owner.isRemoved() && machine.isLoaded() && owner.getLevel() == level && resident(level, owner.getBlockPos()); }
        private boolean receives() { return valid() && at(level, pos, side, CapRole.POWER_IN) == machine; }
        private boolean provides() { return valid() && at(level, pos, side, CapRole.POWER_OUT) == machine; }
        private boolean active() { return receives() || provides(); }
        @Override public Object transactionKey() { return staging; }
        @Override public long amountFe(ServerLevel level, BlockPos pos) { return active() ? staging.amountFe() : 0; }
        @Override public long capacityFe(ServerLevel level, BlockPos pos) { return active() ? staging.capacityFe() : 0; }
        @Override public long insertableFe(ServerLevel level, BlockPos pos) { return receives() ? staging.insertableFe(Long.MAX_VALUE, machine) : 0; }
        @Override public long extractableFe(ServerLevel level, BlockPos pos) { return provides() ? staging.extractableFe(Long.MAX_VALUE, machine) : 0; }
        @Override public long insertableFe(ServerLevel level, BlockPos pos, long maximumFe) { return receives() ? staging.insertableFe(maximumFe, machine) : 0; }
        @Override public long extractableFe(ServerLevel level, BlockPos pos, long maximumFe) { return provides() ? staging.extractableFe(maximumFe, machine) : 0; }
        @Override public long insertFe(ServerLevel level, BlockPos pos, @Nullable BlockPos from, long fe) { return receives() ? staging.insert(fe, machine) : 0; }
        @Override public long extractFe(ServerLevel level, BlockPos pos, long fe) { return provides() ? staging.extract(fe, machine) : 0; }
        @Override public Object snapshot(ServerLevel level, BlockPos pos) { return staging.save(); }
        @Override public void restore(ServerLevel level, BlockPos pos, Object state) { staging.restore((long[]) state); }
        @Override public void commit(ServerLevel level, BlockPos pos) {
            if (!staging.hasPending()) return;
            if (!valid()) throw new IllegalStateException("HE owner changed during a committed transaction");
            if (staging.commit()) owner.setChanged();
        }
    }

    // The registration source is bound before the loader calls any storage operation.
    private static UnsupportedOperationException unbound() { return new UnsupportedOperationException("Bind a machine FE face first"); }
    @Override public long amountFe(ServerLevel level, BlockPos pos) { throw unbound(); }
    @Override public long capacityFe(ServerLevel level, BlockPos pos) { throw unbound(); }
    @Override public long insertableFe(ServerLevel level, BlockPos pos) { throw unbound(); }
    @Override public long extractableFe(ServerLevel level, BlockPos pos) { throw unbound(); }
    @Override public long insertFe(ServerLevel level, BlockPos pos, @Nullable BlockPos from, long fe) { throw unbound(); }
    @Override public long extractFe(ServerLevel level, BlockPos pos, long fe) { throw unbound(); }
    @Override public Object snapshot(ServerLevel level, BlockPos pos) { throw unbound(); }
    @Override public void restore(ServerLevel level, BlockPos pos, Object state) { throw unbound(); }
}
