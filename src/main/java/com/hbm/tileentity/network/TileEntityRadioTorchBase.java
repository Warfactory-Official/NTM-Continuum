package com.hbm.tileentity.network;

import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.RadioTorchMenu;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Shared persisted and synchronized configuration for sender and receiver torches. */
public abstract class TileEntityRadioTorchBase extends TileEntityLoadedBase
        implements IControlReceiver, MenuProvider, RadioTorchTicking {
    public static final int MAPPING_SIZE = 16;

    public String channel = "";
    public int lastState;
    public long lastUpdate = Long.MIN_VALUE;
    public boolean polling;
    public boolean customMap;
    public final String[] mapping = new String[MAPPING_SIZE];

    protected TileEntityRadioTorchBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public abstract void serverTick();

    protected final Direction getTorchFacing() {
        return getBlockState().hasProperty(RadioTorchBase.FACING)
                ? getBlockState().getValue(RadioTorchBase.FACING) : Direction.UP;
    }

    protected final void syncRadio() {
        networkPackNT();
    }

    protected final void setLit(boolean lit) {
        if (level == null) return;
        BlockState state = level.getBlockState(worldPosition);
        if (state.hasProperty(RadioTorchBase.LIT) && state.getValue(RadioTorchBase.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(RadioTorchBase.LIT, lit), 3);
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        polling = input.getBooleanOr("isPolling", false);
        customMap = input.getBooleanOr("hasMapping", false);
        lastState = input.getIntOr("lastPower", 0);
        lastUpdate = input.getLongOr("lastTime", Long.MIN_VALUE);
        channel = input.getStringOr("channel", "");
        for (int i = 0; i < MAPPING_SIZE; i++) mapping[i] = input.getStringOr("mapping" + i, "");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("isPolling", polling);
        output.putBoolean("hasMapping", customMap);
        output.putInt("lastPower", lastState);
        output.putLong("lastTime", lastUpdate);
        if (channel != null) output.putString("channel", channel);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (mapping[i] != null) output.putString("mapping" + i, mapping[i]);
        }
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("isPolling")) polling = Tags.getBooleanOr(data, "isPolling", polling);
        if (data.contains("hasMapping")) customMap = Tags.getBooleanOr(data, "hasMapping", customMap);
        if (data.contains("channel")) channel = limited(Tags.getStringOr(data, "channel", ""), 10);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            String key = "mapping" + i;
            if (data.contains(key)) mapping[i] = limited(Tags.getStringOr(data, key, ""), 32);
        }
        dataChanged();
        setChanged();
    }

    private static String limited(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @Override
    public boolean hasPermission(Player player) {
        double dx = worldPosition.getX() - player.getX();
        double dy = worldPosition.getY() - player.getY();
        double dz = worldPosition.getZ() - player.getZ();
        return dx * dx + dy * dy + dz * dz < 16D * 16D;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeBoolean(polling);
        buffer.writeBoolean(customMap);
        buffer.writeInt(lastState);
        buffer.writeUtf(channel == null ? "" : channel);
        for (String value : mapping) {
            buffer.writeBoolean(value != null);
            if (value != null) buffer.writeUtf(value);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        polling = buffer.readBoolean();
        customMap = buffer.readBoolean();
        lastState = buffer.readInt();
        channel = buffer.readUtf(10);
        for (int i = 0; i < MAPPING_SIZE; i++) mapping[i] = buffer.readBoolean() ? buffer.readUtf(32) : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(titleKey());
    }

    protected abstract String titleKey();

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new RadioTorchMenu(containerId, inventory, this);
    }
}
