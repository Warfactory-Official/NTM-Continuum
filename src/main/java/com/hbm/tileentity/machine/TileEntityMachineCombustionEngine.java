package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.util.TickPhase;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.inventory.container.ContainerCombustionEngine;
import com.hbm.inventory.fluid.FluidCombustion;
import com.hbm.inventory.fluid.FluidCombustion.Combustible;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.items.machine.ItemPistons;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachinePolluting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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

/**
 * Combustion Engine — 1:1 port of the 1.12.2 {@code TileEntityMachineCombustionEngine}: an
 * {@link IEnergyProviderMK2} generator burning DIESEL-grade fuels. 5 slots (0→1 fuel container drain,
 * 2 piston set, 3 battery to charge, 4 fluid identifier). maxPower 2.5M; throttle {@code setting 0-30}
 * ({@code speed = setting·2} mB×10/tick); {@code power += toBurn · combustionEnergy/10_000 · pistonEff}.
 * A sub-mB {@code tenth} accumulator preserves fractional burn.
 *
 * <p>Every fifth burning tick the fuel's {@code FT_Polluting} burn table goes into the 50 mB smoke tanks,
 * which leave through the four fuel ports.
 *
 * <p>Deferred (§20): OpenComputers {@code SimpleComponent}, IFluidCopiable.
 */
public class TileEntityMachineCombustionEngine extends TileEntityMachinePolluting
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public static final long maxPower = 2_500_000;

    public boolean isOn = false;
    public boolean wasOn = false;
    public long power;
    public int setting = 0;
    public int tenth = 0;
    public int playersUsing = 0;

    public float doorAngle = 0;
    public float prevDoorAngle = 0;

    public FluidTankNTM tank;

    public TileEntityMachineCombustionEngine(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_COMBUSTION_ENGINE.get(), pos, state, 5, 50);
        tank = new FluidTankNTM(Fluids.DIESEL, 24_000);
    }

    @Override public String getDefaultName() { return "container.combustionEngine"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCombustionEngine te) {
        te.loadTank();
        te.setTypeFromFluidId();

        te.wasOn = false;

        int fill = te.tank.getFill() * 10 + te.tenth;
        ItemStack piston = te.inventory.getStackInSlot(2);
        Combustible fuel = FluidCombustion.getCombustible(te.tank.getTankType());

        if (te.isOn && te.setting > 0 && !piston.isEmpty() && piston.getItem() instanceof ItemPistons pistonItem
                && fill > 0 && fuel != null) {
            double eff = pistonItem.getVariant().eff[fuel.grade().ordinal()];
            if (eff > 0) {
                int speed = te.setting * 2;
                int toBurn = Math.min(fill, speed);
                te.power += (long) (toBurn * (fuel.energyPerBucket() / 10_000D) * eff);
                fill -= toBurn;

                if (TickPhase.every(level, pos, 5L) && toBurn > 0) {
                    te.pollute(te.tank.getTankType(), FluidReleaseType.BURN, toBurn * 0.5F);
                }

                if (toBurn > 0) te.wasOn = true;
                te.tank.setFill(fill / 10);
                te.tenth = fill % 10;
            }
        }

        te.power = Library.chargeItemsFromTE(te.inventory, 3, te.power, maxPower);

        for (DirPos dp : te.getConPos()) {
            te.tryProvide(level, dp.getPos().getX(), dp.getPos().getY(), dp.getPos().getZ(), dp.getDir());
            te.sendSmoke(dp);
        }

        if (te.power > maxPower) te.power = maxPower;
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCombustionEngine te) {
        te.prevDoorAngle = te.doorAngle;
        float swingSpeed = (te.doorAngle / 10F) + 3;
        if (te.playersUsing > 0) te.doorAngle += swingSpeed; else te.doorAngle -= swingSpeed;
        te.doorAngle = Mth.clamp(te.doorAngle, 0F, 135F);
        com.hbm.client.sound.CombustionEngineSoundHandler.tickLoop(te);
    }

    @Override
    public int countMufflers() {
        if (level == null) return 0;
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(direction)).is(ModBlocks.muffler.get())) count++;
        }
        return count;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) com.hbm.client.sound.CombustionEngineSoundHandler.onRemoved(this);
    }

    public DirPos[] getConPos() {
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.offsetX + rot.offsetX, y, z + dir.offsetZ + rot.offsetZ, dir),
                new DirPos(x + dir.offsetX - rot.offsetX, y, z + dir.offsetZ - rot.offsetZ, dir),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX, y, z - dir.offsetZ * 2 + rot.offsetZ, dir.getOpposite()),
                new DirPos(x - dir.offsetX * 2 - rot.offsetX, y, z - dir.offsetZ * 2 - rot.offsetZ, dir.getOpposite())
        };
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
        ItemStack id = inventory.getStackInSlot(4);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tank.getTankType()) {
            tank.setTankType(t);
            tenth = 0;
            setChanged();
        }
    }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }

    /** Menu button dispatch: 0 = ignition toggle; 1000..1030 = set throttle to (id − 1000). */
    public void handleButton(int id) {
        if (id == 0) isOn = !isOn;
        else if (id >= 1000 && id <= 1030) setting = id - 1000;
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 2) return stack.getItem() instanceof ItemPistons;
        if (slot == 3) return Library.isBattery(stack);
        if (slot == 4) return stack.getItem() instanceof IItemFluidId;
        // slot 1 (emptied container out) stays TRUE — machine writes it via insertItem
        return true;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return this.getSmokeTanks(); }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public Component getDisplayName() { return Component.translatable("container.combustionEngine"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCombustionEngine(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(playersUsing);
        buf.writeInt(setting);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        buf.writeBoolean(wasOn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        playersUsing = buf.readInt();
        setting = buf.readInt();
        power = buf.readLong();
        isOn = buf.readBoolean();
        wasOn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("setting", setting);
        output.putLong("power", power);
        output.putBoolean("isOn", isOn);
        output.putInt("tenth", tenth);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        setting = input.getIntOr("setting", 0);
        power = input.getLongOr("power", 0L);
        isOn = input.getBooleanOr("isOn", false);
        tenth = input.getIntOr("tenth", 0);
        tank.readFromNBT(input, "tank");
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "state", PREFIX_VALUE + "throttle", PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel", PREFIX_VALUE + "efficiency",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "throttle"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "state").equals(name)) return "" + (isOn ? 1 : 0);
        if ((PREFIX_VALUE + "throttle").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "efficiency").equals(name)) {
            ItemStack stack = inventory.getStackInSlot(2);
            FluidCombustion.Combustible fuel = FluidCombustion.getCombustible(tank.getTankType());
            if (!stack.isEmpty() && stack.getItem() instanceof ItemPistons piston && fuel != null) {
                return "" + Math.round(piston.getVariant().eff[fuel.grade().ordinal()] * 100);
            }
            return "0";
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            isOn = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            setting = IRORInteractive.parseInt(params[0], 0, 30);
            setChanged();
        }
        return null;
    }
}
