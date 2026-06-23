package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemStamp;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class TileEntityMachineEPress extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer {

    private static final int[] IO_SLOTS = {1, 2, 3};

    public long power = 0;
    public static final long maxPower = 50000;

    public int progress;
    public static final int maxProgress = 200;
    private boolean isRetracting = false;
    private int delay;

    @Nullable
    public ItemStack syncStack;

    public TileEntityMachineEPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_EPRESS.get(), pos, state, 5);
    }

    @Override
    public String getDefaultName() {
        return "container.epress";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineEPress te) {
        for (DirPos dp : te.getConPos()) te.trySubscribe(level, dp);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);

        boolean canProcess = te.canProcess();
        boolean hasPower = te.power >= 100;

        if ((canProcess || te.isRetracting || te.delay > 0) && hasPower) {
            te.power -= 100;

            if (te.delay <= 0) {

                int speed = 1;
                double processSpeed = (te.isRetracting ? 20 : 45) * (1.0D + speed / 4.0D);

                if (te.isRetracting) {
                    te.progress -= (int) Math.round(processSpeed);
                    if (te.progress <= 0) {
                        te.progress = 0;
                        te.isRetracting = false;
                        te.delay = 5 - speed + 1;
                    }
                } else if (canProcess) {
                    te.progress += (int) Math.round(processSpeed);
                    if (te.progress >= maxProgress) {
                        te.progress = maxProgress;
                        level.playSound(null, pos, ModSounds.PRESS_OPERATE.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
                        te.craftItem();
                        te.isRetracting = true;
                        te.delay = 5 - speed + 1;
                        te.setChanged();
                    }
                }
            } else {
                te.delay--;
            }
        } else if (te.progress > 0) {
            te.isRetracting = true;
        }

        te.networkPackNT();
    }

    private boolean canProcess() {
        ItemStack stamp = inventory.getStackInSlot(1);
        ItemStack input = inventory.getStackInSlot(2);
        if (stamp.isEmpty() || input.isEmpty()) return false;

        ItemStack output = PressRecipes.getOutput(input, stamp);
        if (output.isEmpty()) return false;

        ItemStack outSlot = inventory.getStackInSlot(3);
        if (outSlot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(outSlot, output)) return false;
        return outSlot.getCount() + output.getCount() <= outSlot.getMaxStackSize();
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
        if (stamp.isDamageableItem()) {
            stamp.setDamageValue(stamp.getDamageValue() + 1);
            if (stamp.getDamageValue() >= stamp.getMaxDamage()) inventory.setStackInSlot(1, ItemStack.EMPTY);
        }
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return switch (i) {
            case 0 -> Library.isBattery(stack);
            case 1 -> stack.getItem() instanceof ItemStamp;
            case 2 -> !(stack.getItem() instanceof ItemStamp) && !Library.isBattery(stack);

            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot == 1 || slot == 2) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 3;
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z),
                new DirPos(x, y - 1, z, Library.NEG_Y),
        };
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower / 20; }

    public long getPowerScaled(int i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (progress * i) / maxProgress; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_epress");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineEPress(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, inventory.getStackInSlot(2));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        syncStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putBoolean("isRetracting", isRetracting);
        output.putInt("delay", delay);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
        isRetracting = input.getBooleanOr("isRetracting", false);
        delay = input.getIntOr("delay", 0);
    }
}
