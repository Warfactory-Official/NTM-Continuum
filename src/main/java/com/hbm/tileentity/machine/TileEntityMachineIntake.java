package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import dev.golem.core.machine.SyncedBlockEntity;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Server buffer and client motor state for the legacy air intake. */
public class TileEntityMachineIntake extends SyncedBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardSenderMK2 {

    public static final long MAX_POWER = 2_000L;
    public static final long POWER_PER_TICK = MAX_POWER / 20L;
    public static final int AIR_CAPACITY = 1_000;

    public final FluidTankNTM compair = new FluidTankNTM(Fluids.AIR, AIR_CAPACITY);
    public long power;
    public float fan;
    public float prevFan;

    public TileEntityMachineIntake(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isMotorRunning() {
        return power >= POWER_PER_TICK;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineIntake intake) {
        if (intake.isMotorRunning()) {
            intake.compair.setFill(intake.compair.getMaxFill());
            intake.power -= POWER_PER_TICK;
        }

        for (DirPos connection : intake.getConPos()) {
            if (intake.compair.getFill() > 0) intake.tryProvide(intake.compair, level, connection.getPos(), connection.getDir().toDirection());
        }

        intake.setChanged();
        intake.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineIntake intake) {
        intake.prevFan = intake.fan;
        if (intake.isMotorRunning()) {
            intake.fan += 45F;
            if (intake.fan >= 360F) {
                intake.fan -= 360F;
                intake.prevFan -= 360F;
            }
        }
        com.hbm.client.sound.IntakeSoundHandler.tickLoop(intake);
    }

    public ForgeDirection getFacing() {
        BlockState state = getBlockState();
        int meta = MachineCore.facingOrdinal(state, 2);
        return ForgeDirection.getOrientation(meta);
    }

    /** The eight air/power anchors around the legacy two-by-two intake footprint. */
    public DirPos[] getConPos() {
        ForgeDirection direction = getFacing();
        ForgeDirection rotation = direction.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + direction.offsetX, y, z + direction.offsetZ, direction),
                new DirPos(x + direction.offsetX + rotation.offsetX, y,
                        z + direction.offsetZ + rotation.offsetZ, direction),
                new DirPos(x - direction.offsetX * 2, y, z - direction.offsetZ * 2, direction.getOpposite()),
                new DirPos(x - direction.offsetX * 2 + rotation.offsetX, y,
                        z - direction.offsetZ * 2 + rotation.offsetZ, direction.getOpposite()),
                new DirPos(x + rotation.offsetX * 2, y, z + rotation.offsetZ * 2, rotation),
                new DirPos(x + rotation.offsetX * 2 - direction.offsetX, y,
                        z + rotation.offsetZ * 2 - direction.offsetZ, rotation),
                new DirPos(x - rotation.offsetX, y, z - rotation.offsetZ, rotation.getOpposite()),
                new DirPos(x - rotation.offsetX - direction.offsetX, y,
                        z - rotation.offsetZ - direction.offsetZ, rotation.getOpposite())
        };
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long value) { power = value; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{compair}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{compair}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        compair.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        compair.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        compair.writeToNBT(output, "compair");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        compair.readFromNBT(input, "compair");
    }
}
