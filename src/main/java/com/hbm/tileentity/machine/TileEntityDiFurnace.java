package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.blocks.machine.DiFurnaceContent;
import com.hbm.blocks.machine.MachineDiFurnace;
import com.hbm.inventory.container.ContainerDiFurnace;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Fuel-fired DiFurnace controller, ported from the old TileEntityDiFurnace. */
public class TileEntityDiFurnace extends TileEntityMachinePolluting
        implements MenuProvider, WorldlyContainer, com.hbm.api.fluidmk2.IFluidStandardSenderMK2 {

    public static final int MAX_FUEL = 12_800;
    public static final int PROCESSING_SPEED = 400;
    private static final int SMOKE_BUFFER = 50;

    private static final int[] TOP = {0, 1};
    private static final int[] BOTTOM = {3};
    private static final int[] SIDES = {2};

    public int progress;
    public int fuel;
    public byte sideFuel = 1;
    public byte sideUpper = 1;
    public byte sideLower = 1;

    public TileEntityDiFurnace(BlockPos pos, BlockState state) {
        super(DiFurnaceContent.DI_FURNACE.get(), pos, state, 4, SMOKE_BUFFER);
    }

    @Override
    public String getDefaultName() {
        return "container.diFurnace";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDiFurnace furnace) {
        boolean extension = MachineDiFurnace.hasExtension(level, pos);

        furnace.sendSmoke(level, pos, extension);

        boolean dirty = furnace.loadFuel();
        boolean canProcess = furnace.canProcess();
        if (canProcess) {
            furnace.fuel--;
            furnace.progress += extension ? 3 : 1;

            if (furnace.progress >= PROCESSING_SPEED) {
                furnace.progress -= PROCESSING_SPEED;
                furnace.processItem();
                dirty = true;
            }

            if (TickPhase.every(level, pos, 20L)) furnace.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * (extension ? 3 : 1));
        } else {
            furnace.progress = 0;
        }

        // Keep the old trigger gate: a completed cycle may leave progress at zero while the block stays lit
        // for one tick if another operation can start immediately.
        boolean trigger = !furnace.canProcess() || furnace.progress != 0;
        if (trigger) {
            dirty = true;
            MachineDiFurnace.updateBlockState(furnace.progress > 0, extension, level, pos);
        }

        if (dirty) furnace.setChanged();
        furnace.networkPackNT();
    }

    private boolean loadFuel() {
        ItemStack fuel = inventory.getStackInSlot(2);
        int fuelPower = DiFurnaceRecipes.getItemPower(fuel);
        if (fuel.isEmpty() || fuelPower <= 0 || this.fuel > MAX_FUEL - fuelPower) return false;

        // Match the working press/firebox path: capture the template before shrinking the live stack.
        ItemStack remainder = Compat.craftingRemainder(fuel);
        fuel.shrink(1);
        this.fuel += fuelPower;

        if (fuel.isEmpty()) {
            // A one-item fuel such as lava leaves its crafting remainder in the fuel slot.
            inventory.setStackInSlot(2, remainder);
        } else if (!remainder.isEmpty()) {
            // With more fuel remaining, slot 2 still contains the fuel item, so the old internal
            // insert falls through to the machine output slot instead.
            ItemStack rest = insertRaw(2, remainder, false);
            if (!rest.isEmpty()) insertRaw(3, rest, false);
        }
        return true;
    }

    private void sendSmoke(Level level, BlockPos pos, boolean extension) {
        for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
            sendSmoke(level, new DirPos(pos.getX() + direction.offsetX, pos.getY() + direction.offsetY,
                    pos.getZ() + direction.offsetZ, direction));
        }
        if (extension) sendSmoke(level, new DirPos(pos.above(2), ForgeDirection.UP));
    }

    private void sendSmoke(Level level, DirPos connection) {
        sendSmoke(connection);
    }

    public boolean hasItemPower(ItemStack stack) {
        return DiFurnaceRecipes.getItemPower(stack) > 0;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 3) return false;
        return slot != 2 || hasItemPower(stack);
    }

    /** Legacy sided-inventory rule: top inputs, bottom output, every horizontal side fuel. */
    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return side == Direction.DOWN.ordinal() ? BOTTOM
                : side == Direction.UP.ordinal() ? TOP : SIDES;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return getAccessibleSlotsFromSide(side.ordinal());
    }

    /** Old canInsertItem rule, including the item-identity exclusion between the two input slots. */
    public boolean canInsertItem(int slot, ItemStack stack) {
        if (!isItemValidForSlot(slot, stack)) return false;
        if (slot == 0) {
            ItemStack other = inventory.getStackInSlot(1);
            return other.isEmpty() || other.getItem() != stack.getItem();
        }
        if (slot == 1) {
            ItemStack other = inventory.getStackInSlot(0);
            return other.isEmpty() || other.getItem() != stack.getItem();
        }
        return true;
    }

    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == 3;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canInsertItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractItem(slot, stack, 1);
    }

    public boolean isUsableByPlayer(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64D;
    }

    public int getDiFurnaceProgressScaled(int scale) {
        return progress * scale / PROCESSING_SPEED;
    }

    public int getPowerRemainingScaled(int scale) {
        return fuel * scale / MAX_FUEL;
    }

    public boolean hasPower() {
        return fuel > 0;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    public boolean canProcess() {
        ItemStack first = inventory.getStackInSlot(0);
        ItemStack second = inventory.getStackInSlot(1);
        if (first.isEmpty() || second.isEmpty() || !hasPower()) return false;

        DiFurnaceRecipes.Match match = DiFurnaceRecipes.INSTANCE.getMatch(first, second);
        if (match == null) return false;

        ItemStack remainderFirst = remainder(first, match.firstCount());
        ItemStack remainderSecond = remainder(second, match.secondCount());
        GenericRecipe recipe = match.recipe();
        if (recipe.outputItem == null || recipe.outputItem.length == 0 || recipe.outputItem[0].isEmpty()) return false;

        return canStoreOutput(recipe.outputItem[0], remainderFirst, remainderSecond);
    }

    private boolean canStoreOutput(ItemStack... incoming) {
        ItemStack projected = inventory.getStackInSlot(3).copy();
        for (ItemStack stack : incoming) {
            if (stack.isEmpty()) continue;
            int limit = Math.min(inventory.getSlotLimit(3), stack.getMaxStackSize());
            if (!projected.isEmpty()) {
                if (!ItemStacks.sameItemAndData(projected, stack)
                        || projected.getCount() + stack.getCount() > limit) return false;
            } else if (stack.getCount() > limit) {
                return false;
            }
            if (projected.isEmpty()) projected = stack.copy();
            else projected.grow(stack.getCount());
        }
        return true;
    }

    private ItemStack remainder(ItemStack input, int consumedCount) {
        ItemStack remainder = Compat.craftingRemainder(input);
        if (!remainder.isEmpty()) remainder.setCount(remainder.getCount() * consumedCount);
        return remainder;
    }

    private void processItem() {
        ItemStack first = inventory.getStackInSlot(0);
        ItemStack second = inventory.getStackInSlot(1);
        DiFurnaceRecipes.Match match = DiFurnaceRecipes.INSTANCE.getMatch(first, second);
        if (match == null) return;

        ItemStack usedFirst = inventory.extractItem(0, match.firstCount(), false);
        ItemStack usedSecond = inventory.extractItem(1, match.secondCount(), false);
        GenericRecipe recipe = match.recipe();

        insertRaw(3, recipe.outputItem[0].copy(), false);
        insertRaw(3, remainder(usedFirst, usedFirst.getCount()), false);
        insertRaw(3, remainder(usedSecond, usedSecond.getCount()), false);
    }

    /** Raw insertion is used only for legacy crafting remainders; old ItemStackHandler internal writes
     * did not apply the external slot-validity filter to those remainders. */
    private ItemStack insertRaw(int slot, ItemStack incoming, boolean simulate) {
        if (incoming.isEmpty()) return ItemStack.EMPTY;
        ItemStack existing = inventory.getStackInSlot(slot);
        int limit = Math.min(inventory.getSlotLimit(slot), incoming.getMaxStackSize());
        if (!existing.isEmpty()) {
            if (!ItemStacks.sameItemAndData(existing, incoming)) return incoming;
            limit -= existing.getCount();
        }
        if (limit <= 0) return incoming;

        int amount = Math.min(limit, incoming.getCount());
        if (!simulate) {
            ItemStack result = existing.isEmpty() ? incoming.copy() : existing.copy();
            result.setCount(existing.isEmpty() ? amount : existing.getCount() + amount);
            inventory.setStackInSlot(slot, result);
        }
        if (amount == incoming.getCount()) return ItemStack.EMPTY;
        ItemStack remainder = incoming.copy();
        remainder.setCount(incoming.getCount() - amount);
        return remainder;
    }

    public void cycleSide(int slot) {
        if (slot == 0) sideUpper = (byte) ((sideUpper + 1) % 6);
        if (slot == 1) sideLower = (byte) ((sideLower + 1) % 6);
        if (slot == 2) sideFuel = (byte) ((sideFuel + 1) % 6);
        setChanged();
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return getSmokeTanks();
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return getSendingTanks();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerDiFurnace(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(progress);
        buf.writeShort(fuel);
        buf.writeByte(sideFuel);
        buf.writeByte(sideUpper);
        buf.writeByte(sideLower);
        smoke.serialize(buf);
        smoke_leaded.serialize(buf);
        smoke_poison.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readShort();
        fuel = buf.readShort();
        sideFuel = buf.readByte();
        sideUpper = buf.readByte();
        sideLower = buf.readByte();
        smoke.deserialize(buf);
        smoke_leaded.deserialize(buf);
        smoke_poison.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("powerTime", fuel);
        output.putShort("cookTime", (short) progress);
        output.putByte("sideFuel", sideFuel);
        output.putByte("sideUpper", sideUpper);
        output.putByte("sideLower", sideLower);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        fuel = input.getIntOr("powerTime", 0);
        progress = input.getShortOr("cookTime", (short) 0);
        sideFuel = input.getByteOr("sideFuel", (byte) 1);
        sideUpper = input.getByteOr("sideUpper", (byte) 1);
        sideLower = input.getByteOr("sideLower", (byte) 1);
    }

    public static void updateBlockState(boolean lit, boolean extension, Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.hasProperty(MachineDiFurnace.LIT) || !state.hasProperty(MachineDiFurnace.EXT)) return;
        BlockState updated = state.setValue(MachineDiFurnace.LIT, lit)
                .setValue(MachineDiFurnace.EXT, extension);
        if (updated != state) level.setBlock(pos, updated, 2);
    }
}
