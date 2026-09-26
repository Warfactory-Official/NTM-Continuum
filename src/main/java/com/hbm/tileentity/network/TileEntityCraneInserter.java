package com.hbm.tileentity.network;

import com.hbm.interfaces.IControlReceiver;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.inventory.container.ContainerCraneInserter;
import com.hbm.tileentity.ModConveyorBlockEntities;
import com.hbm.util.ConveyorInventories;

/**
 * 1.7 {@code tileentity/network/TileEntityCraneInserter}: a 21-slot buffer that keeps trying to push into the
 * inventory on its output side, one stack per tick and one ITEM per tick where the stack will not fit.
 */
public class TileEntityCraneInserter extends TileEntityCraneBase implements IControlReceiver {

    public static final int SLOT_COUNT = 21;
    private static final int[] ACCESS = ConveyorInventories.range(SLOT_COUNT);

    public boolean destroyer = true;

    public TileEntityCraneInserter(BlockPos pos, BlockState state) {
        super(ModConveyorBlockEntities.CRANE_INSERTER.get(), pos, state, SLOT_COUNT);
    }

    @Override
    public String getDefaultName() {
        return "container.craneInserter";
    }

    public void serverTick() {
        if (level == null) return;
        if (!level.hasNeighborSignal(worldPosition)) push();
        syncState();
    }

    private void push() {
        Direction output = getOutputSide();
        Container target = ConveyorInventories.containerAt(level, worldPosition.relative(output));
        if (target == null) return;

        Direction face = output.getOpposite();
        int[] access = ConveyorInventories.masquerade(target, face);

        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack left = ConveyorInventories.addToInventory(target, access, stack.copy(), face);
            if (left.getCount() != stack.getCount()) {
                setItem(i, left);
                setChanged();
                return;
            }
        }

        // if the previous operation fails, repeat but use single items instead of the whole stack instead
        // this should fix cases where the inserter can't insert into something that has a stack size limitation
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;

            ItemStack left = ConveyorInventories.addToInventory(target, access, stack.copyWithCount(1), face);
            if (left.isEmpty()) {
                removeItem(i, 1);
                setChanged();
                return;
            }
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(destroyer);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        destroyer = buf.readBoolean();
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCraneInserter(containerId, inventory, this);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        destroyer = input.getBooleanOr("destroyer", false);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putBoolean("destroyer", destroyer);
    }

    @Override
    public boolean hasPermission(Player player) {
        return super.hasPermission(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("destroyer")) this.destroyer = !this.destroyer;
        setChanged();
    }
}
