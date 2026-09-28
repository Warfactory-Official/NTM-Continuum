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

/** 1.7 {@code TileEntityRBMKIndicator}: six lamps that light while an RoR value sits inside a window. */
public class BlockEntityRBMKIndicator extends TileEntityLoadedBase implements IControlReceiver, IRBMKTicking {

    public static final int INDICATORS = 6;
    public final IndicatorUnit[] indicators = new IndicatorUnit[INDICATORS];

    public BlockEntityRBMKIndicator(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_INDICATOR.get(), pos, state);
        for (int i = 0; i < INDICATORS; i++) indicators[i] = new IndicatorUnit(i);
    }

    @Override public void tickServer() { for (IndicatorUnit unit : indicators) unit.update(); networkPackNT(); }

    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); for (IndicatorUnit u : indicators) u.serialize(buf); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); for (IndicatorUnit u : indicators) u.deserialize(buf); }
    @Override protected void saveData(DataOut output) { super.saveData(output); for (int i = 0; i < INDICATORS; i++) indicators[i].save(output, i); }
    @Override protected void loadData(DataIn input) { super.loadData(input); for (int i = 0; i < INDICATORS; i++) indicators[i].load(input, i); }

    @Override public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) < 15 * 15;
    }

    @Override public void receiveControl(CompoundTag data) {
        int active = Tags.getByteOr(data, "active", (byte) 0), polling = Tags.getByteOr(data, "polling", (byte) 0);
        for (int i = 0; i < INDICATORS; i++) {
            indicators[i].active = (active & 1 << i) != 0;
            indicators[i].polling = (polling & 1 << i) != 0;
        }
        for (int i = 0; i < INDICATORS; i++) {
            IndicatorUnit unit = indicators[i];
            unit.color = Mth.clamp(Tags.getIntOr(data, "color" + i, 0), 0, 0xffffff);
            unit.label = Tags.getStringOr(data, "label" + i, "");
            unit.rtty = Tags.getStringOr(data, "rtty" + i, "");
            // An unparsable bound field is simply absent from the packet, which opens that side of the window.
            unit.min = Tags.contains(data, "min" + i) ? Tags.getIntOr(data, "min" + i, 0) : Integer.MIN_VALUE;
            unit.max = Tags.contains(data, "max" + i) ? Tags.getIntOr(data, "max" + i, 0) : Integer.MAX_VALUE;
        }
        setChanged();
    }

    public final class IndicatorUnit {
        public boolean polling;
        public int color;
        public String label = "", rtty = "";
        public long min = 0, max = 100;
        public boolean light, active;

        IndicatorUnit(int index) {
            color = index % 2 == 0 ? 0xff0000 : 0xffff00;
            label = "Indicator " + (index + 1);
        }

        void update() {
            if (!active || rtty == null || rtty.isEmpty()) return;
            RTTYChannel chan = RTTYSystem.listen(level, rtty);
            int sigVal = 0;
            if (chan != null && chan.timeStamp < level.getGameTime() - 1) chan = null;
            if (chan != null && chan.signal != null) {
                try { sigVal = Integer.parseInt(chan.signal.toString()); } catch (Exception ignored) { }
                decideLight(sigVal);
            } else if (polling) {
                decideLight(0);
            }
        }

        /** If max is lower than min, the window inverts: the lamp lights outside of the bounds. */
        public void decideLight(long value) {
            if (min <= max) light = value >= min && value <= max;
            else light = value < max || value > min;
        }

        void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(active); buf.writeBoolean(polling); buf.writeBoolean(light); buf.writeInt(color);
            buf.writeUtf(label); buf.writeUtf(rtty); buf.writeLong(min); buf.writeLong(max);
        }
        void deserialize(RegistryFriendlyByteBuf buf) {
            active = buf.readBoolean(); polling = buf.readBoolean(); light = buf.readBoolean(); color = buf.readInt();
            label = buf.readUtf(); rtty = buf.readUtf(); min = buf.readLong(); max = buf.readLong();
        }
        void save(DataOut out, int i) {
            out.putBoolean("active" + i, active); out.putBoolean("polling" + i, polling); out.putInt("color" + i, color);
            out.putString("label" + i, label); out.putString("rtty" + i, rtty); out.putLong("min" + i, min);
            out.putLong("max" + i, max); out.putBoolean("light" + i, light);
        }
        void load(DataIn in, int i) {
            active = in.getBooleanOr("active" + i, false); polling = in.getBooleanOr("polling" + i, false); color = in.getIntOr("color" + i, 0);
            label = in.getStringOr("label" + i, ""); rtty = in.getStringOr("rtty" + i, ""); min = in.getLongOr("min" + i, 0L);
            max = in.getLongOr("max" + i, 0L); light = in.getBooleanOr("light" + i, false);
        }
    }
}
