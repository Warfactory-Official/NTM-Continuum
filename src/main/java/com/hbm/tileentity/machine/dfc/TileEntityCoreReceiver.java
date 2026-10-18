package com.hbm.tileentity.machine.dfc;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.dfc.CoreComponent;
import com.hbm.interfaces.ILaserable;
import com.hbm.inventory.container.ContainerCoreReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/**
 * The other end of the beam: joules arriving on its face become five thousand HE each, which it pushes
 * out to every side. Hit it from anywhere but the front and it blows up. Like the emitter it spends
 * cryogel whenever it is carrying anything, and turns to lava when it runs out.
 */
public class TileEntityCoreReceiver extends TileEntityMachineBase
        implements MenuProvider, IEnergyProviderMK2, IFluidStandardReceiverMK2, ILaserable {

    public long power;
    public long joules;
    public long prevJoules;
    public FluidTankNTM tank;

    public TileEntityCoreReceiver(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_RECEIVER.get(), pos, state, 0);
        tank = new FluidTankNTM(Fluids.CRYOGEL, 64_000);
    }

    @Override
    public String getDefaultName() {
        return "container.dfcReceiver";
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCoreReceiver receiver) {
        receiver.tickServer(level);
    }

    protected void tickServer(Level level) {
        BlockPos pos = getBlockPos();
        this.setupAllPorts(getPorts());
        this.updatePortPOFIFO();

        power = joules * 5000;

        if (joules > 0) {
            if (tank.getFill() >= 20) {
                tank.setFill(tank.getFill() - 20);
            } else {
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                return;
            }
        }

        prevJoules = joules;
        networkPackNT();
        joules = 0;
    }

    public ForgeDirection facing() {
        return ForgeDirection.getOrientation(getBlockState().getValue(CoreComponent.FACING).ordinal());
    }

    @Override
    public void addEnergy(Level level, BlockPos pos, long energy, Direction dir) {
        if (dir.getOpposite() == facing().toDirection()) {
            joules += energy;
        } else {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 2.5F,
                    Level.ExplosionInteraction.BLOCK);
        }
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { this.power = i; }
    @Override public long getMaxPower() { return power; }

    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }
    @Override public boolean canConnect(FluidType type, ForgeDirection dir) { return type == tank.getTankType(); }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(prevJoules);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.joules = buf.readLong();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putLong("joules", joules);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        joules = input.getLongOr("joules", 0L);
        tank.readFromNBT(input, "tank");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerCoreReceiver(id, playerInventory, this);
    }
}
