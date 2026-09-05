package com.hbm.tileentity.network;

import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.radio.RadioLogicMenu;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class TileEntityRadioTorchLogic extends TileEntityLoadedBase
        implements IControlReceiver, net.minecraft.world.MenuProvider, RadioTorchTicking {
    public static final int MAPPING_SIZE = 16;
    public String channel = "";
    public int lastState;
    public long lastUpdate = Long.MIN_VALUE;
    public boolean polling;
    public boolean descending;
    public final String[] mapping = new String[MAPPING_SIZE];
    public final int[] conditions = new int[MAPPING_SIZE];

    public TileEntityRadioTorchLogic(BlockPos pos, BlockState state) {
        super(ModRadioBlockEntities.RADIO_TORCH_LOGIC.get(), pos, state);
        Arrays.fill(mapping, "");
    }

    @Override public void serverTick() {
        if (level == null) return;
        if (lastUpdate == Long.MIN_VALUE) lastUpdate = level.getGameTime();
        if (channel == null || channel.isEmpty()) { networkPackNT(); return; }
        RTTYSystem.RTTYChannel received = RTTYSystem.listen(level, channel);
        if (received != null && (polling || (received.timeStamp > lastUpdate - 1 && received.timeStamp != -1))) {
            String message = String.valueOf(received.signal);
            lastUpdate = level.getGameTime();
            if (received.timeStamp < lastUpdate - 1 && polling) message = "0";
            int nextState = 0;
            if (descending) {
                for (int i = MAPPING_SIZE - 1; i >= 0; i--) if (!mapping[i].isEmpty() && parseSignal(message, i)) { nextState = i; break; }
            } else {
                for (int i = 0; i < MAPPING_SIZE; i++) if (!mapping[i].isEmpty() && parseSignal(message, i)) { nextState = i; break; }
            }
            if (nextState != lastState) {
                lastState = nextState;
                BlockState state = level.getBlockState(worldPosition);
                if (state.hasProperty(RadioTorchBase.LIT)) level.setBlock(worldPosition,
                        state.setValue(RadioTorchBase.LIT, nextState > 0), 3);
                setChanged(); dataChanged();
            }
        }
        networkPackNT();
    }

    public boolean parseSignal(String signal, int index) {
        if (conditions[index] <= 5) {
            long value;
            long compare;
            try { value = Long.parseLong(signal); compare = Long.parseLong(mapping[index]); }
            catch (Exception ignored) { return false; }
            return switch (conditions[index]) {
                case 1 -> value <= compare; case 2 -> value >= compare; case 3 -> value > compare;
                case 4 -> value == compare; case 5 -> value != compare; default -> value < compare;
            };
        }
        return switch (conditions[index]) {
            case 7 -> !signal.equals(mapping[index]); case 8 -> signal.contains(mapping[index]);
            case 9 -> !signal.contains(mapping[index]); default -> signal.equals(mapping[index]);
        };
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input); polling = input.getBooleanOr("polling", false);
        descending = input.getBooleanOr("descending", false); lastState = input.getIntOr("lastState", 0);
        lastUpdate = input.getLongOr("lastUpdate", Long.MIN_VALUE); channel = input.getStringOr("channel", "");
        for (int i = 0; i < MAPPING_SIZE; i++) mapping[i] = input.getStringOr("mapping" + i, "");
        for (int i = 0; i < MAPPING_SIZE; i++) conditions[i] = input.getIntOr("conditions" + i, 0);
    }
    @Override protected void saveData(DataOut output) {
        super.saveData(output); output.putBoolean("polling", polling); output.putBoolean("descending", descending);
        output.putInt("lastState", lastState); output.putLong("lastUpdate", lastUpdate);
        if (channel != null) output.putString("channel", channel);
        for (int i = 0; i < MAPPING_SIZE; i++) if (!mapping[i].isEmpty()) output.putString("mapping" + i, mapping[i]);
        for (int i = 0; i < MAPPING_SIZE; i++) if (conditions[i] > 0) output.putInt("conditions" + i, conditions[i]);
    }
    @Override public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = Tags.getBooleanOr(data, "polling", polling);
        if (data.contains("descending")) descending = Tags.getBooleanOr(data, "descending", descending);
        if (data.contains("channel")) channel = limit(Tags.getStringOr(data, "channel", ""), 10);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (data.contains("mapping" + i)) mapping[i] = limit(Tags.getStringOr(data, "mapping" + i, ""), 15);
            if (data.contains("conditions" + i)) conditions[i] = Math.floorMod(Tags.getIntOr(data, "conditions" + i, 0), 10);
        }
        dataChanged(); setChanged();
    }
    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }
    @Override public boolean hasPermission(Player player) { return player.distanceToSqr(worldPosition.getCenter()) < 16D; }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer); buffer.writeBoolean(polling); buffer.writeUtf(channel); buffer.writeBoolean(descending);
        buffer.writeByte(lastState); for (String value : mapping) buffer.writeUtf(value); for (int value : conditions) buffer.writeInt(value);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer); polling = buffer.readBoolean(); channel = buffer.readUtf(10); descending = buffer.readBoolean();
        lastState = buffer.readByte(); for (int i = 0; i < MAPPING_SIZE; i++) mapping[i] = buffer.readUtf(15);
        for (int i = 0; i < MAPPING_SIZE; i++) conditions[i] = buffer.readInt();
    }
    @Override public Component getDisplayName() { return Component.translatable("container.rttyLogic"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadioLogicMenu(id, inventory, this);
    }
}
