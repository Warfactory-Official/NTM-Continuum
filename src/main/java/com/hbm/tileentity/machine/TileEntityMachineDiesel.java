package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.util.TickPhase;
import com.hbm.inventory.container.ContainerMachineDiesel;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachinePolluting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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
 * Diesel Generator — 1:1 port of the 1.12.2 {@code TileEntityMachineDiesel} and the port's <b>first
 * real {@link IEnergyProviderMK2} machine generator</b>. Burns any {@code FT_Combustible} fluid at
 * 1 mB/tick into HE and pushes it to the 6 neighbours. Power per tick = {@code combustionEnergy/1000 ·
 * gradeEfficiency}, grade efficiency MEDIUM 0.5 / HIGH 0.75 / AERO 0.1 (LOW refuses); powerCap is
 * maxPower (50k) or ×10 (500k) when the fuel is NITAN. Redstone signal switches it off. 5 slots
 * (0→1 fuel container drain, 2 battery-to-charge, 3→4 fluid identifier + output).
 *
 * <p>Every fifth burning tick the fuel's {@code FT_Polluting} burn table goes into the 100 mB smoke tanks,
 * which it offers to all six neighbours with its power.
 */
public class TileEntityMachineDiesel extends TileEntityMachinePolluting
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardTransceiverMK2 {

    public long power;
    public static final long maxPower = 50000;
    public long powerCap = 50000;
    public FluidTankNTM tank;
    public boolean wasOn = false;
    private AudioWrapper audio;

    /** Grade efficiency table (old {@code fuelEfficiency}); LOW/GAS absent → not burnable here. */
    private static double efficiency(FluidCombustion.FuelGrade grade) {
        return switch (grade) {
            case MEDIUM -> 0.5D;
            case HIGH -> 0.75D;
            case AERO -> 0.1D;
            default -> 0D;
        };
    }

    public TileEntityMachineDiesel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_DIESEL.get(), pos, state, 5, 100);
        tank = new FluidTankNTM(Fluids.DIESEL, 4_000);
    }

    @Override
    public String getDefaultName() {
        return "container.machineDiesel";
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDiesel te) {
        te.audio = MachineAudio.tick(te.audio, te.wasOn, com.hbm.sound.ModProductionSounds.ENGINE.get(), SoundSource.BLOCKS,
                pos, te.getVolume(1F), 10F, 1.0F, 10);
    }

    public long getPowerScaled(long i) {
        return powerCap <= 0 ? 0 : (power * i) / powerCap;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineDiesel te) {
        te.wasOn = false;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            te.tryProvide(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
            te.sendSmoke(pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }

        te.setTypeFromFluidId();
        te.loadTank();

        te.powerCap = te.tank.getTankType() == Fluids.NITAN ? maxPower * 10 : maxPower;

        te.power = Library.chargeItemsFromTE(te.inventory, 2, te.power, te.powerCap);
        te.generate(level);
        te.networkPackNT();
    }

    public boolean hasAcceptableFuel() {
        return getHEFromFuel() > 0;
    }

    public long getHEFromFuel() {
        return getHEFromFuel(tank.getTankType());
    }

    public static long getHEFromFuel(FluidType type) {
        FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(type);
        if (fuel != null && fuel.grade() != FluidCombustion.FuelGrade.LOW) {
            return (long) ((double) fuel.energyPerBucket() / 1000L * efficiency(fuel.grade()));
        }
        return 0;
    }

    private void generate(Level level) {
        if (level.hasNeighborSignal(worldPosition)) return;
        if (hasAcceptableFuel() && tank.getFill() > 0) {
            this.wasOn = true;
            tank.setFill(Math.max(tank.getFill() - 1, 0));

            if (TickPhase.every(level, worldPosition, 5L)) {
                super.pollute(tank.getTankType(), FluidReleaseType.BURN, 5F);
            }

            long he = getHEFromFuel();
            power = Math.min(power + he, powerCap);
        }
    }

    /** Drains a fluid container in slot 0 into the tank, emptied container → slot 1. */
    private void loadTank() {
        ItemStack in = inventory.getStackInSlot(0);
        if (in.isEmpty()) return;

        if (in.getItem() instanceof IFillableItem fillable) {
            int held = fillable.getFill(in);
            FluidType heldType = fillable.getFirstFluidType(in);
            if (held <= 0 || heldType == Fluids.NONE) return;
            int addable = tank.fill(heldType, held, false);
            if (addable <= 0) return;
            ItemStack work = in.copy();
            work.setCount(1);
            int taken = fillable.tryEmpty(heldType, addable, work);
            if (taken <= 0 || !inventory.insertItem(1, work.copy(), true).isEmpty()) return;
            tank.fill(heldType, taken, true);
            inventory.extractItem(0, 1, false);
            inventory.insertItem(1, work, false);
            setChanged();
        } else if (in.getItem() instanceof ItemFluidBucket) {
            FluidType fluid = ItemFluidBucket.getFluid(in);
            if (fluid == Fluids.NONE) return;
            if (tank.fill(fluid, ItemFluidBucket.VOLUME, false) < ItemFluidBucket.VOLUME) return;
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (!inventory.insertItem(1, empty.copy(), true).isEmpty()) return;
            tank.fill(fluid, ItemFluidBucket.VOLUME, true);
            inventory.extractItem(0, 1, false);
            inventory.insertItem(1, empty, false);
            setChanged();
        }
    }

    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(3);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tank.getTankType() && tank.getFill() == 0) {
            tank.setTankType(t);
            setChanged();
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 2) return Library.isBattery(stack);
        if (slot == 3) return stack.getItem() instanceof IItemFluidId;
        // Slot 1 (emptied-container out) must stay TRUE: the machine writes it via inventory.insertItem,
        // which validates against this method — false here silently kills loadTank (the chemplant
        // convention: the MENU's take-only slot blocks the player, not the BE validity).
        if (slot == 4) return false;
        return true;
    }

    // --- IEnergyProviderMK2 ---

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    // --- IFluidStandardTransceiverMK2 ---

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return this.getSmokeTanks(); }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    // --- MenuProvider ---

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineDiesel");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineDiesel(containerId, playerInventory, this);
    }

    // --- sync + persistence ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(powerCap);
        buf.writeBoolean(wasOn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        powerCap = buf.readLong();
        wasOn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("powerTime", power);
        output.putLong("powerCap", powerCap);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("powerTime", 0L);
        powerCap = input.getLongOr("powerCap", maxPower);
        tank.readFromNBT(input, "tank");
    }
}
