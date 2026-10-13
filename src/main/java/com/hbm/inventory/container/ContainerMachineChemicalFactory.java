package com.hbm.inventory.container;

import com.hbm.inventory.SlotCraftingOutput;
import com.hbm.lib.Library;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.tileentity.machine.TileEntityMachineChemicalFactory;
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
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.ModMenus;

/** The 32-slot four-lane Chemical Factory menu. */
public class ContainerMachineChemicalFactory extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = TileEntityMachineChemicalFactory.MACHINE_SLOTS;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityMachineChemicalFactory factory;

    public ContainerMachineChemicalFactory(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos),
                ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    public ContainerMachineChemicalFactory(int containerId, Inventory playerInventory,
                                           TileEntityMachineChemicalFactory factory) {
        this(containerId, playerInventory, factory,
                ContainerLevelAccess.create(factory.getLevel(), factory.getBlockPos()));
    }

    private ContainerMachineChemicalFactory(int containerId, Inventory playerInventory,
                                             @Nullable TileEntityMachineChemicalFactory factory,
                                             ContainerLevelAccess access) {
        super(ModMenus.MACHINE_CHEMICAL_FACTORY.get(), containerId);
        this.factory = factory;
        this.machine = factory != null ? factory : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        addSlot(validated(0, 224, 88));
        addSlot(validated(1, 206, 125));
        addSlot(validated(2, 206, 143));
        addSlot(validated(3, 206, 161));

        for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
            int rowY = 20 + lane * 22;
            addSlot(validated(TileEntityMachineChemicalFactory.blueprintSlot(lane), 93, rowY));
            for (int i = 0; i < 3; i++) {
                addSlot(validated(TileEntityMachineChemicalFactory.inputStart(lane) + i, 10 + i * 16, rowY)); // 1.7 addSlots(..., 1, 3, 16): 16 px pitch
            }
            for (int i = 0; i < 3; i++) {
                addSlot(new SlotCraftingOutput(playerInventory.player, this.machine, TileEntityMachineChemicalFactory.outputStart(lane) + i,
                        139 + i * 16, rowY)); // 1.7 addOutputSlots(..., 1, 3, 16): 16 px pitch
            }
        }

        Compat.addPlayerInventory(this::addSlot, playerInventory, 26, 134);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) {
                return ContainerMachineChemicalFactory.this.machine.canPlaceItem(index, stack);
            }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityMachineChemicalFactory resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineChemicalFactory factory ? factory : null;
    }

    public @Nullable TileEntityMachineChemicalFactory getFactory() { return factory; }

    @Override
    public boolean stillValid(Player player) {
        return Containers.stillValidMultiblock(access, player,
                ModMachineBlocks.machine_chemical_factory.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < MACHINE_SLOTS) {
                if (!moveItemStackTo(stack, MACHINE_SLOTS, MACHINE_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemBlueprints) {
                // The legacy source repeated the first branch four times; route each blueprint to one lane.
                boolean moved = false;
                for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
                    int blueprint = TileEntityMachineChemicalFactory.blueprintSlot(lane);
                    if (!machine.getItem(blueprint).isEmpty()) continue;
                    if (moveItemStackTo(stack, blueprint, blueprint + 1, false)) {
                        moved = true;
                        break;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof ItemMachineUpgrade) {
                if (!moveItemStackTo(stack, 1, 4, false)) return ItemStack.EMPTY;
            } else {
                boolean moved = false;
                for (int lane = 0; lane < TileEntityMachineChemicalFactory.LANES; lane++) {
                    int input = TileEntityMachineChemicalFactory.inputStart(lane);
                    if (moveItemStackTo(stack, input, input + 3, false)) {
                        moved = true;
                        break;
                    }
                }
                if (!moved) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            slot.onTake(player, result); // 1.7 transferStackInSlot: slot.onPickupFromSlot (SlotCraftingOutput)
        }
        return result;
    }
}
