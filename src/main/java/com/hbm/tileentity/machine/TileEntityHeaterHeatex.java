package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerHeaterHeatex;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.Tags;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The legacy heat-exchanging heater. Hot fluid is cooled into its trait-declared output while its
 * released heat is retained as TU for an adjacent consumer.
 *
 * <p>The legacy machine has one identifier slot and no sided item access. Fluid input and output are
 * network-only and are deliberately restricted to the four remote horizontal connection points.
 */
public class TileEntityHeaterHeatex extends TileEntityMachineBase
        implements IHeatSource, IControlReceiver, IFluidStandardTransceiverMK2, MenuProvider, IRORValueProvider {

    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.COOLANT_HOT, TANK_CAPACITY),
            new FluidTankNTM(Fluids.COOLANT, TANK_CAPACITY)
    };

    public int amountToCool = TANK_CAPACITY;
    public int tickDelay = 1;
    public int heatEnergy;

    public TileEntityHeaterHeatex(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATER_HEATEX.get(), pos, state, 1);
    }

    @Override
    public String getDefaultName() {
        return "container.heaterHeatex";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityHeaterHeatex heater) {
        heater.updateTypeFromFluidId();
        heater.setupTanks();
        heater.updateConnections(level);

        // The old machine decays its stored TU before adding the current cooling operation's heat.
        heater.heatEnergy *= 0.999;
        heater.tryConvert(level);
        heater.networkPackNT();

        for (DirPos connection : heater.getConPos()) {
            if (heater.tanks[1].getFill() > 0) {
                heater.tryProvide(heater.tanks[1], level, connection);
            }
        }
    }

    /** Slot 0 changes the input type in place; the identifier is not consumed or moved. */
    private void updateTypeFromFluidId() {
        var stack = inventory.getStackInSlot(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof com.hbm.items.machine.IItemFluidId identifier)) return;

        FluidType type = identifier.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        if (type != tanks[0].getTankType()) {
            tanks[0].setTankType(type);
            setChanged();
        }
    }

    /** Derives the output type from the input's heat-exchanger efficiency every tick. */
    protected void setupTanks() {
        FluidType input = tanks[0].getTankType();
        FT_Coolable trait = input.getTrait(FT_Coolable.class);
        if (trait != null && trait.getEfficiency(FT_Coolable.CoolingType.HEATEXCHANGER) > 0D) {
            tanks[1].setTankType(trait.coolsTo);
            return;
        }

        tanks[0].setTankType(Fluids.NONE);
        tanks[1].setTankType(Fluids.NONE);
    }

    private void updateConnections(Level level) {
        for (DirPos connection : getConPos()) {
            trySubscribe(tanks[0].getTankType(), level, connection);
        }
    }

    /** Performs the exact old min(input, output, throttle) operation calculation. */
    protected void tryConvert(Level level) {
        FT_Coolable trait = tanks[0].getTankType().getTrait(FT_Coolable.class);
        if (trait == null || trait.getEfficiency(FT_Coolable.CoolingType.HEATEXCHANGER) <= 0D) return;

        if (tickDelay < 1) tickDelay = 1;
        if (level.getGameTime() % tickDelay != 0) return;

        int inputOps = tanks[0].getFill() / trait.amountReq;
        int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
        int ops = Math.min(inputOps, Math.min(outputOps, amountToCool));

        tanks[0].setFill(tanks[0].getFill() - trait.amountReq * ops);
        tanks[1].setFill(tanks[1].getFill() + trait.amountProduced * ops);
        heatEnergy += (int) (trait.heatEnergy * ops
                * trait.getEfficiency(FT_Coolable.CoolingType.HEATEXCHANGER));
        setChanged();
    }

    /** The four old horizontal ports: two facing forward and two facing backward. */
    public DirPos[] getConPos() {
        int meta = getBlockState().hasProperty(BlockDummyable.META)
                ? getBlockState().getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        ForgeDirection facing = ForgeDirection.getOrientation(meta - BlockDummyable.offset);
        ForgeDirection lateral = facing.getRotation(ForgeDirection.UP);
        BlockPos pos = getBlockPos();

        return new DirPos[]{
                new DirPos(pos.getX() + facing.offsetX * 2 + lateral.offsetX, pos.getY(),
                        pos.getZ() + facing.offsetZ * 2 + lateral.offsetZ, facing),
                new DirPos(pos.getX() + facing.offsetX * 2 - lateral.offsetX, pos.getY(),
                        pos.getZ() + facing.offsetZ * 2 - lateral.offsetZ, facing),
                new DirPos(pos.getX() - facing.offsetX * 2 + lateral.offsetX, pos.getY(),
                        pos.getZ() - facing.offsetZ * 2 + lateral.offsetZ, facing.getOpposite()),
                new DirPos(pos.getX() - facing.offsetX * 2 - lateral.offsetX, pos.getY(),
                        pos.getZ() - facing.offsetZ * 2 - lateral.offsetZ, facing.getOpposite())
        };
    }

    @Override
    public int getHeatStored() {
        return heatEnergy;
    }

    @Override
    public void useUpHeat(int heat) {
        heatEnergy = Math.max(0, heatEnergy - heat);
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        ForgeDirection facing = ForgeDirection.getOrientation(getBlockState().getValue(BlockDummyable.META)
                - BlockDummyable.offset);
        return dir == facing || dir == facing.getOpposite();
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return tanks;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{tanks[1]};
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{tanks[0]};
    }

    /** The old machine inherited an empty sided-inventory list, so hoppers cannot reach slot 0. */
    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[0];
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) < 16D * 16D;
    }

    /** The old menu used the base machine's 128-square-use range, not the control packet's 16-block range. */
    @Override
    public boolean stillValid(Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 128D;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toCool")) {
            amountToCool = net.minecraft.util.Mth.clamp(Tags.getIntOr(data, "toCool", 0), 1, tanks[0].getMaxFill());
        }
        if (data.contains("delay")) {
            tickDelay = Math.max(Tags.getIntOr(data, "delay", 0), 1);
        }
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerHeaterHeatex(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeInt(heatEnergy);
        buf.writeInt(amountToCool);
        buf.writeInt(tickDelay);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        heatEnergy = buf.readInt();
        amountToCool = buf.readInt();
        tickDelay = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "0");
        tanks[1].writeToNBT(output, "1");
        output.putInt("heatEnergy", heatEnergy);
        output.putInt("toCool", amountToCool);
        output.putInt("delay", tickDelay);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "0");
        tanks[1].readFromNBT(input, "1");
        heatEnergy = input.getIntOr("heatEnergy", 0);
        amountToCool = input.getIntOr("toCool", 0);
        tickDelay = input.getIntOr("delay", 0);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "hotfluid", PREFIX_VALUE + "coldfluid", PREFIX_VALUE + "heat"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "hotfluid").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "coldfluid").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heatEnergy;
        return null;
    }
}
