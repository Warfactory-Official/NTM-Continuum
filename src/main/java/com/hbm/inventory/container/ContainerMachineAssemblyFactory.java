package com.hbm.inventory.container;

import com.hbm.assemblyfactory.AssemblyFactoryRegistrations;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineAssemblyFactory;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the Assembly Factory: 60 machine slots and the 36-slot player inventory. */
public final class ContainerMachineAssemblyFactory extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityMachineAssemblyFactory.MACHINE_SLOTS;
    private static final int PLAYER_INV_START = MACHINE_SLOTS;
    private static final int PLAYER_INV_END = PLAYER_INV_START + 36;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineAssemblyFactory factory;

    public ContainerMachineAssemblyFactory(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineAssemblyFactory(int containerId, Inventory playerInventory,
                                           TileEntityMachineAssemblyFactory factory) {
        this(containerId, playerInventory, factory,
                ContainerLevelAccess.create(factory.getLevel(), factory.getBlockPos()));
    }

    private ContainerMachineAssemblyFactory(int containerId, Inventory playerInventory,
                                            @Nullable TileEntityMachineAssemblyFactory factory,
                                            ContainerLevelAccess access) {
        super(AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY_MENU.get(), containerId);
        this.factory = factory;
        this.machine = factory != null ? factory : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 234, 112));
        addSlot(validated(1, 214, 149));
        addSlot(validated(2, 214, 167));
        addSlot(validated(3, 214, 185));

        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int x = 25 + (lane % 2) * 109;
            int y = 54 + (lane / 2) * 56;
            addSlot(validated(TileEntityMachineAssemblyFactory.templateSlot(lane), x, y));
            for (int row = 0; row < 2; row++) for (int column = 0; column < 6; column++) {
                int input = TileEntityMachineAssemblyFactory.inputStart(lane) + row * 6 + column;
                addSlot(validated(input, 7 + (lane % 2) * 109 + column * 16,
                        20 + (lane / 2) * 56 + row * 16));
            }
            addSlot(new FactoryOutputSlot(playerInventory.player, machine,
                    TileEntityMachineAssemblyFactory.outputSlot(lane),
                    87 + (lane % 2) * 109, 54 + (lane / 2) * 56));
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 33, 158);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return machine.canPlaceItem(index, stack);
            }
        };
    }

    private static @Nullable TileEntityMachineAssemblyFactory resolve(Inventory inventory, BlockPos pos) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        return blockEntity instanceof TileEntityMachineAssemblyFactory factory ? factory : null;
    }

    public @Nullable TileEntityMachineAssemblyFactory getFactory() { return factory; }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player,
                AssemblyFactoryRegistrations.MACHINE_ASSEMBLY_FACTORY.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack source = slot.getItem();
        ItemStack result = source.copy();
        int originalCount = source.getCount();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(source, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
        } else if (Library.isBattery(source)) {
            if (!moveItemStackTo(source, 0, 1, false)) return ItemStack.EMPTY;
        } else if (source.getItem() instanceof ItemBlueprints) {
            // The old source repeated the first blueprint branch four times; route to the next empty lane.
            if (!moveBlueprint(source)) return ItemStack.EMPTY;
        } else if (Library.isMachineUpgrade(source)) {
            if (!moveItemStackTo(source, 1, 4, false)) return ItemStack.EMPTY;
        } else if (!moveInputs(source)) {
            return ItemStack.EMPTY;
        }

        if (source.getCount() == originalCount) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, result.copyWithCount(originalCount - source.getCount()));
        return result;
    }

    private boolean moveBlueprint(ItemStack source) {
        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int slot = TileEntityMachineAssemblyFactory.templateSlot(lane);
            if (!machine.getItem(slot).isEmpty()) continue;
            if (moveItemStackTo(source, slot, slot + 1, false)) return true;
        }
        return false;
    }

    private boolean moveInputs(ItemStack source) {
        for (int lane = 0; lane < TileEntityMachineAssemblyFactory.LANES; lane++) {
            int start = TileEntityMachineAssemblyFactory.inputStart(lane);
            if (moveItemStackTo(source, start, start + 12, false)) return true;
        }
        return false;
    }

    /** Modern equivalent of the old SlotCraftingOutput, including crafted-by accounting. */
    private static final class FactoryOutputSlot extends Slot {
        private final Player player;
        private int craftBuffer;

        private FactoryOutputSlot(Player player, Container container, int index, int x, int y) {
            super(container, index, x, y);
            this.player = player;
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }

        @Override public ItemStack remove(int amount) {
            if (hasItem()) craftBuffer += Math.min(amount, getItem().getCount());
            return super.remove(amount);
        }

        @Override public void onTake(Player player, ItemStack stack) {
            onCrafting(stack);
            super.onTake(player, stack);
        }

        @Override protected void onQuickCraft(ItemStack stack, int amount) { craftBuffer += amount; }

        private void onCrafting(ItemStack stack) {
            if (craftBuffer <= 0) return;
            dev.golem.core.world.ItemStacks.onCraftedBy(stack, player, craftBuffer);
            craftBuffer = 0;
        }
    }
}
