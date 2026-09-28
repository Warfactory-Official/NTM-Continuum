package com.hbm.tileentity.machine.rbmk;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.ModRBMKBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.tileentity.network.RTTYSystem.RTTYChannel;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code TileEntityRBMKGraph}: two 30-sample line graphs (two samples a second for fifteen seconds),
 * each optionally clamped to a fixed lower and upper bound.
 */
public class BlockEntityRBMKGraph extends TileEntityLoadedBase implements IControlReceiver, IRBMKTicking {

    public static final int GRAPHS = 2;
    public final GraphUnit[] graphs = new GraphUnit[GRAPHS];

    public BlockEntityRBMKGraph(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_GRAPH.get(), pos, state);
        for (int i = 0; i < GRAPHS; i++) graphs[i] = new GraphUnit(i);
    }

    @Override public void tickServer() {
        if (level.getGameTime() % 10 == 0) for (GraphUnit graph : graphs) graph.update();
        networkPackNT();
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); for (GraphUnit g : graphs) g.serialize(buf); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); for (GraphUnit g : graphs) g.deserialize(buf); }
    @Override protected void saveData(DataOut output) { super.saveData(output); for (int i = 0; i < GRAPHS; i++) graphs[i].save(output, i); }
    @Override protected void loadData(DataIn input) { super.loadData(input); for (int i = 0; i < GRAPHS; i++) graphs[i].load(input, i); }

    @Override public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) < 15 * 15;
    }

    @Override public void receiveControl(CompoundTag data) {
        int active = Tags.getByteOr(data, "active", (byte) 0), polling = Tags.getByteOr(data, "polling", (byte) 0);
        for (int i = 0; i < GRAPHS; i++) {
            graphs[i].active = (active & 1 << i) != 0;
            graphs[i].polling = (polling & 1 << i) != 0;
        }
        for (int i = 0; i < GRAPHS; i++) {
            GraphUnit graph = graphs[i];
            graph.label = Tags.getStringOr(data, "label" + i, "");
            graph.rtty = Tags.getStringOr(data, "rtty" + i, "");
            graph.minBound = Tags.contains(data, "min" + i);
            if (graph.minBound) graph.min = Tags.getLongOr(data, "min" + i, 0L);
            graph.maxBound = Tags.contains(data, "max" + i);
            if (graph.maxBound) graph.max = Tags.getLongOr(data, "max" + i, 0L);
            if (graph.max < graph.min) {
                long temp = graph.max;
                graph.max = graph.min;
                graph.min = temp;
            }
        }
        markChanged();
    }

    public final class GraphUnit {
        public boolean polling;
        public String label = "", rtty = "";
        /** 2 values/s for 15 seconds. */
        public final long[] values = new long[30];
        public boolean active;
        public long min, max;
        public boolean minBound = false, maxBound = false;

        GraphUnit(int index) { label = "Graph " + (index + 1); }

        void update() {
            if (!active || rtty == null || rtty.isEmpty()) return;
            RTTYChannel chan = RTTYSystem.listen(level, rtty);
            long sigVal = 0;
            if (chan != null && chan.timeStamp < level.getGameTime() - 1) chan = null;
            if (chan != null && chan.signal != null) {
                try { sigVal = Long.parseLong(chan.signal.toString()); } catch (Exception ignored) { }
                pushValue(sigVal);
            } else if (polling) {
                pushValue(0);
            }
        }

        public void pushValue(long value) {
            System.arraycopy(values, 1, values, 0, values.length - 1);
            values[values.length - 1] = value;
        }

        void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(active); buf.writeBoolean(polling); buf.writeUtf(label); buf.writeUtf(rtty);
            buf.writeBoolean(minBound); if (minBound) buf.writeLong(min);
            buf.writeBoolean(maxBound); if (maxBound) buf.writeLong(max);
            if (active) for (long value : values) buf.writeLong(value);
        }
        void deserialize(RegistryFriendlyByteBuf buf) {
            active = buf.readBoolean(); polling = buf.readBoolean(); label = buf.readUtf(); rtty = buf.readUtf();
            minBound = buf.readBoolean(); if (minBound) min = buf.readLong();
            maxBound = buf.readBoolean(); if (maxBound) max = buf.readLong();
            if (active) for (int i = 0; i < values.length; i++) values[i] = buf.readLong();
        }
        void save(DataOut out, int index) {
            out.putBoolean("active" + index, active); out.putBoolean("polling" + index, polling);
            out.putString("label" + index, label); out.putString("rtty" + index, rtty);
            out.putBoolean("minBound" + index, minBound); out.putLong("min" + index, min);
            out.putBoolean("maxBound" + index, maxBound); out.putLong("max" + index, max);
            for (int i = 0; i < values.length; i++) out.putLong("value" + index + "_" + i, values[i]);
        }
        void load(DataIn in, int index) {
            active = in.getBooleanOr("active" + index, false); polling = in.getBooleanOr("polling" + index, false);
            label = in.getStringOr("label" + index, ""); rtty = in.getStringOr("rtty" + index, "");
            minBound = in.getBooleanOr("minBound" + index, false); min = in.getLongOr("min" + index, 0L);
            maxBound = in.getBooleanOr("maxBound" + index, false); max = in.getLongOr("max" + index, 0L);
            for (int i = 0; i < values.length; i++) values[i] = in.getLongOr("value" + index + "_" + i, 0L);
        }
    }
}
