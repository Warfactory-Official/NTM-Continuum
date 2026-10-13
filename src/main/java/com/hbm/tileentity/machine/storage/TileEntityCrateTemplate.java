package com.hbm.tileentity.machine.storage;

import com.hbm.inventory.container.ContainerCrateTemplate;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Ordinary 27-slot template crate support, isolated from the central three-crate registry row. */
public class TileEntityCrateTemplate extends TileEntityMachineBase implements MenuProvider {

    private final Supplier<MenuType<ContainerCrateTemplate>> menuType;

    public TileEntityCrateTemplate(
            Supplier<? extends BlockEntityType<?>> blockEntityType,
            Supplier<MenuType<ContainerCrateTemplate>> menuType, BlockPos pos, BlockState state) {
        super(blockEntityType.get(), pos, state, TemplateCrateLayout.SLOTS);
        this.menuType = menuType;
    }

    /** Registered instances use the lazy suppliers wired by the central rows. */
    public TileEntityCrateTemplate(BlockPos pos, BlockState state) {
        this(TemplateCrateLayout::blockEntityType, TemplateCrateLayout::menuType, pos, state);
    }

    @Override
    public String getDefaultName() {
        return "container.crateTemplate";
    }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName()) : Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCrateTemplate(containerId, playerInventory, this, menuType);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        // Contents travel with the dropped item, as with a shulker box.
    }

    @Override
    protected NonNullList<ItemStack> carriedItems() {
        return inventory.getStacks();
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("customName");
    }
}
