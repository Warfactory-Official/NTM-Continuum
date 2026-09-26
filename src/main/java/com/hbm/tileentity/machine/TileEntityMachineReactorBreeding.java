package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.inventory.recipes.BreederRecipes;
import com.hbm.inventory.recipes.BreederRecipes.BreederRecipe;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
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
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.inventory.container.ContainerMachineReactorBreeding;
import com.hbm.tileentity.ModNuclearMiscBlockEntities;

/** The legacy breeder: flux from adjacent research reactors advances the rod in slot 0 into slot 1. */
public final class TileEntityMachineReactorBreeding extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public int flux;
    public float progress;

    private static final int[] slots_io = {0, 1};

    public TileEntityMachineReactorBreeding(BlockPos pos, BlockState state) {
        super(ModNuclearMiscBlockEntities.REACTOR_BREEDING.get(), pos, state, 2);
    }

    @Override
    public String getDefaultName() {
        return "container.reactorBreeding";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineReactorBreeding te) {
        te.flux = 0;
        te.getInteractions(level);

        if (te.canProcess()) {
            te.progress += 0.0025F * (te.flux / BreederRecipes.getOutput(te.inventory.getStackInSlot(0)).flux());

            if (te.progress >= 1.0F) {
                te.progress = 0F;
                te.processItem();
                te.setChanged();
            }
        } else {
            te.progress = 0.0F;
        }

        te.syncState();
    }

    /** Every research reactor touching one of the four sides (any cell of it) feeds its total flux in. */
    public void getInteractions(Level level) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos neighbour = worldPosition.relative(dir);
            if (level.getBlockState(neighbour).getBlock() != ModNuclearMiscBlocks.REACTOR_RESEARCH.get()) continue;

            BlockPos core = MultiblockSurface.coreOfAny(level, neighbour);
            if (core != null && level.getBlockEntity(core) instanceof TileEntityReactorResearch reactor) {
                this.flux += reactor.totalFlux;
            }
        }
    }

    public boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return false;

        BreederRecipe recipe = BreederRecipes.getOutput(input);
        if (recipe == null) return false;
        if (this.flux < recipe.flux()) return false;

        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) return true;
        if (!ItemStacks.sameItemAndData(output, recipe.output())) return false;
        return output.getCount() < output.getMaxStackSize();
    }

    private void processItem() {
        if (!canProcess()) return;

        BreederRecipe rec = BreederRecipes.getOutput(inventory.getStackInSlot(0));
        if (rec == null) return;

        ItemStack itemStack = rec.output();
        ItemStack output = inventory.getStackInSlot(1);
        if (output.isEmpty()) {
            inventory.setStackInSlot(1, itemStack.copy());
        } else if (ItemStacks.sameItemAndData(output, itemStack)) {
            output.grow(itemStack.getCount());
        }

        inventory.extractItem(0, 1, false);
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return slots_io; }
    @Override public int[] getSlotsForFace(Direction side) { return slots_io; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return slot == 0; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    public int getProgressScaled(int i) {
        return (int) (this.progress * i);
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineReactorBreeding(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(flux);
        buf.writeFloat(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        flux = buf.readInt();
        progress = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("flux", flux);
        output.putFloat("progress", progress);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        flux = input.getIntOr("flux", 0);
        progress = input.getFloatOr("progress", 0F);
    }
}
