package com.hbm.tileentity.machine.oil;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerMachineRefinery;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;

/**
 * Refinery — 1:1 port of the 1.12.2 {@code TileEntityMachineRefinery}: a 3×3×9 tower that fractionates
 * a HOT oil into four products. 13 slots (0 battery, 1→2 input container pair, 3→4/5→6/7→8/9→10 four
 * output container pairs, 11 sulfur/byproduct out, 12 fluid identifier). Tanks: {@code [0] HOTOIL 64k}
 * input + 4×24k outputs conformed to the recipe. {@link #refine} needs {@code power ≥ 5} and
 * {@code tank0 ≥ 100} and all four outputs to fit; each op drains 100 mB + 5 HE and produces the
 * fractions, and every 100 ops emits one byproduct item. maxPower 1000.
 *
 * <p>Deferred (§20): the overpressure/fire/explosion mechanic (IOverpressurable/IRepairable, nothing
 * produces pressure and ExplosionVNT/extinguishers are unported), pollution/soot, the audio loop, the
 * meta&lt;12 self-heal branch and checkTilt.
 */
public class TileEntityMachineRefinery extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final int maxSulfur = 100;
    public static final long maxPower = 1000;

    public long power = 0;
    public int sulfur = 0;
    public boolean isOn;
    private AudioWrapper audio;
    private int audioTime;
    public FluidTankNTM[] tanks;

    private static final int[] IO_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

    public TileEntityMachineRefinery(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_REFINERY.get(), pos, state, 13);
        tanks = new FluidTankNTM[5];
        tanks[0] = new FluidTankNTM(Fluids.HOTOIL, 64_000);
        tanks[1] = new FluidTankNTM(Fluids.HEAVYOIL, 24_000);
        tanks[2] = new FluidTankNTM(Fluids.NAPHTHA, 24_000);
        tanks[3] = new FluidTankNTM(Fluids.LIGHTOIL, 24_000);
        tanks[4] = new FluidTankNTM(Fluids.PETROLEUM, 24_000);
    }

    @Override
    public String getDefaultName() {
        return "container.machineRefinery";
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRefinery te) {
        if (te.isOn) te.audioTime = 20;
        if (te.audioTime > 0) te.audioTime--;
        te.audio = MachineAudio.tick(te.audio, te.audioTime > 0, ModSounds.BOILER.get(),
                SoundSource.BLOCKS, pos, te.getVolume(0.25F), 15F, 1.0F, 20);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineRefinery te) {
        te.isOn = false;

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.setTypeFromFluidId();
        te.loadInput();

        te.refine();

        te.unloadTank(1, 3, 4);
        te.unloadTank(2, 5, 6);
        te.unloadTank(3, 7, 8);
        te.unloadTank(4, 9, 10);

        for (DirPos dp : te.getConPos()) {
            for (int i = 1; i < 5; i++) {
                if (te.tanks[i].getFill() > 0) te.tryProvide(te.tanks[i], level, dp.getPos(), dp.getDir().toDirection());
            }
        }

        te.syncState();
    }


    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z)
        };
    }

    private void refine() {
        RefineryRecipes.RefineryRecipe recipe = RefineryRecipes.getRefinery(tanks[0].getTankType());
        if (recipe == null) {
            for (int i = 1; i < 5; i++) tanks[i].setTankType(Fluids.NONE);
            return;
        }

        FluidStack[] stacks = recipe.fractions();
        for (int i = 0; i < stacks.length; i++) tanks[i + 1].setTankType(stacks[i].type);

        if (power < 5 || tanks[0].getFill() < 100) return;

        for (int i = 0; i < stacks.length; i++) {
            if (tanks[i + 1].getFill() + stacks[i].fill > tanks[i + 1].getMaxFill()) return;
        }

        this.isOn = true;
        tanks[0].setFill(tanks[0].getFill() - 100);

        for (int i = 0; i < stacks.length; i++) tanks[i + 1].setFill(tanks[i + 1].getFill() + stacks[i].fill);

        this.sulfur++;

        if (this.sulfur >= maxSulfur) {
            this.sulfur -= maxSulfur;
            ItemStack out = recipe.byproduct();
            if (out != null && !out.isEmpty()) {
                ItemStack cur = inventory.getStackInSlot(11);
                if (cur.isEmpty()) {
                    inventory.setStackInSlot(11, out.copy());
                } else if (dev.golem.core.world.ItemStacks.sameItemAndData(cur, out) && cur.getCount() + out.getCount() <= cur.getMaxStackSize()) {
                    cur.grow(out.getCount());
                    inventory.setStackInSlot(11, cur);
                }
            }
            setChanged();
        }

        this.power -= 5;
    }

    /** Slot 12 fluid identifier types the input tank (old tanks[0].setType(12,...)). */
    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(12);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tanks[0].getTankType()) {
            tanks[0].setTankType(t);
            setChanged();
        }
    }

    /** Drains a fluid container in slot 1 INTO tank 0, emptied container → slot 2. */
    private void loadInput() {
        ItemStack in = inventory.getStackInSlot(1);
        if (in.isEmpty()) return;
        FluidTankNTM tank = tanks[0];

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !inventory.insertItem(2, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(1, 1, false);
            inventory.insertItem(2, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(2, empty.copy(), true).isEmpty()) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(1, 1, false);
            inventory.insertItem(2, empty, false);
            setChanged();
        }
    }

    /** Fills a container in {@code loadSlot} FROM {@code tanks[tank]}, result → {@code outSlot}. */
    private void unloadTank(int tank, int loadSlot, int outSlot) {
        FluidTankNTM t = tanks[tank];
        FluidType fluid = t.getTankType();
        if (fluid == Fluids.NONE || t.getFill() <= 0) return;
        ItemStack in = inventory.getStackInSlot(loadSlot);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            if (!fillable.acceptsFluid(fluid, in)) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int remainder = fillable.tryFill(fluid, t.getFill(), work);
            int moved = t.getFill() - remainder;
            if (moved <= 0 || !inventory.insertItem(outSlot, work.copy(), true).isEmpty()) return;
            t.setFill(t.getFill() - moved);
            inventory.extractItem(loadSlot, 1, false);
            inventory.insertItem(outSlot, work, false);
            setChanged();
        } else if (in.is(Items.BUCKET) && t.getFill() >= ItemFluidBucket.VOLUME) {
            ItemStack full = ItemFluidBucket.make(fluid);
            if (!inventory.insertItem(outSlot, full.copy(), true).isEmpty()) return;
            t.setFill(t.getFill() - ItemFluidBucket.VOLUME);
            inventory.extractItem(loadSlot, 1, false);
            inventory.insertItem(outSlot, full, false);
            setChanged();
        }
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 12) return stack.getItem() instanceof IItemFluidId;
        // The container-out slots (2/4/6/8/10) stay TRUE — the machine writes them via
        // inventory.insertItem, which validates against this method; the menu's take-only slots block
        // the player. Slot 11 (byproduct) is written via setStackInSlot (no validation) so it can stay
        // false.
        if (slot == 11) return false;
        return true;
    }

    // --- WorldlyContainer: extract only from the output/byproduct slots ---

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 2 || slot == 4 || slot == 6 || slot == 8 || slot == 10 || slot == 11;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2], tanks[3], tanks[4]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineRefinery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineRefinery(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(this.power);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
        this.isOn = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("sulfur", sulfur);
        tanks[0].writeToNBT(output, "input");
        tanks[1].writeToNBT(output, "heavy");
        tanks[2].writeToNBT(output, "naphtha");
        tanks[3].writeToNBT(output, "light");
        tanks[4].writeToNBT(output, "petroleum");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        sulfur = input.getIntOr("sulfur", 0);
        tanks[0].readFromNBT(input, "input");
        tanks[1].readFromNBT(input, "heavy");
        tanks[2].readFromNBT(input, "naphtha");
        tanks[3].readFromNBT(input, "light");
        tanks[4].readFromNBT(input, "petroleum");
    }
}
