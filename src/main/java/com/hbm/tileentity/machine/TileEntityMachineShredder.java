package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.container.ContainerMachineShredder;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.machine.ItemBlades;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

/**
 * Shredder: grinds items into dusts (and anything unrecognised into scrap; see {@link ShredderRecipes}).
 * Faithful port of the 1.12.2 {@code TileEntityMachineShredder}.
 *
 * <p><b>Slots (30):</b> 0-8 input grid, 9-26 output grid (18), 27+28 the two {@link ItemBlades blades},
 * 29 battery. Every completed cycle ({@value #PROCESSING_SPEED} ticks at {@value #CONSUMPTION} HE/t)
 * shreds <b>one item from every occupied input slot</b> and deals one damage to each blade.
 *
 * <p><b>The blades gate the machine.</b> {@link #getGearLeft()}/{@link #getGearRight()} return 0 = missing,
 * 1 = fine (&lt; half worn), 2 = worn, 3 = spent; processing needs both strictly between 0 and 3. Spent
 * blades are not destroyed; they stay in place (and only then may be extracted), exactly as before.
 */
public class TileEntityMachineShredder extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer {

    public static final long maxPower = 10_000;
    public static final int PROCESSING_SPEED = 60;
    public static final int CONSUMPTION = 5;

    public static final int SLOT_BLADE_LEFT = 27;
    public static final int SLOT_BLADE_RIGHT = 28;
    public static final int SLOT_BATTERY = 29;

    /** Every slot is exposed to automation, as in the old {@code slots_io} (0..29). */
    private static final int[] IO_SLOTS = new int[30];

    static {
        for (int i = 0; i < 30; i++) IO_SLOTS[i] = i;
    }

    public long power;
    public int progress;
    private int soundCycle = 0;

    /**
     * Blade wear states, refreshed server-side each tick and synced. The BlockEntity's inventory is not
     * replicated to clients, so the GUI reads these instead of inspecting the blade slots.
     */
    public int gearLeft;
    public int gearRight;

    public TileEntityMachineShredder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_SHREDDER.get(), pos, state, 30);
    }

    @Override
    public String getDefaultName() {
        return "container.machineShredder";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i < 9) return !(stack.getItem() instanceof ItemBlades);  // everything else shreds (to scrap)
        if (i == SLOT_BATTERY) return Library.isBattery(stack);
        return (i == SLOT_BLADE_LEFT || i == SLOT_BLADE_RIGHT) && stack.getItem() instanceof ItemBlades;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineShredder te) {
        for (DirPos con : te.getConPos()) te.trySubscribe(level, con);
        te.gearLeft = te.gearState(SLOT_BLADE_LEFT);
        te.gearRight = te.gearState(SLOT_BLADE_RIGHT);

        if (te.hasPower() && te.canProcess()) {
            te.progress++;
            te.power -= CONSUMPTION;

            if (te.progress >= PROCESSING_SPEED) {
                te.wearBlades();
                te.progress = 0;
                te.processItem();
                te.setChanged();
            }

            if (te.soundCycle == 0) {
                level.playSound(null, pos, SoundEvents.MINECART_RIDING, SoundSource.BLOCKS, 1.0F, 0.75F);
            }
            te.soundCycle++;
            if (te.soundCycle >= 50) te.soundCycle = 0;
        } else {
            te.progress = 0;
        }

        te.power = Library.chargeTEFromItems(te.inventory, SLOT_BATTERY, te.power, maxPower);
        te.networkPackNT();
    }

    /** One damage per blade per completed cycle; blades with no durability (desh) never wear. */
    private void wearBlades() {
        for (int i = SLOT_BLADE_LEFT; i <= SLOT_BLADE_RIGHT; i++) {
            ItemStack blade = inventory.getStackInSlot(i);
            if (blade.isDamageableItem()) blade.setDamageValue(blade.getDamageValue() + 1);
        }
    }

    /**
     * Shreds one item out of every occupied input slot into the output grid. Ported 1:1 from the old
     * {@code processItem}: merge into matching stacks first, then fill empty output slots.
     */
    private void processItem() {
        for (int inpSlot = 0; inpSlot < 9; inpSlot++) {
            ItemStack input = inventory.getStackInSlot(inpSlot);
            if (input.isEmpty() || !hasSpace(input)) continue;

            ItemStack output = ShredderRecipes.getShredderResult(input);
            int itemsLeft = output.getCount();

            for (int out = 9; out < 27 && itemsLeft > 0; out++) {
                ItemStack slotStack = inventory.getStackInSlot(out);
                if (!slotStack.isEmpty() && ItemStacks.sameItemAndData(slotStack, output)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    if (space > 0) {
                        int amount = Math.min(itemsLeft, space);
                        slotStack.grow(amount);
                        itemsLeft -= amount;
                    }
                }
            }
            for (int out = 9; out < 27 && itemsLeft > 0; out++) {
                if (inventory.getStackInSlot(out).isEmpty()) {
                    int amount = Math.min(itemsLeft, output.getMaxStackSize());
                    ItemStack newStack = output.copy();
                    newStack.setCount(amount);
                    inventory.setStackInSlot(out, newStack);
                    itemsLeft -= amount;
                }
            }

            input.shrink(1);
            if (input.isEmpty()) inventory.setStackInSlot(inpSlot, ItemStack.EMPTY);
        }
    }

    /** Both blades must be present and not spent, and at least one input must have somewhere to go. */
    /** A listener on each input the shredder checks, in slot order (ADDON-API-INVENTORY H-70). */
    @FunctionalInterface
    public interface Intercept {
        /**
         * Called for each non-empty input slot while the blades work, before the slot is considered; true stops the
         * check (no shredding this tick; the listener has done what it does, e.g. blown the machine up).
         */
        boolean intercept(TileEntityMachineShredder shredder, int slot, ItemStack stack);
    }

    /** Every listener in order; HBM registers none. NTM: Space's Gwen bobblehead blows the shredder up. */
    public static final Hook<Intercept> INTERCEPT = Hook.notifying();

    public boolean canProcess() {
        int left = gearState(SLOT_BLADE_LEFT), right = gearState(SLOT_BLADE_RIGHT);
        if (left <= 0 || left >= 3 || right <= 0 || right >= 3) return false;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                for (Intercept listener : INTERCEPT) {
                    if (listener.intercept(this, i, stack)) return false;
                }
            }
            if (!stack.isEmpty() && hasSpace(stack)) return true;
        }
        return false;
    }

    /** Whether the output grid can absorb a full result stack for {@code stack}. */
    public boolean hasSpace(ItemStack stack) {
        ItemStack result = ShredderRecipes.getShredderResult(stack);
        if (result.isEmpty()) return false;

        int spaceLeft = 0;
        for (int i = 9; i < 27; i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) {
                spaceLeft += result.getMaxStackSize();
            } else if (ItemStacks.sameItemAndData(slotStack, result)) {
                spaceLeft += slotStack.getMaxStackSize() - slotStack.getCount();
            }
        }
        return spaceLeft >= result.getCount();
    }

    /** 0 = missing, 1 = good, 2 = worn past halfway, 3 = spent. Durability-less blades are always 1. */
    private int gearState(int slot) {
        ItemStack blade = inventory.getStackInSlot(slot);
        if (blade.isEmpty() || !(blade.getItem() instanceof ItemBlades)) return 0;
        if (blade.getMaxDamage() == 0) return 1;   // desh: unbreakable
        if (blade.getDamageValue() < blade.getMaxDamage() / 2) return 1;
        if (blade.getDamageValue() != blade.getMaxDamage()) return 2;
        return 3;
    }

    /** The synced wear state (valid on both sides); server logic uses {@link #gearState} directly. */
    public int getGearLeft() { return gearLeft; }
    public int getGearRight() { return gearRight; }

    /** True when either blade is missing or spent: the GUI's error indicator. */
    public boolean bladesBroken() {
        return gearLeft == 0 || gearLeft == 3 || gearRight == 0 || gearRight == 3;
    }

    // WorldlyContainer: insert into the input grid / blades / battery; extract outputs, spent blades,
    // and drained batteries. Mirrors the old canInsertItem/canExtractItem. ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot != SLOT_BLADE_LEFT && slot != SLOT_BLADE_RIGHT && stack.getItem() instanceof ItemBlades) return false;
        if (slot != SLOT_BATTERY && Library.isDischargeableBattery(stack)) return false;
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot >= 9 && slot <= 26) return true;                                  // outputs
        if (slot == SLOT_BLADE_LEFT || slot == SLOT_BLADE_RIGHT) {                 // spent blades only
            return stack.getMaxDamage() > 0 && stack.getDamageValue() == stack.getMaxDamage();
        }
        return slot == SLOT_BATTERY && Library.isEmptyBattery(stack);
    }

    /** Power connection points: all six faces. */
    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z),
                new DirPos(x, y + 1, z, Library.POS_Y),
                new DirPos(x, y - 1, z, Library.NEG_Y),
        };
    }

    // IEnergyReceiverMK2

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (progress * i) / PROCESSING_SPEED; }
    public boolean hasPower() { return power >= CONSUMPTION; }
    public boolean isProcessing() { return progress > 0; }

    // MenuProvider

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_shredder");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineShredder(containerId, playerInventory, this);
    }

    // Sync (BufPacket) + persistence

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeByte(gearLeft);
        buf.writeByte(gearRight);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        gearLeft = buf.readByte();
        gearRight = buf.readByte();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
    }
}
