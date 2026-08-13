package com.hbm.inventory.container;

import dev.golem.core.world.Compat;
import com.hbm.blocks.machine.NTMAnvil;
import com.hbm.inventory.ModMenus;
import com.hbm.inventory.recipes.anvil.AnvilRecipes;
import com.hbm.inventory.recipes.anvil.AnvilSmithingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Menu for the {@link NTMAnvil}: two smithing input slots + a smithing output (matched live against
 * {@link AnvilRecipes#getSmithing()}), plus the player inventory. The construction-recipe browser lives
 * entirely in the client screen ({@code GUIAnvil}) and crafts via {@link com.hbm.packet.AnvilCraftPayload}
 * straight from the player's inventory — no container slots involved.
 *
 * <p>The anvil has no BlockEntity; the {@link #tier} is re-derived from the block state at {@code pos} on
 * BOTH sides (the old container carried it from the client "because we can't trust these rascals with
 * their packets" — here the server reads its own world, which is stronger).
 */
public class ContainerAnvil extends AbstractContainerMenu {

    /** 0-1 = smithing inputs, 2 = smithing output, then the 36 player slots. */
    public static final int MACHINE_SLOTS = 3;

    public final int tier;
    private final SimpleContainer input = new SimpleContainer(2);
    private final ResultContainer output = new ResultContainer();
    private final ContainerLevelAccess access;

    public ContainerAnvil(int containerId, Inventory playerInventory, BlockPos pos) {
        super(ModMenus.ANVIL.get(), containerId);
        this.access = ContainerLevelAccess.create(playerInventory.player.level(), pos);
        this.tier = playerInventory.player.level().getBlockState(pos).getBlock() instanceof NTMAnvil anvil
                ? anvil.tier : NTMAnvil.TIER_IRON;

        this.addSlot(new SmithingSlot(input, 0, 17, 27));
        this.addSlot(new SmithingSlot(input, 1, 53, 27));
        this.addSlot(new Slot(output, 0, 89, 27) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                ItemStack left = ContainerAnvil.this.input.getItem(0);
                ItemStack right = ContainerAnvil.this.input.getItem(1);

                if (!left.isEmpty() && !right.isEmpty()) {
                    for (AnvilSmithingRecipe rec : AnvilRecipes.getSmithing()) {
                        int i = rec.matchesInt(left, right);
                        if (i != -1 && rec.tier <= ContainerAnvil.this.tier) {
                            ContainerAnvil.this.input.removeItem(0, rec.amountConsumed(0, i == 1));
                            ContainerAnvil.this.input.removeItem(1, rec.amountConsumed(1, i == 1));
                            break;
                        }
                    }
                }
                ContainerAnvil.this.updateSmithing();
                super.onTake(player, stack);
            }
        });

        //? if >=26.1 {
        this.addStandardInventorySlots(playerInventory, 8, 140);
        //?} else {
        /*Compat.addPlayerInventory(this::addSlot, playerInventory, 8, 140);
        *///?}

        updateSmithing();
    }

    private class SmithingSlot extends Slot {

        SmithingSlot(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public void setChanged() {
            super.setChanged();
            ContainerAnvil.this.updateSmithing();
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            ContainerAnvil.this.updateSmithing();
            super.onTake(player, stack);
        }
    }

    private void updateSmithing() {

        ItemStack left = this.input.getItem(0);
        ItemStack right = this.input.getItem(1);

        if (left.isEmpty() || right.isEmpty()) {
            this.output.setItem(0, ItemStack.EMPTY);
            return;
        }

        for (AnvilSmithingRecipe rec : AnvilRecipes.getSmithing()) {
            if (rec.matches(left, right) && rec.tier <= this.tier) {
                this.output.setItem(0, rec.getOutput(left, right));
                return;
            }
        }

        this.output.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public boolean stillValid(Player player) {
        // Any of the 12 anvil blocks keeps the menu open (stillValid(access, player, block) is per-block).
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof NTMAnvil
                && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0, true);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.input));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int invStart = MACHINE_SLOTS;
            int invEnd = MACHINE_SLOTS + 36;
            if (index == 2) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else if (index < MACHINE_SLOTS) {
                if (!this.moveItemStackTo(stack, invStart, invEnd, true)) return ItemStack.EMPTY;
            } else {
                if (!this.moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (index == 2) slot.onTake(player, stack);
        }
        return result;
    }
}
