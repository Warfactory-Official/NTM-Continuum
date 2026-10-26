package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import com.hbm.module.portmanager.ModulePortManFluidAdaptive;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;

/** Common smoke reception, capture counters, sync and Tower-particle behavior. */
public abstract class TileEntityChimneyBase extends TileEntityLoadedBase implements IFluidReceiverMK2 {

    public long ashTick;
    public long sootTick;
    public int onTicks;

    /** Only the port types: the smoke goes straight into the ash counters. */
    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.SMOKE, 1_000_000),
            new FluidTankNTM(Fluids.SMOKE_LEADED, 1_000_000),
            new FluidTankNTM(Fluids.SMOKE_POISON, 1_000_000)};

    protected final ModulePortManFluidAdaptive moduleSmokePorts = new ModulePortManFluidAdaptive(this).setInputTanks(tanks);

    protected TileEntityChimneyBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() { if (cachedPorts == null) cachedPorts = TilePortShapes.flare(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPorts; }

    @Override
    public void setRemoved() {
        if (level != null) moduleSmokePorts.destroy();
        super.setRemoved();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityChimneyBase chimney) {
        chimney.moduleSmokePorts.update(chimney.getPorts());

        if (chimney.ashTick > 0L || chimney.sootTick > 0L) {
            if (level.getBlockEntity(pos.below()) instanceof TileEntityAshpit ashpit) {
                ashpit.ashLevelFly += (int) chimney.ashTick;
                ashpit.ashLevelSoot += (int) chimney.sootTick;
                ashpit.setChanged();
            }
            chimney.ashTick = 0L;
            chimney.sootTick = 0L;
        }

        // The legacy packet was sent before the visible 20-tick countdown.
        chimney.networkPackNT();
        if (chimney.onTicks > 0) chimney.onTicks--;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityChimneyBase chimney) {
        if (chimney.onTicks > 0) chimney.spawnParticles();
    }

    public static boolean isSmoke(FluidType type) {
        return type == Fluids.SMOKE || type == Fluids.SMOKE_LEADED || type == Fluids.SMOKE_POISON;
    }

    private static final FluidType[] SMOKE_TYPES = {
            Fluids.SMOKE, Fluids.SMOKE_LEADED, Fluids.SMOKE_POISON
    };

    @Override
    public boolean exposesAnyFluidContact(ForgeDirection dir) {
        for (var type : SMOKE_TYPES) if (canConnect(type, dir)) return true;
        return false;
    }

    /** The old misspelling is retained because it is part of the concrete chimney contract. */
    public boolean cpaturesAsh() { return true; }

    /** The old misspelling is retained because it is part of the concrete chimney contract. */
    public boolean cpaturesSoot() { return false; }

    public void spawnParticles() {}

    protected final void spawnTowerSmoke(double y, float base) {
        if (!TickPhase.every(this, 2L)) return;

        CompoundTag data = new CompoundTag();
        data.putFloat("lift", 10F);
        data.putFloat("base", base);
        data.putFloat("max", 3F);
        data.putInt("life", 250 + level.getRandom().nextInt(50));
        data.putInt("color", 0x404040);
        HbmEffectNT.Tower.summonParticle(level, worldPosition.getX() + 0.5D, y,
                worldPosition.getZ() + 0.5D, data);
    }

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return isSmoke(type) && (dir == ForgeDirection.NORTH || dir == ForgeDirection.SOUTH
                || dir == ForgeDirection.EAST || dir == ForgeDirection.WEST);
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long fluid) {
        if (!isSmoke(type)) return fluid;

        onTicks = 20;
        if (cpaturesAsh()) ashTick += fluid;
        if (cpaturesSoot()) sootTick += fluid;

        fluid *= getPollutionMod();

        PollutionType pollutant = type == Fluids.SMOKE ? PollutionType.SOOT
                : type == Fluids.SMOKE_LEADED ? PollutionType.HEAVYMETAL
                : type == Fluids.SMOKE_POISON ? PollutionType.POISON : null;
        if (pollutant != null) {
            PollutionHandler.incrementPollution(level, worldPosition, pollutant, fluid / 100F);
        }
        return 0L;
    }

    public abstract double getPollutionMod();

    @Override
    public long getDemand(FluidType type, int pressure) {
        return 1_000_000L;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{};
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeInt(onTicks);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        onTicks = buf.readInt();
    }
}
