package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.inventory.ModMenus;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.oil.TileEntityOilDrillBase;
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

/**
 * Menu shared by the oil derrick and the pumpjack (both are {@link TileEntityOilDrillBase}), the
 * 1.7 {@code ContainerMachineOilWell} layout: battery (8,58), oil container pair (94,22)/(94,58),
 * gas container pair (130,22)/(130,58), 2 upgrade slots (156,36/54); 184x190 with the player
 * inventory at x=12, y=108/166.
 */
public class ContainerMachineOilWell extends AbstractContainerMenu {

    public static final int MACHINE_SLOTS = 8;
    /** Machine slots the menu shows (1.7 leaves inventory slot 7 out). */
    private static final int MENU_MACHINE_SLOTS = 7;

    private final Container machine;
    private final ContainerLevelAccess access;
    private final @Nullable TileEntityOilDrillBase drill;

    /** Client-side: resolve the BlockEntity by position. */
    public ContainerMachineOilWell(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, resolve(playerInventory, pos), ContainerLevelAccess.create(playerInventory.player.level(), pos));
    }

    /** Server-side: bound to the live BlockEntity. */
    public ContainerMachineOilWell(int containerId, Inventory playerInventory, TileEntityOilDrillBase drill) {
        this(containerId, playerInventory, drill, ContainerLevelAccess.create(drill.getLevel(), drill.getBlockPos()));
    }

    private ContainerMachineOilWell(int containerId, Inventory playerInventory, @Nullable TileEntityOilDrillBase drill, ContainerLevelAccess access) {
        super(ModMenus.MACHINE_OIL_WELL.get(), containerId);
        this.drill = drill;
        this.machine = drill != null ? drill : new SimpleContainer(MACHINE_SLOTS);
        this.access = access;

        // 1.7 ContainerMachineOilWell positions (184x190 GUI). 1.7 exposes only upgrade slots 5 and 6 even though
        // the drill's upgrade check reads 5..7; slot 7 has no frame on the 1.7 texture and is left out as in 1.7.
        this.addSlot(validated(0, 8, 58));    // battery
        this.addSlot(validated(1, 94, 22));   // oil container in
        this.addSlot(takeOnly(2, 94, 58));    // oil container out
        this.addSlot(validated(3, 130, 22));  // gas container in
        this.addSlot(takeOnly(4, 130, 58));   // gas container out
        this.addSlot(validated(5, 156, 36));  // upgrades
        this.addSlot(validated(6, 156, 54));

        Compat.addPlayerInventory(this::addSlot, playerInventory, 12, 108);
        // rows 108/126/144, hotbar 166 (1.7 layout)
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return ContainerMachineOilWell.this.machine.canPlaceItem(index, stack); }
        };
    }

    private Slot takeOnly(int index, int x, int y) {
        return new Slot(this.machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        };
    }

    private static @Nullable TileEntityOilDrillBase resolve(Inventory playerInventory, BlockPos pos) {
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityOilDrillBase drill ? drill : null;
    }

    /** The live (client) BlockEntity, for the screen to read power/indicator/tanks. */
    public @Nullable TileEntityOilDrillBase getDrill() {
        return drill;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.machine.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MENU_MACHINE_SLOTS;
            int invEnd = MENU_MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isMachineUpgrade(stack)) {
                if (!this.moveItemStackTo(stack, 5, 7, false)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else {
                // fluid containers go to the oil load slot, then the gas load slot (old TransferStrategy rules)
                if (!this.moveItemStackTo(stack, 1, 2, false)
                        && !this.moveItemStackTo(stack, 3, 4, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return result;
    }
}
