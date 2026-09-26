package com.hbm.tileentity;

import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.machine.InventoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** HBM machine on Core's {@link InventoryBlockEntity}: custom name, muffling, upgrade plug sound, GUI button hook. */
public abstract class TileEntityMachineBase extends InventoryBlockEntity {

    private String customName;
    public boolean muffled = false;

    public TileEntityMachineBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state, slotCount);
    }

    public String getName() {
        return hasCustomName() ? customName : getDefaultName();
    }

    public abstract String getDefaultName();

    public boolean hasCustomName() {
        return customName != null && !customName.isEmpty();
    }

    public void setCustomName(String name) {
        this.customName = name;
    }

    @Override
    protected Component collectCarriedName() {
        return hasCustomName() ? Component.literal(customName) : null;
    }

    @Override
    protected void applyCarriedName(Component name) {
        customName = name.getString();
    }

    /** GUI button: {@code value} = button id, {@code meta} extra. */
    public void handleButtonPacket(int value, int meta) {}

    /** @return slots accessible from {@code side}; {@code null} = all, empty = none (default). */
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{};
    }

    /** Inclusive {@code {first, last}} upgrade slots, or null; an upgrade inserted there plays the plug sound. */
    protected int[] getUpgradeSlotRange() {
        return null;
    }

    @Override
    protected void onSlotContentsChanged(int slot, ItemStack stack) {
        int[] range = getUpgradeSlotRange();
        if (range != null && slot >= range[0] && slot <= range[1] && Library.isMachineUpgrade(stack)
                && level != null && !level.isClientSide()) {
            level.playSound(null, worldPosition, ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public float getVolume(float baseVolume) {
        return muffled ? baseVolume * 0.1F : baseVolume;
    }

    public void setMuffled(boolean muffled) {
        this.muffled = muffled;
        dataChanged();
    }

    /** Muffler block unported => 0. */
    public int countMufflers() {
        return 0;
    }

    /** {@code toSilence} = mufflers for full silence. */
    public float getVolume(int toSilence) {
        return Math.max(1F - (countMufflers() / (float) toSilence), 0F);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("muffled", muffled);
        if (hasCustomName()) output.putString("customName", customName);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        muffled = input.getBooleanOr("muffled", false);
        customName = input.getStringOr("customName", null);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(muffled);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        muffled = buf.readBoolean();
    }
}
