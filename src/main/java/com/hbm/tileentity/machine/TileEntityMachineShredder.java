package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.inventory.container.ContainerMachineShredder;
import com.hbm.inventory.recipes.ShredderRecipes;
import com.hbm.items.machine.ItemBlades;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.hook.Hook;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

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

    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_OUTPUT_START = 9;
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
        if (i < SLOT_OUTPUT_START) return !(stack.getItem() instanceof ItemBlades);  // everything else shreds (to scrap)
        if (i == SLOT_BATTERY) return Library.isBattery(stack);
        return (i == SLOT_BLADE_LEFT || i == SLOT_BLADE_RIGHT) && stack.getItem() instanceof ItemBlades;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineShredder te) {
        te.setupPowerPorts(te.getPorts());
        te.updateAllPorts();
        te.receivePower();
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

    /** Shreds one item out of every occupied input slot; the whole result goes into one output slot. */
    private void processItem() {
        for (int inpSlot = SLOT_INPUT_START; inpSlot < SLOT_OUTPUT_START; inpSlot++) {
            ItemStack input = inventory.getStackInSlot(inpSlot);
            if (input.isEmpty() || !hasSpace(input)) continue;

            ItemStack output = ShredderRecipes.shred(input, level.getRandom());
            if (output.isEmpty()) {
                input.shrink(1);
                if (input.isEmpty()) inventory.setStackInSlot(inpSlot, ItemStack.EMPTY);
                continue;
            }
            boolean merged = false;
            for (int out = SLOT_OUTPUT_START; out < SLOT_BLADE_LEFT; out++) {
                ItemStack slotStack = inventory.getStackInSlot(out);
                if (!slotStack.isEmpty() && ItemStacks.sameItemAndData(slotStack, output)
                        && slotStack.getCount() + output.getCount() <= output.getMaxStackSize()) {
                    slotStack.grow(output.getCount());
                    input.shrink(1);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                for (int out = SLOT_OUTPUT_START; out < SLOT_BLADE_LEFT; out++) {
                    if (inventory.getStackInSlot(out).isEmpty()) {
                        inventory.setStackInSlot(out, output.copy());
                        input.shrink(1);
                        break;
                    }
                }
            }
            if (input.isEmpty()) inventory.setStackInSlot(inpSlot, ItemStack.EMPTY);
        }
    }

    /** Both blades must be present and not spent, and at least one input must have somewhere to go. */
    /** A listener on each input the shredder checks, in slot order. */
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

        for (int i = 0; i < SLOT_OUTPUT_START; i++) {
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

    /** Whether one output slot is empty or holds the result with room for all of it. */
    public boolean hasSpace(ItemStack stack) {
        ItemStack result = ShredderRecipes.getShredderResult(stack);
        if (result.isEmpty()) return false;
        if (result.isEmpty()) return false;
        for (int i = SLOT_OUTPUT_START; i < SLOT_BLADE_LEFT; i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (slotStack.isEmpty()) return true;
            if (ItemStacks.sameItemAndData(slotStack, result)
                    && slotStack.getCount() + result.getCount() <= result.getMaxStackSize()) return true;
        }
        return false;
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
        if ((slot >= SLOT_OUTPUT_START && slot != SLOT_BLADE_LEFT && slot != SLOT_BLADE_RIGHT) || !isItemValidForSlot(slot, stack)) return false;
        ItemStack current = inventory.getStackInSlot(slot);
        if (current.isEmpty()) return true;

        // Hoppers only top up the input slot with the smallest matching stack.
        for (int k = 0; k < 9; k++) {
            ItemStack other = inventory.getStackInSlot(k);
            if (other.isEmpty()) return false;
            if (ItemStacks.sameItemAndData(other, stack) && other.getCount() < current.getCount()) return false;
        }
        return true;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot >= SLOT_OUTPUT_START && slot <= 26) return true;                                  // outputs
        if (slot == SLOT_BLADE_LEFT || slot == SLOT_BLADE_RIGHT) {                 // spent blades only
            return stack.getMaxDamage() > 0 && stack.getDamageValue() == stack.getMaxDamage();
        }
        return slot == SLOT_BATTERY && Library.isEmptyBattery(stack);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    // IEnergyReceiverMK2

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (progress * i) / PROCESSING_SPEED; }
    public boolean hasPower() { return power > 0; }
    public boolean isProcessing() { return progress > 0; }

    // MenuProvider

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
