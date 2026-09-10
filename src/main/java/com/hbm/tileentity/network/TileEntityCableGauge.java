package com.hbm.tileentity.network;

import com.hbm.api.energymk2.IEnergyConductorMK2;
import com.hbm.api.energymk2.Nodespace;
import com.hbm.api.energymk2.PowerNetMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.tile.ILoadedTile;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cable that reports what passes through it. Ported from 1.12.2's nested
 * {@code BlockCableGauge.TileEntityCableGauge}.
 *
 * <p>It conducts exactly as an ordinary cable does — same UNINOS node, same six connections — and reads
 * {@link PowerNetMK2#energyTracker}, the running total the network itself keeps, so the reading is the
 * whole network's throughput rather than this block's. Two numbers come out of that: the current tick's
 * total, and the previous whole second's, accumulated and rolled over on the 20-tick boundary.
 *
 * <p><b>Deviation from the fork.</b> There the gauge extends the cable tile; here it extends
 * {@link TileEntityLoadedBase} and repeats the node lifecycle, because the readout has to reach the
 * client for the look overlay and only that base carries the packet sync. The conduction contract is
 * identical either way — {@link IEnergyConductorMK2} supplies the node, and nothing in the power engine
 * tests for the cable class.
 */
public class TileEntityCableGauge extends TileEntityLoadedBase
        implements IEnergyConductorMK2, ILoadedTile, IRORValueProvider {

    protected Nodespace.PowerNode node;

    public long deltaTick = 10;
    public long deltaLastSecond;
    private long deltaSecond;

    public TileEntityCableGauge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE_GAUGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCableGauge gauge) {
        if (gauge.node == null || gauge.node.expired) {
            gauge.node = UniNodespace.getNode(level, pos, Nodespace.THE_POWER_PROVIDER);
            if (gauge.node == null || gauge.node.expired) {
                gauge.node = gauge.createNode();
                UniNodespace.createNode(level, gauge.node);
            }
        }

        if (gauge.node != null && gauge.node.net != null) {
            gauge.deltaTick = gauge.node.net.energyTracker;
            if (level.getGameTime() % 20 == 0) {
                gauge.deltaLastSecond = gauge.deltaSecond;
                gauge.deltaSecond = 0;
            }
            gauge.deltaSecond += gauge.deltaTick;
        }

        gauge.networkPackNT();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide()) {
            UniNodespace.destroyNode(level, getBlockPos(), Nodespace.THE_POWER_PROVIDER);
        }
    }

    @Override
    public boolean isLoaded() {
        return !isRemoved();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(deltaTick);
        buf.writeLong(deltaLastSecond);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        deltaTick = Math.max(buf.readLong(), 0L);
        deltaLastSecond = Math.max(buf.readLong(), 0L);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "deltatick", PREFIX_VALUE + "deltasecond"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "deltatick").equals(name)) return "" + deltaTick;
        if ((PREFIX_VALUE + "deltasecond").equals(name)) return "" + deltaLastSecond;
        return null;
    }
}
