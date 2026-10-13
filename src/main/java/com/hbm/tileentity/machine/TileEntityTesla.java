package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.TeslaZap;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.hbm.tileentity.ModEnergyNetBlockEntities;

/**
 * 1.7's {@code TileEntityTesla}: a 100 kHE receiver that, every tick it holds 5 kHE, spends it on an
 * arc at everything living within 10 blocks (the shared {@link TeslaZap} law: crabs heal, creepers
 * charge, Faraday suits are immune). A meteor battery underneath keeps it full for nothing.
 */
public class TileEntityTesla extends TileEntityMachineBase implements IEnergyReceiverMK2 {

    public long power;
    public static final long maxPower = 100000;

    public static int range = 10;
    public static double offset = 1.75;

    public List<Vec3> targets = new ArrayList<>();

    public TileEntityTesla(BlockPos pos, BlockState state) {
        super(ModEnergyNetBlockEntities.TESLA.get(), pos, state, 0);
    }

    @Override
    public String getDefaultName() {
        return "";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityTesla tesla) {
        if (!(level instanceof ServerLevel server)) return;

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            tesla.trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }

        tesla.targets.clear();

        if (level.getBlockState(pos.below()).is(ModBlocks.meteor_battery.get())) tesla.power = maxPower;

        if (tesla.power >= 5000) {
            tesla.power -= 5000;
            double dx = pos.getX() + 0.5;
            double dy = pos.getY() + offset;
            double dz = pos.getZ() + 0.5;
            tesla.targets = TeslaZap.zap(server, dx, dy, dz, range, null);
        }

        tesla.networkPackNT();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeShort((short) targets.size());
        for (Vec3 d : targets) {
            buf.writeDouble(d.x);
            buf.writeDouble(d.y);
            buf.writeDouble(d.z);
        }
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        int s = buf.readShort();
        targets.clear();
        for (int i = 0; i < s; i++) targets.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    @Override public void setPower(long i) { power = i; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
    }
}
