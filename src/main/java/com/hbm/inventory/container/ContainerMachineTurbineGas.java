package com.hbm.inventory.container;

import com.hbm.inventory.ModMenus;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.Library;
import com.hbm.tileentity.machine.TileEntityMachineTurbineGas;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/** Menu for the gas turbine: battery and gas-fluid identifier. */
public class ContainerMachineTurbineGas extends AbstractContainerMenu {

    /**
     * Where the screen draws each tank.
     *
     * <p>These are screen coordinates, but the screen is client-only and a dedicated-server GameTest
     * pins them, so they live on the menu, the half of this machine's UI that both sides can load.
     */
    public static final int FUEL_TANK_TOP = 17;
    public static final int LUBRICANT_TANK_TOP = 71;
    public static final int WATER_TANK_TOP = 62;
    public static final int STEAM_TANK_TOP = 22;

    private static final int MACHINE_SLOTS = 2;
    private final Container machine;
    private final @Nullable TileEntityMachineTurbineGas turbine;

    public ContainerMachineTurbineGas(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, resolve(playerInventory, pos));
    }

    public ContainerMachineTurbineGas(int id, Inventory playerInventory, @Nullable TileEntityMachineTurbineGas turbine) {
        super(ModMenus.MACHINE_TURBINE_GAS.get(), id);
        this.turbine = turbine;
        this.machine = turbine != null ? turbine : new SimpleContainer(MACHINE_SLOTS);
        addSlot(validated(0, 8, 109));
        addSlot(validated(1, 36, 17));
        Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 141);
    }

    private Slot validated(int index, int x, int y) {
        return new Slot(machine, index, x, y) {
            @Override public boolean mayPlace(ItemStack stack) { return machine.canPlaceItem(index, stack); }
        };
    }

    private static @Nullable TileEntityMachineTurbineGas resolve(Inventory inventory, BlockPos pos) {
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        return be instanceof TileEntityMachineTurbineGas turbine ? turbine : null;
    }

    public @Nullable TileEntityMachineTurbineGas getTurbine() { return turbine; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (turbine == null) return false;
        if (id != 0 && id != 1 && id != 2 && (id < 1000 || id > 1060)) return false;
        turbine.handleButton(id);
        return true;
    }

    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS, invEnd = MACHINE_SLOTS + 36;
            if (index < invStart) {
                if (!moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (Library.isBattery(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof IItemFluidId identifier
                    && FluidCombustion.getCombustible(identifier.getType(player.level(), 0, 0, 0, stack)) != null
                    && FluidCombustion.getCombustible(identifier.getType(player.level(), 0, 0, 0, stack)).grade()
                    == FluidCombustion.FuelGrade.GAS) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        }
        return result;
    }
}
