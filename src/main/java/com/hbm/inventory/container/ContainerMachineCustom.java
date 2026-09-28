package com.hbm.inventory.container;

import com.hbm.inventory.ModCustomMachineMenus;
import com.hbm.tileentity.machine.TileEntityCustomMachine;
import dev.golem.core.gui.CompatContainerMenu;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code ContainerMachineCustom}: battery (150,72), one fluid-ID slot per input tank (8+18i, 54), then as many of
 * the six inputs (8/26/44, 72/90), six pattern filters (8/26/44, 108/126) and six outputs (78/96/114, 72/90) as the
 * configuration asks for; player inventory at (8,174), hotbar at 232. The slot list therefore depends on the
 * machine, which both sides read from the same config.
 *
 * <p>Filter clicks work as the old {@code slotClick}: a right-click on a set filter cycles its match mode, any other
 * click writes the carried stack into the filter. Shift-clicking moves nothing, as in 1.7.
 */
public final class ContainerMachineCustom extends CompatContainerMenu {

    private final Container machine;
    private final @Nullable TileEntityCustomMachine custom;

    public ContainerMachineCustom(int containerId, Inventory playerInv, BlockPos pos) {
        this(containerId, playerInv, resolve(playerInv, pos));
    }

    public ContainerMachineCustom(int containerId, Inventory playerInv, @Nullable TileEntityCustomMachine tile) {
        super(ModCustomMachineMenus.CUSTOM_MACHINE.get(), containerId);
        this.custom = tile;
        this.machine = tile != null ? tile : new SimpleContainer(22);
        int fluidIn = tile != null ? tile.inputTanks.length : 0;
        int itemIn = tile != null && tile.config != null ? tile.config.itemInCount : 0;
        int itemOut = tile != null && tile.config != null ? tile.config.itemOutCount : 0;

        //Input
        this.addSlot(new Slot(machine, 0, 150, 72));
        //Fluid IDs
        for (int i = 0; i < fluidIn; i++) {
            this.addSlot(new Slot(machine, 1 + i, 8 + 18 * i, 54));
        }
        //Item inputs
        if (itemIn > 0) this.addSlot(new Slot(machine, 4, 8, 72));
        if (itemIn > 1) this.addSlot(new Slot(machine, 5, 26, 72));
        if (itemIn > 2) this.addSlot(new Slot(machine, 6, 44, 72));
        if (itemIn > 3) this.addSlot(new Slot(machine, 7, 8, 90));
        if (itemIn > 4) this.addSlot(new Slot(machine, 8, 26, 90));
        if (itemIn > 5) this.addSlot(new Slot(machine, 9, 44, 90));
        //Templates
        if (itemIn > 0) this.addSlot(new SlotPattern(machine, 10, 8, 108));
        if (itemIn > 1) this.addSlot(new SlotPattern(machine, 11, 26, 108));
        if (itemIn > 2) this.addSlot(new SlotPattern(machine, 12, 44, 108));
        if (itemIn > 3) this.addSlot(new SlotPattern(machine, 13, 8, 126));
        if (itemIn > 4) this.addSlot(new SlotPattern(machine, 14, 26, 126));
        if (itemIn > 5) this.addSlot(new SlotPattern(machine, 15, 44, 126));
        //Output
        if (itemOut > 0) this.addSlot(new SlotCraftingOutput(machine, 16, 78, 72));
        if (itemOut > 1) this.addSlot(new SlotCraftingOutput(machine, 17, 96, 72));
        if (itemOut > 2) this.addSlot(new SlotCraftingOutput(machine, 18, 114, 72));
        if (itemOut > 3) this.addSlot(new SlotCraftingOutput(machine, 19, 78, 90));
        if (itemOut > 4) this.addSlot(new SlotCraftingOutput(machine, 20, 96, 90));
        if (itemOut > 5) this.addSlot(new SlotCraftingOutput(machine, 21, 114, 90));

        Compat.addPlayerInventory(this::addSlot, playerInv, 8, 174);
    }

    private static @Nullable TileEntityCustomMachine resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityCustomMachine machine ? machine : null;
    }

    public @Nullable TileEntityCustomMachine getCustom() {
        return custom;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine.stillValid(player);
    }

    /** 1.7 {@code transferStackInSlot} returned null: nothing moves on shift-click. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean handleClick(int slotId, int button, ClickAction action, Player player) {

        //L/R: 0
        //M3: 3
        //SHIFT: 1
        //DRAG: 5
        if (slotId < 0 || slotId >= this.slots.size() || !(this.slots.get(slotId) instanceof SlotPattern)) {
            return false;
        }

        Slot slot = this.slots.get(slotId);
        int index = slot.getContainerSlot();
        ItemStack held = getCarried();

        if (button == 1 && action == ClickAction.PICKUP && slot.hasItem()) {
            if (custom != null) custom.matcher.nextMode(player.level(), slot.getItem(), index - 10);
            return true;

        } else {

            machine.setItem(index, held.isEmpty() ? ItemStack.EMPTY : held.copy());
            slot.setChanged();
            if (custom != null) custom.matcher.initPatternSmart(player.level(), slot.getItem(), index - 10);

            return true;
        }
    }

    /** 1.7 {@code SlotPattern}: a filter that never takes or gives items through the vanilla paths. */
    public static class SlotPattern extends Slot {
        public SlotPattern(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean mayPickup(Player player) { return false; }
    }

    /** 1.7 {@code SlotCraftingOutput}: output only. */
    public static class SlotCraftingOutput extends Slot {
        public SlotCraftingOutput(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override public boolean mayPlace(ItemStack stack) { return false; }
    }
}
