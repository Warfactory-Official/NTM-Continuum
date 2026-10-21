package com.hbm.tileentity.machine;

import com.hbm.inventory.container.ContainerMachineAmmoPress;
import com.hbm.inventory.recipes.AmmoPressRecipes;
import com.hbm.inventory.recipes.AmmoPressRecipes.AmmoPressRecipe;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.SavedIds;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import dev.golem.core.world.ItemStacks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
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

/** The old Ammo Press: a selected nine-slot recipe is consumed immediately, batch by batch. */
public class TileEntityMachineAmmoPress extends TileEntityMachineBase
        implements IControlReceiver, MenuProvider, WorldlyContainer {

    private static final int[] ACCESS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};

    /** The selected recipe's id ({@code hbm:ammo_press/m357_bp}), empty for none; saved and synced by id, never by index. */
    public String selectedRecipe = "";
    public AnimationState animState = AnimationState.LIFTING;
    public int playAnimation;
    public float prevLift;
    public float lift;
    public float prevPress;
    public float press;

    public enum AnimationState {
        LIFTING,
        PRESSING,
        RETRACTING,
        LOWERING
    }

    public TileEntityMachineAmmoPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_AMMO_PRESS.get(), pos, state, 10);
    }

    @Override
    public String getDefaultName() {
        return "container.machineAmmoPress";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAmmoPress press) {
        if (press.playAnimation > 0) press.playAnimation--;
        press.performRecipe();
        press.networkPackNT();
    }

    /** Client-only animation state, matching the old remote branch and its 40-tick trigger. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAmmoPress press) {
        press.prevLift = press.lift;
        press.prevPress = press.press;

        if (press.playAnimation > 0 || press.lift > 0F) {
            switch (press.animState) {
                case LIFTING -> {
                    press.lift += 1F / 40F;
                    if (press.lift >= 1F) {
                        press.lift = 1F;
                        press.animState = AnimationState.PRESSING;
                    }
                }
                case PRESSING -> {
                    press.press += 1F / 20F;
                    if (press.press >= 1F) {
                        press.press = 1F;
                        press.animState = AnimationState.RETRACTING;
                    }
                }
                case RETRACTING -> {
                    press.press -= 1F / 20F;
                    if (press.press <= 0F) {
                        press.press = 0F;
                        press.animState = AnimationState.LOWERING;
                    }
                }
                case LOWERING -> {
                    press.lift -= 1F / 40F;
                    if (press.lift <= 0F) {
                        press.lift = 0F;
                        press.animState = AnimationState.LIFTING;
                    }
                }
            }
        }
    }

    /** The selected recipe, or null (none selected, or a reload removed it). */
    public @org.jspecify.annotations.Nullable AmmoPressRecipe selected() {
        return AmmoPressRecipes.byId(selectedRecipe);
    }

    public void performRecipe() {
        // The legacy method recursed to process every available batch in one update. A loop has the
        // same observable result without making a malformed external recipe able to overflow the stack.
        while (true) {
            AmmoPressRecipe recipe = selected();
            if (recipe == null) return;
            ItemStack output = inventory.getStackInSlot(9);
            if (!output.isEmpty() && (!ItemStacks.sameItemAndData(output, recipe.output)
                    || output.getCount() + recipe.output.getCount() > output.getMaxStackSize())) return;
            if (!hasIngredients(recipe)) return;
            produceAmmo(recipe);
        }
    }

    public boolean hasIngredients(AmmoPressRecipe recipe) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (recipe.input[i] == null && stack.isEmpty()) continue;
            if (recipe.input[i] == null || stack.isEmpty()) return false;
            if (!recipe.input[i].matchesRecipe(stack, false)) return false;
        }
        return true;
    }

    private void produceAmmo(AmmoPressRecipe recipe) {
        for (int i = 0; i < 9; i++) {
            if (recipe.input[i] != null) inventory.getStackInSlot(i).shrink(recipe.input[i].stacksize);
        }

        ItemStack output = inventory.getStackInSlot(9);
        if (output.isEmpty()) inventory.setStackInSlot(9, recipe.output.copy());
        else output.grow(recipe.output.getCount());
        playAnimation = 40;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return ACCESS;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        AmmoPressRecipe recipe = slot > 8 ? null : selected();
        if (recipe == null) return false;
        return recipe.input[slot] != null && recipe.input[slot].matchesRecipe(stack, true);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ACCESS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot < 9 && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 9;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineAmmoPress(containerId, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerMachineAmmoPress menu
                && menu.getPress() == this && menu.stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("selection")) return;
        String newRecipe = Tags.getStringOr(data, "selection", "");
        selectedRecipe = newRecipe.equals(selectedRecipe) ? "" : newRecipe;
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeUtf(selectedRecipe);
        buf.writeInt(playAnimation);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        selectedRecipe = buf.readUtf();
        playAnimation = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        // the chosen recipe by its recipe id, never its list index
        output.putString("recipe", selectedRecipe);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        String saved = input.getStringOr("recipe", null);
        if (saved == null) {
            if (input.getIntOr("recipe", Integer.MIN_VALUE) != Integer.MIN_VALUE) SavedIds.warnNumeric("recipe index", "recipe");
            saved = "";
        }
        if (AmmoPressRecipes.byId(saved) != null || !saved.contains("#")) {
            selectedRecipe = saved;
        } else {
            // a save from before the recipes were data names the recipe by output and occurrence ("hbm:...x16#0")
            int index = SavedIds.fromListKey(signatures(), saved);
            selectedRecipe = index < 0 ? "" : AmmoPressRecipes.recipes.get(index).id;
        }
    }

    private static List<String> signatures() {
        List<String> keys = new ArrayList<>();
        for (AmmoPressRecipes.AmmoPressRecipe recipe : AmmoPressRecipes.recipes) {
            keys.add(BuiltInRegistries.ITEM.getKey(recipe.output.getItem()) + "x" + recipe.output.getCount());
        }
        return keys;
    }
}
