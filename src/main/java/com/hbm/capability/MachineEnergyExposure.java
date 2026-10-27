package com.hbm.capability;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import dev.golem.core.world.EnergyExposure;
import dev.golem.core.world.EnergySource;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Authored machine FE faces over real finite HE buffers. All accesses are on the owning server thread. */
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

    private static Block[] carriers() {
        return BuiltInRegistries.BLOCK.stream().filter(block -> block instanceof BlockMultiblockCell
                || block instanceof ICapabilityBlock declaring && (declaring.caps().declaredBits() & MachineCaps.FE) != 0)
                .toArray(Block[]::new);
    }

    private static boolean resident(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static @Nullable IEnergyHandlerMK2 at(ServerLevel level, BlockPos pos, Direction side, CapRole role) {
        if (!resident(level, pos)) return null;
        var handler = NativeContacts.powerAt(level, pos, side, role);
        if (handler == null || !handler.ownsEnergyBuffer()) return null;
        var owner = (BlockEntity) handler;
        if (!(owner.getBlockState().getBlock() instanceof ICapabilityBlock declaring)) return null;
        var bits = declaring.caps().declaredBits();
        if ((bits & MachineCaps.FE) == 0 || (bits & role.bit) == 0) return null;
        if (owner.getBlockState().getBlock() instanceof BlockMultiblockCore block) {
            var core = owner.getBlockPos();
            if (!block.isOpenAt(owner.getBlockState().getValue(BlockMultiblockCore.FACING),
                    pos.getX() - core.getX(), pos.getY() - core.getY(), pos.getZ() - core.getZ(), side,
                    BlockMultiblockCore.PASSIVE_NONE, role)) return null;
        } else if (!pos.equals(owner.getBlockPos())) return null;
        return handler;
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
