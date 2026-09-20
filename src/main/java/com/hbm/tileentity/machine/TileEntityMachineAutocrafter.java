package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.lib.Library;
import com.hbm.modules.ModulePatternMatcher;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import com.hbm.blocks.machine.MachineAutocrafter;
import com.hbm.inventory.container.ContainerAutocrafter;
import com.hbm.tileentity.ModProductionBlockEntities;

/**
 * The automatic crafting table, ported from the original {@code TileEntityMachineAutocrafter}.
 *
 * <p>Slots 0..8 are the pattern ghosts, 9 the template output, 10..18 the live recipe grid, 19 the
 * output, 20 the battery. The template grid selects the list of matching vanilla crafting recipes
 * (cycled with a right-click on the template output); each tick the chosen recipe is tried against the
 * live grid and, when it matches and the output fits, one craft is taken for {@link #consumption} HE.
 * Recipe lookups go through Golem's {@code Recipes.craftingMatches} seam, which hides the recipe-manager
 * and recipe-input types; the old {@code IControlReceiverFilter} ghost-drop packet is not carried, the
 * menu's click path writes the patterns instead.
 */
public class TileEntityMachineAutocrafter extends TileEntityMachineBase
        implements IEnergyReceiverMK2, MenuProvider, WorldlyContainer {

    public static final int consumption = 100;
    public static final long maxPower = consumption * 100L;
    private static final int[] ACCESS = {10, 11, 12, 13, 14, 15, 16, 17, 18, 19};

    public long power;
    public List<Recipes.CraftingMatch> recipes = new ArrayList<>();
    public int recipeIndex;
    public int recipeCount;
    /** The template grid is re-matched on the first server tick after loading: no level exists at load time. */
    private boolean templateDirty = true;

    public final ModulePatternMatcher matcher = new ModulePatternMatcher(9);

    public TileEntityMachineAutocrafter(BlockPos pos, BlockState state) {
        super(ModProductionBlockEntities.AUTOCRAFTER.get(), pos, state, 21);
    }

    @Override
    public String getDefaultName() {
        return "container.autocrafter";
    }

    public void nextMode(int i) {
        if (level != null) this.matcher.nextMode(level, inventory.getStackInSlot(i), i);
    }

    public void nextTemplate() {
        if (level == null || level.isClientSide()) return;

        this.recipeIndex++;
        if (this.recipeIndex >= this.recipes.size()) this.recipeIndex = 0;

        refreshTemplateOutput();
    }

    private void refreshTemplateOutput() {
        if (level == null) return;
        if (!this.recipes.isEmpty() && recipeIndex < recipes.size()) {
            inventory.setStackInSlot(9, Recipes.craftingResult(level, recipes.get(recipeIndex), 3, 3, grid(0)).orElse(ItemStack.EMPTY));
        } else {
            inventory.setStackInSlot(9, ItemStack.EMPTY);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAutocrafter te) {
        if (te.templateDirty) {
            te.templateDirty = false;
            int keep = te.recipeIndex;
            te.updateTemplateGrid();
            if (keep < te.recipes.size()) {
                te.recipeIndex = keep;
                te.refreshTemplateOutput();
            }
        }

        te.power = Library.chargeTEFromItems(te.inventory, 20, te.power, maxPower);

        if (!te.recipes.isEmpty() && te.power >= consumption && te.recipeIndex < te.recipes.size()) {
            Recipes.CraftingMatch recipe = te.recipes.get(te.recipeIndex);
            List<ItemStack> grid = te.grid(10);
            ItemStack stack = Recipes.craftingResult(level, recipe, 3, 3, grid).orElse(ItemStack.EMPTY);

            if (!stack.isEmpty()) {
                boolean didCraft = false;
                ItemStack out = te.inventory.getStackInSlot(19);

                if (out.isEmpty()) {
                    te.inventory.setStackInSlot(19, stack.copy());
                    didCraft = true;
                } else if (ItemStacks.sameItemAndData(out, stack) && out.getCount() + stack.getCount() <= out.getMaxStackSize()) {
                    out.grow(stack.getCount());
                    didCraft = true;
                }

                if (didCraft) {
                    for (int i = 10; i < 19; i++) {
                        ItemStack ingredient = te.inventory.getStackInSlot(i);
                        if (ingredient.isEmpty()) continue;

                        ItemStack remainder = Compat.craftingRemainder(ingredient);
                        te.inventory.extractItem(i, 1, false);

                        if (te.inventory.getStackInSlot(i).isEmpty() && !remainder.isEmpty()) {
                            if (remainder.isDamageableItem() && remainder.getDamageValue() > remainder.getMaxDamage()) continue;
                            te.inventory.setStackInSlot(i, remainder);
                        }
                    }

                    te.power -= consumption;
                    te.setChanged();
                }
            }
        }

        te.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        matcher.serialize(buf);
        buf.writeInt(recipeCount);
        buf.writeInt(recipeIndex);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        matcher.deserialize(buf);
        recipeCount = buf.readInt();
        recipeIndex = buf.readInt();
    }

    public void updateTemplateGrid() {
        if (level == null) return;
        this.recipes = Recipes.craftingMatches(level, 3, 3, grid(0));
        this.recipeCount = recipes.size();
        this.recipeIndex = 0;
        refreshTemplateOutput();
        setChanged();
    }

    private List<ItemStack> grid(int start) {
        List<ItemStack> grid = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) grid.add(inventory.getStackInSlot(start + i));
        return grid;
    }

    @Override public int[] getAccessibleSlotsFromSide(int side) { return ACCESS; }
    @Override public int[] getSlotsForFace(Direction side) { return ACCESS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == 19) return true;

        if (slot > 9 && slot < 19) {
            ItemStack filter = inventory.getStackInSlot(slot - 10);
            if (filter.isEmpty() || matcher.modes[slot - 10] == null || matcher.modes[slot - 10].isEmpty()) return true;
            return !matcher.isValidForFilter(filter, slot - 10, stack);
        }

        return false;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // automatically prohibit stacked container items
        if (stack.getCount() > 1 && !Compat.craftingRemainder(stack).isEmpty()) return false;

        // only allow insertion for the nine recipe slots
        if (slot < 10 || slot > 18) return false;

        // is the filter at this space null? no input.
        if (inventory.getStackInSlot(slot - 10).isEmpty()) return false;

        // do not permit total stacking beyond 4 items
        ItemStack present = inventory.getStackInSlot(slot);
        if (!present.isEmpty() && present.getCount() + stack.getCount() > 4) return false;
        if (stack.getCount() > 4) return false;

        // let's find all slots that this item could potentially go in
        List<Integer> validSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack filter = inventory.getStackInSlot(i);
            if (filter.isEmpty() || matcher.modes[i] == null || matcher.modes[i].isEmpty()) continue;

            if (matcher.isValidForFilter(filter, i, stack)) {
                validSlots.add(i + 10);

                // if the current slot is valid and has no item in it, shortcut to true [*]
                if (i + 10 == slot && present.isEmpty()) return true;
            }
        }

        // if the slot we are looking at isn't valid, skip
        if (!validSlots.contains(slot)) return false;

        // assumption from [*]: the slot has to be valid by now, and it cannot be null
        int size = present.getCount();

        // now we decide based on stacksize, woohoo
        for (Integer i : validSlots) {
            ItemStack valid = inventory.getStackInSlot(i);

            if (valid.isEmpty()) return false; // empty? since this slot is not empty by now, this other slot needs the item more
            if (!ItemStack.isSameItem(valid, stack)) continue; // different item anyway? out with it

            // if there is another slot that actually does need the same item more, cancel
            if (valid.getCount() < size) return false;
        }

        // prevent items with containers from stacking
        if (!Compat.craftingRemainder(stack).isEmpty()) return false;

        // by now, we either already have filled the slot (if valid by filter and null) or weeded out all other options, which means it is good to go
        return true;
    }

    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public void setPower(long power) { this.power = power; }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.power = input.getLongOr("power", 0L);
        matcher.load(input);
        this.recipeIndex = input.getIntOr("rec", 0);
        this.templateDirty = true;
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        matcher.save(output);
        output.putInt("rec", this.recipeIndex);
    }

    @Override public Component getDisplayName() { return Component.translatable(getName()); }
    @Override public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerAutocrafter(containerId, playerInventory, this);
    }

    /** 1.7 {@code hasPermission}: within 20 blocks. */
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()) < 20 * 20;
    }

    /** 1.7 {@code MachineAutocrafter.breakBlock}: only slots 10 and up spill; the recipe grid and output do not. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level == null) return;
        for (int i = 10; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) continue;
            net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
            setItem(i, ItemStack.EMPTY);
        }
    }
}
