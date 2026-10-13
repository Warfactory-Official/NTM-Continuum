package com.hbm.tileentity.network;

import com.hbm.blocks.network.RadioTorchBase;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.module.ModulePatternMatcher;
import com.hbm.inventory.container.RadioCounterMenu;
import com.hbm.tileentity.network.RadioTargets;
import com.hbm.tileentity.TileEntityInventoryBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.ModBlockEntities;

/** Server-side three-slot ROR inventory counter. */
public class TileEntityRadioTorchCounter extends TileEntityInventoryBase implements IControlReceiver, RadioTorchTicking,
        MenuProvider {
    public static final int MAPPING_SIZE = 3;

    public final String[] channel = new String[MAPPING_SIZE];
    public final int[] lastCount = new int[MAPPING_SIZE];
    public final ModulePatternMatcher matcher = new ModulePatternMatcher(MAPPING_SIZE);
    public boolean polling;

    public TileEntityRadioTorchCounter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RADIO_TORCH_COUNTER.get(), pos, state, MAPPING_SIZE);
        for (int i = 0; i < MAPPING_SIZE; i++) channel[i] = "";
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        Direction direction = getTorchFacing().getOpposite();
        Container container = RadioTargets.container(
                level, worldPosition.relative(direction));
        if (container != null) {
            for (int i = 0; i < MAPPING_SIZE; i++) {
                if (channel[i] == null || channel[i].isEmpty()) continue;
                ItemStack filter = inventory.getStackInSlot(i);
                if (filter.isEmpty()) continue;

                int count = 0;
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack input = container.getItem(slot);
                    if (!input.isEmpty() && matcher.isValidForFilter(filter, i, input)) count += input.getCount();
                }

                if (polling || lastCount[i] != count) RTTYSystem.broadcast(level, channel[i], count);
                lastCount[i] = count;
            }
        }
        networkPackNT();
    }

    public void nextMode(int index) {
        if (level != null) matcher.nextMode(level, inventory.getStackInSlot(index), index);
        dataChanged();
    }

    public int[] getFilterSlots() {
        return new int[]{0, inventory.getSlots()};
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        polling = input.getBooleanOr("polling", false);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            channel[i] = input.getStringOr("channel" + i, "");
            lastCount[i] = input.getIntOr("lastCount" + i, 0);
        }
        matcher.load(input);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("polling", polling);
        for (int i = 0; i < MAPPING_SIZE; i++) {
            output.putString("channel" + i, channel[i] == null ? "" : channel[i]);
            output.putInt("lastCount" + i, lastCount[i]);
        }
        matcher.save(output);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        // The old counter's screen sends a marker and the receiver toggles it, rather than using its value.
        if (data.contains("polling")) polling = !polling;
        for (int i = 0; i < MAPPING_SIZE; i++) {
            String key = "channel" + i;
            if (data.contains(key)) channel[i] = limited(Tags.getStringOr(data, key, ""), 10);
        }
        setChanged();
        dataChanged();
    }

    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getCenter()) <= 128D;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buffer) {
        super.serialize(buffer);
        buffer.writeBoolean(polling);
        for (int count : lastCount) buffer.writeInt(count);
        matcher.serialize(buffer);
        for (String value : channel) buffer.writeUtf(value == null ? "" : value, 10);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buffer) {
        super.deserialize(buffer);
        polling = buffer.readBoolean();
        for (int i = 0; i < MAPPING_SIZE; i++) lastCount[i] = buffer.readInt();
        matcher.deserialize(buffer);
        for (int i = 0; i < MAPPING_SIZE; i++) channel[i] = buffer.readUtf(10);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.rttyCounter");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadioCounterMenu(id, inventory, this);
    }

    private static String limited(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }

    private Direction getTorchFacing() {
        return getBlockState().hasProperty(RadioTorchBase.FACING)
                ? getBlockState().getValue(RadioTorchBase.FACING) : Direction.UP;
    }
}
