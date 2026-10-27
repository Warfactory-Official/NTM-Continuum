package com.hbm.api.energymk2;

import dev.golem.core.world.EnergySource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jspecify.annotations.Nullable;

/**
 * A conductor's FE face, backed by the {@link ForeignEnergyBridge} of whatever network that conductor
 * belongs to. Every number here is a BUDGET the last round published, never a stored charge: a cable
 * holds nothing, so what a peer may take is what this net's providers had left over, and what a peer
 * may give is what its machines asked for and did not get.
 *
 * <p>A position with no network answers zero to everything, which is the right answer for a cable
 * whose chunk has come back before its graph has.
 */
final class PowerEnergySource implements EnergySource {

    static final PowerEnergySource INSTANCE = new PowerEnergySource();

    private PowerEnergySource() {}

    @Override
    public @Nullable EnergySource forOperation(ServerLevel level, BlockPos pos) {
        return PowerNetwork.bridgeAt(level, pos);
    }

    @Override
    public long amountFe(ServerLevel level, BlockPos pos) {
        ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, pos);
        return bridge == null ? 0L : bridge.amountFe();
    }

    @Override
    public long capacityFe(ServerLevel level, BlockPos pos) {
        ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, pos);
        return bridge == null ? 0L : bridge.capacityFe();
    }

    @Override
    public long insertableFe(ServerLevel level, BlockPos pos) {
        ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, pos);
        return bridge == null ? 0L : bridge.insertableFe();
    }

    @Override
    public long extractableFe(ServerLevel level, BlockPos pos) {
        ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, pos);
        return bridge == null ? 0L : bridge.extractableFe();
    }

    @Override
    public long insertFe(ServerLevel level, BlockPos pos, @Nullable BlockPos from, long fe) {
        ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, pos);
        return bridge == null ? 0L : bridge.insertFe(fe, (from == null ? pos : from).asLong(), level.getGameTime());
    }

    @Override
    public long extractFe(ServerLevel level, BlockPos pos, long fe) {
        ForeignEnergyBridge bridge = PowerNetwork.bridgeAt(level, pos);
        return bridge == null ? 0L : bridge.extractFe(fe);
    }

    @Override
    public Object snapshot(ServerLevel level, BlockPos pos) {
        throw new UnsupportedOperationException("Bind a cable transaction owner before snapshotting");
    }

    @Override
    public void restore(ServerLevel level, BlockPos pos, Object state) {
        throw new UnsupportedOperationException("Restore the captured cable transaction owner");
    }
}
