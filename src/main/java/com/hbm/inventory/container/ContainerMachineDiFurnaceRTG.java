package com.hbm.inventory.container;

import com.hbm.items.machine.ItemRTGPellet;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;
import com.hbm.blocks.ModProduction2Blocks;
import com.hbm.inventory.ModProduction2Menus;
import com.hbm.tileentity.machine.TileEntityDiFurnaceRTG;

/** 1.7 {@code ContainerMachineDiFurnaceRTG}: 176x166, two inputs, output, six pellets, right-click side cycling on the inputs. */
public final class ContainerMachineDiFurnaceRTG extends CompatContainerMenu {

    public static final int MACHINE_SLOTS = 9;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityDiFurnaceRTG furnace;

    public ContainerMachineDiFurnaceRTG(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineDiFurnaceRTG(int containerId, Inventory playerInventory, TileEntityDiFurnaceRTG furnace) {
        this(containerId, playerInventory, furnace, ContainerLevelAccess.create(furnace.getLevel(), furnace.getBlockPos()));
    }

    private ContainerMachineDiFurnaceRTG(int containerId, Inventory playerInventory,
                                         @Nullable TileEntityDiFurnaceRTG furnace, ContainerLevelAccess access) {
        super(ModProduction2Menus.DIFURNACE_RTG.get(), containerId);
        this.furnace = furnace;
        this.machine = furnace != null ? furnace : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // Input
        addSlot(validated(0, 80, 18));
        addSlot(validated(1, 80, 54));
        // Output
        addSlot(takeOnly(2, 134, 36));
        // RTG pellets
        addSlot(validated(3, 22, 18));
        addSlot(validated(4, 40, 18));
        addSlot(validated(5, 22, 36));
        addSlot(validated(6, 40, 36));
        addSlot(validated(7, 22, 54));
        addSlot(validated(8, 40, 54));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 84);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityDiFurnaceRTG resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityDiFurnaceRTG furnace ? furnace : null;
    }

    public @Nullable TileEntityDiFurnaceRTG getFurnace() { return furnace; }

    /** 1.7 {@code slotClick}: a right click on an empty input with an empty cursor cycles that input's accepted side. */
    @Override
    protected boolean handleClick(int slotId, int button, ClickAction clickType, Player player) {
        if (furnace != null && slotId >= 0 && slotId < 2 && button == 1
                && clickType == ClickAction.PICKUP && !slots.get(slotId).hasItem()
                && getCarried().isEmpty()) {
            if (!player.level().isClientSide()) furnace.cycleSide(slotId);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        return com.hbm.inventory.container.Containers.stillValidMultiblock(access, player,
                ModProduction2Blocks.MACHINE_DIFURNACE_RTG.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemRTGPellet) {
                if (!moveItemStackTo(stack, 3, 9, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
