package com.hbm.tileentity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * The canonical base class for HBM machines, ported from 1.12.2's {@code TileEntityMachineBase}. On
 * top of {@link TileEntityInventoryBase} it adds a custom name, the GUI button-packet hook, the
 * side-accessible-slots query, and the muffler/volume helpers.
 *
 * <p>Use this for all machines: it carries the inventory, the chunk-load + per-tick sync, and the
 * loaded flag, so a concrete machine only implements its own logic, slots layout, and
 * {@link #getDefaultName()}.
 */
public abstract class TileEntityMachineBase extends TileEntityInventoryBase {

    private String customName;

    public TileEntityMachineBase(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state, slotCount);
    }

    // --- naming ---

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

    // --- GUI / sided access hooks ---

    /** Handles a button press routed from the GUI. {@code value} is the button id, {@code meta} extra. */
    public void handleButtonPacket(int value, int meta) {}

    /**
     * @return the slots accessible from {@code side}; {@code null} = full access, empty array = none.
     * Default: no automation access (matches the old base's empty default).
     */
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{};
    }

    // --- muffler / volume ---

    /**
     * Counts adjacent muffler blocks. The muffler block isn't ported yet (see IMPLEMENTATION.md §19),
     * so this returns 0 for now — meaning no machine is muffled, the faithful default when no mufflers
     * are placed. Wire this to the ported block when it lands.
     */
    public int countMufflers() {
        return 0;
    }

    /** Volume scaled by adjacent mufflers; {@code toSilence} = mufflers needed for full silence. */
    public float getVolume(int toSilence) {
        return Math.max(1F - (countMufflers() / (float) toSilence), 0F);
    }

    // --- persistence ---

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        if (hasCustomName()) output.putString("customName", customName);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        customName = input.getStringOr("customName", null);
    }

    // Per-tick sync: machines only sync the muffled flag by default (matches the old override).
    // Concrete machines override serialize/deserialize to add their own fields.
}
