package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import dev.golem.core.world.Compat;
import com.hbm.inventory.container.ContainerMachinePress;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
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
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import org.jspecify.annotations.Nullable;

/**
 * Burner Press — a functional fuel-driven press. It burns furnace fuel (slot 0) to build up
 * {@code burnTime}; while it has fuel and a valid stamp (slot 1) + ingredient (slot 2) it ramps a
 * {@code speed} gauge and drives the stamp head up/down ({@code progress}), producing the
 * {@link PressRecipes} output into slot 3 each full stroke. Faithful port of the 1.12.2
 * {@code TileEntityMachinePress}.
 *
 * <p>Slots: 0 fuel, 1 stamp, 2 input, 3 output, 4–12 storage buffer.
 *
 * <p>The {@code press_preheater} block (speeds up the stroke) lives in {@code com.hbm.production}; the
 * press-operate sound remains deferred (see IMPLEMENTATION.md §18).
 */
public class TileEntityMachinePress extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    private static final int[] SLOTS_DOWN = {3};            // output only (extract from below)
    private static final int[] SLOTS_SIDE = {0, 1, 2};      // fuel / stamp / input (insert from the sides)

    public int speed = 0;
    public static final int maxSpeed = 400;
    public static final int progressAtMax = 25;
    public int burnTime = 0;

    public int progress;
    /** Client-only: last tick's stroke, so the head glides between packets the way 1.7 interpolates it. */
    public int lastProgress;
    public static final int maxProgress = 200;
    private boolean isRetracting = false;
    private int delay;

    /** Client-only: the ingredient (slot 2) being pressed, for the floating-item render above the head. */
    @Nullable
    public ItemStack syncStack;

    public TileEntityMachinePress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PRESS.get(), pos, state, 13);
    }

    @Override
    public String getDefaultName() {
        return "container.press";
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePress te) {
        te.lastProgress = te.progress;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePress te) {
        // 1.7: any of the six neighbours being a burner press preheater quadruples the spool-up.
        boolean preheated = false;
        for (com.hbm.lib.ForgeDirection dir : com.hbm.lib.ForgeDirection.VALID_DIRECTIONS) {
            if (level.getBlockState(pos.offset(dir.offsetX, dir.offsetY, dir.offsetZ))
                    .is(com.hbm.blocks.ModProductionBlocks.PRESS_PREHEATER.get())) {
                preheated = true;
                break;
            }
        }

        boolean canProcess = te.canProcess();
        if ((canProcess || te.isRetracting) && te.burnTime >= 200) {
            te.speed += preheated ? 4 : 1;
            if (te.speed > maxSpeed) te.speed = maxSpeed;
        } else {
            te.speed -= 1;
            if (te.speed < 0) te.speed = 0;
        }

        if (te.delay <= 0) {
            int stampSpeed = te.speed * progressAtMax / maxSpeed;

            if (te.isRetracting) {
                te.progress -= stampSpeed;
                if (te.progress <= 0) {
                    te.progress = 0;
                    te.isRetracting = false;
                    te.delay = 5;
                }
            } else if (canProcess) {
                te.progress += stampSpeed;
                if (te.progress >= maxProgress) {
                    te.progress = maxProgress;
                    level.playSound(null, pos, ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                    te.craftItem();
                    te.isRetracting = true;
                    te.delay = 5;
                    if (te.burnTime >= 200) te.burnTime -= 200;
                    te.setChanged();
                }
            }
        } else {
            te.delay--;
        }

        if (!canProcess && !te.isRetracting && te.progress > 0) {
            te.isRetracting = true;
        }

        // Consume fuel into burnTime when below the threshold.
        ItemStack fuel = te.inventory.getStackInSlot(0);
        if (!fuel.isEmpty() && te.burnTime < 200 && Compat.burnDuration(level, fuel) > 0) {
            te.burnTime += Compat.burnDuration(level, fuel);
            ItemStack remainder = Compat.craftingRemainder(fuel); // e.g. lava bucket -> empty bucket
            fuel.shrink(1);
            if (fuel.isEmpty()) {
                te.inventory.setStackInSlot(0, remainder);
            }
            te.setChanged();
        }

        te.syncState();
    }

    private void craftItem() {
        ItemStack input = inventory.getStackInSlot(2);
        ItemStack stamp = inventory.getStackInSlot(1);
        ItemStack output = PressRecipes.getOutput(input, stamp);
        if (output.isEmpty()) return;

        ItemStack outSlot = inventory.getStackInSlot(3);
        if (outSlot.isEmpty()) inventory.setStackInSlot(3, output.copy());
        else outSlot.grow(output.getCount());

        input.shrink(1);
        damageStamp(stamp);
    }

    /** Damages a breakable stamp by one; clears the slot when it wears out (matching the original). */
    private void damageStamp(ItemStack stamp) {
        if (!stamp.isDamageableItem()) return;
        stamp.setDamageValue(stamp.getDamageValue() + 1);
        if (stamp.getDamageValue() >= stamp.getMaxDamage()) {
            inventory.setStackInSlot(1, ItemStack.EMPTY);
        }
    }

    public boolean canProcess() {
        if (burnTime < 200) return false;
        ItemStack stamp = inventory.getStackInSlot(1);
        ItemStack input = inventory.getStackInSlot(2);
        if (stamp.isEmpty() || input.isEmpty()) return false;

        ItemStack output = PressRecipes.getOutput(input, stamp);
        if (output.isEmpty()) return false;

        ItemStack outSlot = inventory.getStackInSlot(3);
        if (outSlot.isEmpty()) return true;
        if (!dev.golem.core.world.ItemStacks.sameItemAndData(outSlot, output)) return false;
        return outSlot.getCount() + output.getCount() <= outSlot.getMaxStackSize();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        Level level = this.level;
        return switch (i) {
            case 0 -> level != null && Compat.burnDuration(level, stack) > 0;
            case 1 -> stack.getItem() instanceof ItemStamp;
            case 2 -> !(stack.getItem() instanceof ItemStamp)
                    && (level == null || Compat.burnDuration(level, stack) <= 0);
            case 3 -> false;
            default -> i >= 4 && i <= 12;
        };
    }

    public int getProgressScaled(int i) {
        return (progress * i) / maxProgress;
    }

    // --- WorldlyContainer: hoppers extract only the output (slot 3) from below; insert fuel/stamp/input
    // from the sides. Without this a hopper under a plain Container BE pulls from every slot. ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? SLOTS_DOWN : SLOTS_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 0 && slot <= 2 && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 3;
    }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachinePress(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(speed);
        buf.writeInt(burnTime);
        buf.writeInt(progress);
        dev.golem.core.world.ItemStacks.writeOptional(buf, inventory.getStackInSlot(2));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        speed = buf.readInt();
        burnTime = buf.readInt();
        progress = buf.readInt();
        syncStack = dev.golem.core.world.ItemStacks.readOptional(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("progress", progress);
        output.putInt("burnTime", burnTime);
        output.putInt("speed", speed);
        output.putBoolean("isRetracting", isRetracting);
        output.putInt("delay", delay);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        progress = input.getIntOr("progress", 0);
        burnTime = input.getIntOr("burnTime", 0);
        speed = input.getIntOr("speed", 0);
        isRetracting = input.getBooleanOr("isRetracting", false);
        delay = input.getIntOr("delay", 0);
    }
}
