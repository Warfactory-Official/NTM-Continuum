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
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;

/** Four independent RTTY gauges. A non-polling gauge retains its last received value when a signal goes stale. */
public class BlockEntityRBMKGauge extends TileEntityLoadedBase implements IControlReceiver, IRBMKTicking {
    public static final int GAUGES = 4;
    public final GaugeUnit[] gauges = {new GaugeUnit(0), new GaugeUnit(1), new GaugeUnit(2), new GaugeUnit(3)};
    public BlockEntityRBMKGauge(BlockPos pos, BlockState state) { super(ModRBMKBlockEntities.RBMK_GAUGE.get(), pos, state); }
    @Override public void tickServer() { for (GaugeUnit gauge : gauges) gauge.update(level); networkPackNT(); }
    @Override public void tickClient() { for (GaugeUnit gauge : gauges) gauge.updateClient(); }
    public void setValue(int channel, long value) { if (channel >= 0 && channel < GAUGES) { gauges[channel].value = value; dataChanged(); } }
    @Override public void receiveControl(CompoundTag data) {
        int active = Tags.getByteOr(data, "active", (byte) 0), polling = Tags.getByteOr(data, "polling", (byte) 0);
        for (int i = 0; i < GAUGES; i++) { gauges[i].active = (active & 1 << i) != 0; gauges[i].polling = (polling & 1 << i) != 0; }
        for (int i = 0; i < GAUGES; i++) { GaugeUnit gauge = gauges[i]; gauge.color = Mth.clamp(Tags.getIntOr(data, "color" + i, 0), 0, 0xffffff); gauge.label = Tags.getStringOr(data, "label" + i, ""); gauge.rtty = Tags.getStringOr(data, "rtty" + i, ""); gauge.min = Tags.getIntOr(data, "min" + i, 0); gauge.max = Tags.getIntOr(data, "max" + i, 0); }
        setChanged();
    }
    @Override public boolean hasPermission(Player player) { return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) < 15 * 15; }
    @Override protected void saveData(DataOut output) { super.saveData(output); for (int i = 0; i < GAUGES; i++) gauges[i].save(output, i); }
    @Override protected void loadData(DataIn input) { super.loadData(input); for (int i = 0; i < GAUGES; i++) gauges[i].load(input, i); }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) { super.serialize(buffer); for (GaugeUnit gauge : gauges) gauge.serialize(buffer); }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { super.deserialize(buffer); for (GaugeUnit gauge : gauges) gauge.deserialize(buffer); }

    public static final class GaugeUnit {
        public boolean active, polling; public int color; public String label = "", rtty = ""; public long min, max = 100, value; public double renderValue, lastRenderValue;
        GaugeUnit(int index) { color = new int[]{0x800000, 0x804000, 0x808000, 0x000080}[index]; label = "Gauge " + (index + 1); }
        public void updateClient() { lastRenderValue = renderValue; renderValue += (value - renderValue) * .1D; }
        public void update(net.minecraft.world.level.Level level) {
            if (!active || rtty == null || rtty.isEmpty()) return;
            RTTYChannel channel = RTTYSystem.listen(level, rtty);
            if (channel != null && channel.timeStamp < level.getGameTime() - 1) channel = null;
            if (channel != null && channel.signal != null) {
                try { value = Integer.parseInt(channel.signal.toString()); } catch (Exception ignored) { }
            } else if (polling) value = 0;
        }
        void serialize(RegistryFriendlyByteBuf buffer) { buffer.writeBoolean(active); buffer.writeBoolean(polling); buffer.writeInt(color); buffer.writeUtf(label); buffer.writeUtf(rtty); buffer.writeLong(min); buffer.writeLong(max); buffer.writeLong(value); }
        void deserialize(RegistryFriendlyByteBuf buffer) { active = buffer.readBoolean(); polling = buffer.readBoolean(); color = buffer.readInt(); label = buffer.readUtf(); rtty = buffer.readUtf(); min = buffer.readLong(); max = buffer.readLong(); value = buffer.readLong(); }
        void save(DataOut output, int index) { output.putBoolean("active" + index, active); output.putBoolean("polling" + index, polling); output.putInt("color" + index, color); output.putString("label" + index, label); output.putString("rtty" + index, rtty); output.putLong("min" + index, min); output.putLong("max" + index, max); output.putLong("value" + index, value); }
        void load(DataIn input, int index) { active = input.getBooleanOr("active" + index, false); polling = input.getBooleanOr("polling" + index, false); color = input.getIntOr("color" + index, 0); label = input.getStringOr("label" + index, ""); rtty = input.getStringOr("rtty" + index, ""); min = input.getLongOr("min" + index, 0); max = input.getLongOr("max" + index, 0); value = input.getLongOr("value" + index, 0); }
    }
}
