package com.hbm.tileentity.network;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.radio.ModRadioBlockEntities;
import com.hbm.radio.RadioReaderMenu;
import com.hbm.radio.RadioTargets;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Locale;

public final class TileEntityRadioTorchReader extends TileEntityLoadedBase
        implements IControlReceiver, net.minecraft.world.MenuProvider, RadioTorchTicking {
    public static final int MAPPING_SIZE = 8;
    public final String[] channels = new String[MAPPING_SIZE];
    public final String[] names = new String[MAPPING_SIZE];
    public final String[] prev = new String[MAPPING_SIZE];
    public boolean polling;

    public TileEntityRadioTorchReader(BlockPos pos, BlockState state) {
        super(ModRadioBlockEntities.RADIO_TORCH_READER.get(), pos, state);
        Arrays.fill(channels, "");
        Arrays.fill(names, "");
        Arrays.fill(prev, "");
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        Direction direction = getTorchFacing().getOpposite();
        IRORValueProvider provider = RadioTargets.valueProvider(level, worldPosition.relative(direction));
        if (provider != null) {
            for (int i = 0; i < MAPPING_SIZE; i++) {
                if (channels[i] == null || channels[i].isEmpty() || names[i] == null || names[i].isEmpty()) continue;
                String value = provider.provideRORValue(IRORValueProvider.PREFIX_VALUE
                        + names[i].toLowerCase(Locale.US));
                if (value != null && (polling || !value.equals(prev[i]))) {
                    RTTYSystem.broadcast(level, channels[i], value);
                    prev[i] = value;
                }
            }
        }
        networkPackNT();
    }

    public Direction getTorchFacing() {
        return getBlockState().hasProperty(RadioTorchBase.FACING)
                ? getBlockState().getValue(RadioTorchBase.FACING) : Direction.UP;
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        polling = input.getBooleanOr("polling", false);
        for (int i = 0; i < MAPPING_SIZE; i++) channels[i] = input.getStringOr("channels" + i, "");
        for (int i = 0; i < MAPPING_SIZE; i++) names[i] = input.getStringOr("names" + i, "");
        for (int i = 0; i < MAPPING_SIZE; i++) prev[i] = input.getStringOr("prev" + i, "");
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("polling", polling);
        for (int i = 0; i < MAPPING_SIZE; i++) output.putString("channels" + i, channels[i]);
        for (int i = 0; i < MAPPING_SIZE; i++) output.putString("names" + i, names[i]);
        for (int i = 0; i < MAPPING_SIZE; i++) output.putString("prev" + i, prev[i]);
    }

    @Override public void receiveControl(CompoundTag data) {
        if (data.contains("polling")) polling = Tags.getBooleanOr(data, "polling", polling);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            if (data.contains("channels" + i)) channels[i] = limit(Tags.getStringOr(data, "channels" + i, ""), 15);
            if (data.contains("names" + i)) names[i] = limit(Tags.getStringOr(data, "names" + i, ""), 25);
        }
        dataChanged();
        setChanged();
    }

    private static String limit(String value, int max) { return value.length() <= max ? value : value.substring(0, max); }

    @Override public boolean hasPermission(Player player) { return player.distanceToSqr(worldPosition.getCenter()) < 256D; }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer); buffer.writeBoolean(polling);
        for (String value : channels) buffer.writeUtf(value);
        for (String value : names) buffer.writeUtf(value);
    }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer); polling = buffer.readBoolean();
        for (int i = 0; i < MAPPING_SIZE; i++) channels[i] = buffer.readUtf(15);
        for (int i = 0; i < MAPPING_SIZE; i++) names[i] = buffer.readUtf(25);
    }
    @Override public Component getDisplayName() { return Component.translatable("container.rttyReader"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadioReaderMenu(id, inventory, this);
    }
}
