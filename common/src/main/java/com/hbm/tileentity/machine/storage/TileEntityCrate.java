package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerCrate;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;

public class TileEntityCrate extends TileEntityMachineBase implements MenuProvider {

    public final CrateType crateType;

    public TileEntityCrate(CrateType crateType, BlockPos pos, BlockState state) {
        super(crateType.blockEntityType(), pos, state, crateType.slots);
        this.crateType = crateType;
    }

    @Override
    public String getDefaultName() {
        return crateType.defaultName;
    }

    private NonNullList<ItemStack> getItems() {
        return inventory.getStacks();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(getItems());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(getItems()));
    }

    @Override
    public void removeComponentsFromTag(ValueOutput output) {

        output.discard("inventory");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm." + crateType.id);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCrate(containerId, playerInventory, this);
    }
}
