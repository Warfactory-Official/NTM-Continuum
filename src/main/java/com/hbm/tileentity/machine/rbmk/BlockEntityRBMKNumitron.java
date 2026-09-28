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

/** 1.7 {@code TileEntityRBMKNumitron}: two seven-digit numitron readouts fed from RoR channels. */
public class BlockEntityRBMKNumitron extends TileEntityLoadedBase implements IControlReceiver, IRBMKTicking {

    public static final int DISPLAYS = 2;
    public final DisplayUnit[] displays = new DisplayUnit[DISPLAYS];

    public BlockEntityRBMKNumitron(BlockPos pos, BlockState state) {
        super(ModRBMKBlockEntities.RBMK_NUMITRON.get(), pos, state);
        for (int i = 0; i < DISPLAYS; i++) displays[i] = new DisplayUnit(i);
    }

    @Override public void tickServer() { for (DisplayUnit display : displays) display.update(); networkPackNT(); }

    @Override public void serialize(RegistryFriendlyByteBuf buf) { super.serialize(buf); for (DisplayUnit d : displays) d.serialize(buf); }
    @Override public void deserialize(RegistryFriendlyByteBuf buf) { super.deserialize(buf); for (DisplayUnit d : displays) d.deserialize(buf); }
    @Override protected void saveData(DataOut output) { super.saveData(output); for (int i = 0; i < DISPLAYS; i++) displays[i].save(output, i); }
    @Override protected void loadData(DataIn input) { super.loadData(input); for (int i = 0; i < DISPLAYS; i++) displays[i].load(input, i); }

    @Override public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + .5D, worldPosition.getY() + .5D, worldPosition.getZ() + .5D) < 15 * 15;
    }

    @Override public void receiveControl(CompoundTag data) {
        int active = Tags.getByteOr(data, "active", (byte) 0), polling = Tags.getByteOr(data, "polling", (byte) 0);
        int shorten = Tags.getByteOr(data, "shorten_number", (byte) 0), zeroes = Tags.getByteOr(data, "leading_zeroes", (byte) 0);
        for (int i = 0; i < DISPLAYS; i++) {
            displays[i].active = (active & 1 << i) != 0;
            displays[i].polling = (polling & 1 << i) != 0;
            displays[i].shorten_number = (shorten & 1 << i) != 0;
            displays[i].leading_zeroes = (zeroes & 1 << i) != 0;
        }
        for (int i = 0; i < DISPLAYS; i++) {
            displays[i].label = Tags.getStringOr(data, "label" + i, "");
            displays[i].rtty = Tags.getStringOr(data, "rtty" + i, "");
        }
        setChanged();
    }

    public final class DisplayUnit {
        /** If the value should be pulled from the RTTY system every tick, otherwise only on state change. */
        public boolean polling;
        public String label = "", rtty = "";
        public long value;
        public boolean active;
        /** If leading zeros should be added (the original field name says the opposite of what it does). */
        public boolean leading_zeroes;
        /** Which digits are activated (bit mask, msb ignored). */
        public long active_digits;
        public boolean shorten_number;

        DisplayUnit(int index) {
            label = "Display " + (index + 1);
            active_digits = 0b01111111;
            shorten_number = true;
            leading_zeroes = true;
        }

        void update() {
            if (!active || rtty == null || rtty.isEmpty()) return;
            RTTYChannel chan = RTTYSystem.listen(level, rtty);
            long sigVal = 0;
            if (chan != null && chan.timeStamp < level.getGameTime() - 1) chan = null;
            // always accept new signals
            if (chan != null && chan.signal != null) {
                try { sigVal = Long.parseLong(chan.signal.toString()); } catch (Exception ignored) { }
                value = sigVal;
            } else if (polling) {
                // if there's no new signal and we're polling, set to 0
                value = 0;
            }
        }

        void serialize(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(shorten_number); buf.writeLong(active_digits); buf.writeBoolean(leading_zeroes);
            buf.writeBoolean(active); buf.writeBoolean(polling); buf.writeUtf(label); buf.writeUtf(rtty); buf.writeLong(value);
        }
        void deserialize(RegistryFriendlyByteBuf buf) {
            shorten_number = buf.readBoolean(); active_digits = buf.readLong(); leading_zeroes = buf.readBoolean();
            active = buf.readBoolean(); polling = buf.readBoolean(); label = buf.readUtf(); rtty = buf.readUtf(); value = buf.readLong();
        }
        void save(DataOut out, int i) {
            out.putBoolean("shorten_number" + i, shorten_number); out.putLong("active_digits" + i, active_digits);
            out.putBoolean("leading_zeroes" + i, leading_zeroes); out.putBoolean("active" + i, active); out.putBoolean("polling" + i, polling);
            out.putString("label" + i, label); out.putString("rtty" + i, rtty); out.putLong("value" + i, value);
        }
        void load(DataIn in, int i) {
            shorten_number = in.getBooleanOr("shorten_number" + i, false); active_digits = in.getLongOr("active_digits" + i, 0L);
            leading_zeroes = in.getBooleanOr("leading_zeroes" + i, false); active = in.getBooleanOr("active" + i, false);
            polling = in.getBooleanOr("polling" + i, false); label = in.getStringOr("label" + i, ""); rtty = in.getStringOr("rtty" + i, "");
            value = in.getLongOr("value" + i, 0L);
        }
    }
}
