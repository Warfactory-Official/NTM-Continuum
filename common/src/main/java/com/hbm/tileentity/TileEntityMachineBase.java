package com.hbm.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class TileEntityMachineBase extends TileEntityInventoryBase {

    private String customName;

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

    public void handleButtonPacket(int value, int meta) {}

    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{};
    }

    public int countMufflers() {
        return 0;
    }

    public float getVolume(int toSilence) {
        return Math.max(1F - (countMufflers() / (float) toSilence), 0F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (hasCustomName()) output.putString("customName", customName);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        customName = input.getStringOr("customName", null);
    }

}
