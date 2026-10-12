package com.hbm.tileentity.machine;

import com.hbm.inventory.recipes.DiFurnaceRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemRTGPellet;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.RTGUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
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
import com.hbm.blocks.machine.BlockRtgFurnaceBase;
import com.hbm.inventory.container.ContainerMachineDiFurnaceRTG;
import com.hbm.tileentity.ModProduction2BlockEntities;

/**
 * 1.7 {@code TileEntityDiFurnaceRTG}: the nuclear blast furnace. Slots 0-1 inputs, 2 output, 3-8 RTG
 * pellets. Progress advances by the summed pellet heat per tick (at least 15 to run) until 1,200. The
 * two-input recipe table is the port's {@link DiFurnaceRecipes} (1.7 {@code BlastFurnaceRecipes}).
 */
public class TileEntityDiFurnaceRTG extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public short progress;
    private short processSpeed = 0;
    // Edit as needed
    private static final short timeRequired = 1200;
    private static final int[] rtgIn = new int[] {3, 4, 5, 6, 7, 8};
    private static final int[] ALL_SLOTS = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8};
    public byte sideUpper = 1;
    public byte sideLower = 1;

    public TileEntityDiFurnaceRTG(BlockPos pos, BlockState state) {
        super(ModProduction2BlockEntities.DIFURNACE_RTG.get(), pos, state, 9);
    }

    @Override
    public String getDefaultName() {
        return "container.diFurnaceRTG";
    }

    public boolean canProcess() {
        ItemStack first = inventory.getStackInSlot(0);
        ItemStack second = inventory.getStackInSlot(1);
        // 1.7: ((slots[0] == null || slots[1] == null) && !hasPower()) - note the extra hasPower() (and pellet decay) in this branch
        if ((first.isEmpty() || second.isEmpty()) && !hasPower())
            return false;

        DiFurnaceRecipes.Match match = DiFurnaceRecipes.INSTANCE.getMatch(first, second);
        if (match == null) return false;
        GenericRecipe recipe = match.recipe();
        if (recipe.outputItem == null || recipe.outputItem.length == 0 || recipe.outputItem[0].isEmpty()) return false;
        ItemStack recipeResult = recipe.outputItem[0];

        ItemStack output = inventory.getStackInSlot(2);
        if (output.isEmpty())
            return true;
        else if (!ItemStack.isSameItem(output, recipeResult))
            return false;
        else if (output.getCount() + recipeResult.getCount() > 64)
            return false;
        else if (output.getCount() < 64 && output.getCount() < output.getMaxStackSize())
            return true;
        else
            return output.getCount() < recipeResult.getMaxStackSize();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityDiFurnaceRTG te) {
        if (te.canProcess() && te.hasPower()) {
            te.progress += te.processSpeed;
            if (te.progress >= timeRequired) {
                te.processItem();
                te.progress = 0;
            }
        } else {
            te.progress = 0;
        }

        // 1.7 calls hasPower() (and so pellet decay) again here; kept as-is.
        BlockRtgFurnaceBase.updateBlockState(te.isProcessing() || (te.canProcess() && te.hasPower()), level, pos);

        te.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort(progress);
        buf.writeShort(processSpeed);
        buf.writeByte(sideUpper);
        buf.writeByte(sideLower);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readShort();
        processSpeed = buf.readShort();
        sideUpper = buf.readByte();
        sideLower = buf.readByte();
    }

    private void processItem() {
        if (canProcess()) {
            ItemStack first = inventory.getStackInSlot(0);
            ItemStack second = inventory.getStackInSlot(1);
            DiFurnaceRecipes.Match match = DiFurnaceRecipes.INSTANCE.getMatch(first, second);
            if (match == null) return;
            ItemStack recipeOut = match.recipe().outputItem[0];

            ItemStack output = inventory.getStackInSlot(2);
            if (output.isEmpty()) {
                inventory.setStackInSlot(2, recipeOut.copy());
            } else if (ItemStack.isSameItem(output, recipeOut)) {
                output.grow(recipeOut.getCount());
                inventory.setStackInSlot(2, output);
            }

            // 1.7 took exactly one of each; the port's recipe table carries per-input counts.
            inventory.extractItem(0, match.firstCount(), false);
            inventory.extractItem(1, match.secondCount(), false);
            setChanged();
        }
    }

    @Override
    protected void loadData(DataIn nbt) {
        super.loadData(nbt);
        progress = nbt.getShortOr("progress", (short) 0);
        processSpeed = nbt.getShortOr("speed", (short) 0);
        sideUpper = nbt.getByteOr("sideUpper", (byte) 1);
        sideLower = nbt.getByteOr("sideLower", (byte) 1);
    }

    @Override
    protected void saveData(DataOut nbt) {
        super.saveData(nbt);
        nbt.putShort("progress", progress);
        nbt.putShort("speed", processSpeed);
        nbt.putByte("sideUpper", sideUpper);
        nbt.putByte("sideLower", sideLower);
    }

    public int getDiFurnaceProgressScaled(int i) {
        return (progress * i) / timeRequired;
    }

    public boolean hasPower() {
        processSpeed = (short) RTGUtil.updateRTGs(inventory, rtgIn);
        return processSpeed >= 15;
    }

    public int getPower() {
        return processSpeed;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    public void cycleSide(int slot) {
        if (slot == 0) sideUpper = (byte) ((sideUpper + 1) % 6);
        if (slot == 1) sideLower = (byte) ((sideLower + 1) % 6);
        setChanged();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == 0 && this.sideUpper != side.ordinal()) return false;
        if (slot == 1 && this.sideLower != side.ordinal()) return false;

        return this.isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 2) return false;
        if (stack.getItem() instanceof ItemRTGPellet) return slot > 2;
        return !(stack.getItem() instanceof ItemRTGPellet);
    }

    @Override public int[] getSlotsForFace(Direction side) { return ALL_SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return ALL_SLOTS; }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot > 2) {
            return !(stack.getItem() instanceof ItemRTGPellet);
        }
        return slot == 2;
    }

    @Override public Component getDisplayName() { return Component.translatable("container.diFurnaceRTG"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineDiFurnaceRTG(containerId, playerInventory, this);
    }
}
