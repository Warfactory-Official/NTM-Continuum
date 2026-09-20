package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.inventory.container.ContainerFurnaceBrick;
import com.hbm.items.ModItems;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/** The ordinary brick furnace, including its three accumulated ash streams. */
public class TileEntityFurnaceBrick extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    private static final int[] TOP = {0};
    private static final int[] BOTTOM = {2, 3};
    private static final int[] SIDES = {1};
    private static final Map<Item, Integer> BURN_SPEED = new HashMap<>();

    static {
        BURN_SPEED.put(Items.CLAY_BALL, 4);
        BURN_SPEED.put(ModItems.ball_fireclay.get(), 4);
        BURN_SPEED.put(Items.NETHERRACK, 4);
        BURN_SPEED.put(Items.COBBLESTONE, 2);
        BURN_SPEED.put(Items.SAND, 2);
    }

    public int burnTime;
    public int maxBurnTime;
    public int progress;
    public int ashLevelWood;
    public int ashLevelCoal;
    public int ashLevelMisc;

    public TileEntityFurnaceBrick(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_BRICK.get(), pos, state, 4);
    }

    @Override
    public String getDefaultName() {
        return "container.furnaceBrick";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceBrick furnace) {
        boolean wasBurning = furnace.burnTime > 0;
        boolean dirty = false;

        if (furnace.burnTime > 0) furnace.burnTime--;

        ItemStack input = furnace.inventory.getStackInSlot(0);
        ItemStack fuel = furnace.inventory.getStackInSlot(1);
        if (furnace.burnTime != 0 || (!fuel.isEmpty() && !input.isEmpty())) {
            if (furnace.burnTime == 0 && furnace.canSmelt(level)) {
                int burn = Compat.burnDuration(level, fuel);
                if (burn > 0) {
                    furnace.maxBurnTime = furnace.burnTime = burn;
                    com.hbm.items.ItemEnums.EnumAshType type = TileEntityFireboxBase.getAshFromFuel(fuel);
                    if (type == com.hbm.items.ItemEnums.EnumAshType.WOOD) furnace.ashLevelWood += burn;
                    if (type == com.hbm.items.ItemEnums.EnumAshType.COAL) furnace.ashLevelCoal += burn;
                    if (type == com.hbm.items.ItemEnums.EnumAshType.MISC) furnace.ashLevelMisc += burn;

                    int threshold = 2_000;
                    if (furnace.processAsh(furnace.ashLevelWood, com.hbm.items.ItemEnums.EnumAshType.WOOD, threshold)) furnace.ashLevelWood -= threshold;
                    if (furnace.processAsh(furnace.ashLevelCoal, com.hbm.items.ItemEnums.EnumAshType.COAL, threshold)) furnace.ashLevelCoal -= threshold;
                    if (furnace.processAsh(furnace.ashLevelMisc, com.hbm.items.ItemEnums.EnumAshType.MISC, threshold)) furnace.ashLevelMisc -= threshold;

                    ItemStack remainder = Compat.craftingRemainder(fuel);
                    fuel.shrink(1);
                    if (fuel.isEmpty()) furnace.inventory.setStackInSlot(1, remainder);
                    dirty = true;
                }
            }

            if (furnace.burnTime > 0 && furnace.canSmelt(level)) {
                furnace.progress += furnace.getBurnSpeed();
                if (furnace.progress >= 200) {
                    furnace.progress = 0;
                    furnace.smeltItem(level);
                    dirty = true;
                }
            } else {
                furnace.progress = 0;
            }
        }

        boolean burning = furnace.burnTime > 0;
        if (wasBurning != burning) {
            if (state.hasProperty(com.hbm.blocks.machine.MachineFurnaceBrick.LIT)
                    && state.getValue(com.hbm.blocks.machine.MachineFurnaceBrick.LIT) != burning) {
                level.setBlock(pos, state.setValue(com.hbm.blocks.machine.MachineFurnaceBrick.LIT, burning), 3);
            }
            dirty = true;
        }
        if (dirty) furnace.setChanged();
        furnace.networkPackNT();
    }

    public int getBurnSpeed() {
        Integer speed = BURN_SPEED.get(inventory.getStackInSlot(0).getItem());
        if (speed != null) return speed;
        return inventory.getStackInSlot(0).is(net.minecraft.tags.ItemTags.LOGS) ? 2 : 1;
    }

    public boolean canSmelt(Level level) {
        if (inventory.getStackInSlot(0).isEmpty()) return false;
        ItemStack result = Recipes.smeltingResult(level, inventory.getStackInSlot(0)).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return false;
        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty()) return true;
        if (!dev.golem.core.world.ItemStacks.sameItemAndData(output, result)) return false;
        return output.getCount() + result.getCount() <= 64
                && output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    public void smeltItem(Level level) {
        if (!canSmelt(level)) return;
        ItemStack result = Recipes.smeltingResult(level, inventory.getStackInSlot(0)).orElse(ItemStack.EMPTY);
        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty()) inventory.setStackInSlot(2, result.copy());
        else output.grow(result.getCount());
        inventory.getStackInSlot(0).shrink(1);
    }

    protected boolean processAsh(int level, com.hbm.items.ItemEnums.EnumAshType type, int threshold) {
        if (level < threshold) return false;
        ItemStack ash = new ItemStack(com.hbm.items.ModIngredientItems.ash(type));
        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) {
            inventory.setStackInSlot(3, ash);
            return true;
        }
        if (output.is(ash.getItem()) && output.getCount() < output.getMaxStackSize()) {
            output.grow(1);
            return true;
        }
        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 || slot == 1 && level != null && Compat.burnDuration(level, stack) > 0;
    }

    @Override public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? BOTTOM : side == Direction.UP ? TOP : SIDES;
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 2; }

    @Override public Component getDisplayName() { return Component.translatable("container.furnaceBrick"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerFurnaceBrick(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(burnTime);
        buf.writeInt(maxBurnTime);
        buf.writeInt(progress);
        buf.writeInt(ashLevelWood);
        buf.writeInt(ashLevelCoal);
        buf.writeInt(ashLevelMisc);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        burnTime = buf.readInt();
        maxBurnTime = buf.readInt();
        progress = buf.readInt();
        ashLevelWood = buf.readInt();
        ashLevelCoal = buf.readInt();
        ashLevelMisc = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("burnTime", burnTime);
        output.putInt("maxBurn", maxBurnTime);
        output.putInt("progress", progress);
        output.putInt("ashWood", ashLevelWood);
        output.putInt("ashCoal", ashLevelCoal);
        output.putInt("ashMisc", ashLevelMisc);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        burnTime = input.getIntOr("burnTime", 0);
        maxBurnTime = input.getIntOr("maxBurn", 0);
        progress = input.getIntOr("progress", 0);
        ashLevelWood = input.getIntOr("ashWood", 0);
        ashLevelCoal = input.getIntOr("ashCoal", 0);
        ashLevelMisc = input.getIntOr("ashMisc", 0);
    }
}
