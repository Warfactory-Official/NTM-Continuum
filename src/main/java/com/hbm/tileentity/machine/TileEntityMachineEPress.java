package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineEPress;
import com.hbm.inventory.recipes.PressRecipes;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.items.machine.ItemStamp;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.ChatFormatting;
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
 * Electric Press — a functional powered press. It draws HE from adjacent power networks (and from a
 * battery item in slot 0), and while powered presses a stamp (slot 1) + ingredient (slot 2) into the
 * {@link PressRecipes} output (slot 3) over a stroke. Faithful port of the 1.12.2
 * {@code TileEntityMachineEPress}.
 *
 * <p>Slots: 0 battery, 1 stamp, 2 input, 3 output, 4 upgrade (SPEED only, 1.12.2 formula: faster
 * stroke + shorter inter-cycle delay). Deferred (see IMPLEMENTATION.md §18): the per-loader energy
 * capability bridge.
 */
public class TileEntityMachineEPress extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    private static final int[] IO_SLOTS = {1, 2, 3}; // stamp / input (insert) + output (extract)

    public long power = 0;
    public static final long maxPower = 50000;

    public int progress;
    /** Client-only: last tick's stroke, so the head glides between packets the way 1.7 interpolates it. */
    public int lastProgress;
    public static final int maxProgress = 200;
    private boolean isRetracting = false;
    private int delay;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    /** Client-only: the ingredient (slot 2) being pressed, for the floating-item render above the head. */
    @Nullable
    public ItemStack syncStack;

    public TileEntityMachineEPress(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_EPRESS.get(), pos, state, 5);
    }

    @Override
    public String getDefaultName() {
        return "container.epress";
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineEPress te) {
        te.lastProgress = te.progress;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineEPress te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.upgradeManager.checkSlots(te.inventory, 4, 4);

        boolean canProcess = te.canProcess();
        boolean hasPower = te.power >= 100;

        if ((canProcess || te.isRetracting || te.delay > 0) && hasPower) {
            te.power -= 100;

            if (te.delay <= 0) {
                // 1.12.2 formula: speed = 1 + SPEED level (1..4) -> stroke ×(1 + speed/4), delay 5-speed+1.
                int speed = 1 + Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3);
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
        if (!dev.golem.core.world.ItemStacks.sameItemAndData(outSlot, output)) return false;
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
            case 2 -> !(stack.getItem() instanceof ItemStamp) && !Library.isBattery(stack) && !Library.isMachineUpgrade(stack);
            case 4 -> Library.isMachineUpgrade(stack); // upgrade slot (any type, like SlotUpgrade)
            default -> false; // case 3 output
        };
    }

    // --- WorldlyContainer: hoppers extract only the output (slot 3); insert stamp/input. ---

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

    /** Power connection points: the 4 horizontal faces + the bottom of the core block. */
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

    // --- IUpgradeInfoProvider (text ported from the 1.12.2 provideInfo) ---

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{4, 4};
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, java.util.List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_epress.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (50 * level / 3) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public java.util.HashMap<UpgradeType, Integer> getValidUpgrades() {
        java.util.HashMap<UpgradeType, Integer> upgrades = new java.util.HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        return upgrades;
    }

    // --- IEnergyReceiverMK2 ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower / 20; }

    public long getPowerScaled(int i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (progress * i) / maxProgress; }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_epress");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineEPress(containerId, playerInventory, this);
    }

    // --- sync (BufPacket) + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        dev.golem.core.world.ItemStacks.writeOptional(buf, inventory.getStackInSlot(2));
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        syncStack = dev.golem.core.world.ItemStacks.readOptional(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        output.putBoolean("isRetracting", isRetracting);
        output.putInt("delay", delay);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
        isRetracting = input.getBooleanOr("isRetracting", false);
        delay = input.getIntOr("delay", 0);
    }
}
