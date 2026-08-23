package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerCrate;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.NonNullList;
//? if >=26.1 {
import net.minecraft.core.component.DataComponentGetter;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;

/**
 * The HBM storage crate BlockEntity — a plain slot inventory whose size and GUI layout come from its
 * {@link CrateType} (iron 36 / steel 54 / desh 104). One class serves all three crates; the type is
 * passed in by the block / {@link com.hbm.tileentity.ModBlockEntities} factory.
 *
 * <p><b>Contents travel with the dropped crate</b> (the 1.12.2 {@code CRATE_KEEP_CONTENTS=true} default):
 * implemented the vanilla shulker-box way — the inventory round-trips through the
 * {@code minecraft:container} component on 1.20.5+ and the legacy block-entity item tag on 1.20.1.
 * The crate's loot table copies that component from the block
 * entity onto the dropped item, so breaking a crate gives you a crate item holding everything, and
 * placing it back restores the contents. (The 1.12.2 lock/lockpicking, hazard-radiation, loot-table
 * fill, and hand-held "open from item" behaviours are dropped — see IMPLEMENTATION.md.)
 */
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

    /**
     * Don't spill the inventory on removal — the contents travel with the dropped crate item via the
     * CONTAINER component + loot table. The {@link net.minecraft.world.level.block.entity.BlockEntity}
     * default drops a {@link net.minecraft.world.Container}'s contents (chest behaviour); the crate keeps
     * them (the shulker box overrides this the same way).
     */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
    }

    // --- contents-in-item ---

    //? if >=26.1 {
    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(getItems());
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(getItems()));
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        // The inventory lives in the CONTAINER component on the item; drop the redundant saved copy
        // (mirrors vanilla discarding "Items"). Only affects the save-to-item path, not the world save.
        output.discard("inventory");
    }
    //?} else if >=1.20.5 {
    /*@Override
    protected void applyImplicitComponents(net.minecraft.world.level.block.entity.BlockEntity.DataComponentInput components) {
        super.applyImplicitComponents(components);
        components.getOrDefault(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.EMPTY).copyInto(getItems());
    }

    @Override
    protected void collectImplicitComponents(net.minecraft.core.component.DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(net.minecraft.core.component.DataComponents.CONTAINER,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(getItems()));
    }
    *///?}

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm." + crateType.id);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCrate(containerId, playerInventory, this);
    }
}
