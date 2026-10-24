package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.tileentity.IRelocatable;
import com.hbm.tileentity.ModRadarUtilityBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePortShapes;
import dev.golem.core.Ids;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.SubLevels;
import dev.golem.core.world.Teleports;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//? if >=26.1 {
import net.minecraft.world.level.portal.TeleportTransition;
//?} elif >=1.21.1 {
/*import net.minecraft.world.level.portal.DimensionTransition;
*///?}
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 1.7 teleporter: all six power faces, one million HE per attempted send, and an exit 1.5 blocks above its target.
 * An addon may configure a real receiving tank and crossing charge; same-level sends still need power alone.
 */
public class TileEntityMachineTeleporter extends TileEntityLoadedBase
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, IRelocatable {

    private record CrossingFuel(Supplier<FluidType> fluid, int capacity, int cost) {}
    private static volatile @Nullable CrossingFuel crossingFuel;
    private static final FluidTankNTM[] NO_TANKS = new FluidTankNTM[0];

    /** Register during mod construction, before any world or block entity exists. */
    public static synchronized void configureCrossingFuel(Supplier<FluidType> fluid, int capacity, int cost) {
        if (crossingFuel != null) throw new IllegalStateException("Teleporter crossing fuel is already configured");
        if (cost <= 0 || capacity < cost) throw new IllegalArgumentException("Invalid teleporter crossing fuel capacity/cost");
        crossingFuel = new CrossingFuel(fluid, capacity, cost);
    }

    public static boolean hasCrossingFuel() { return crossingFuel != null; }

    public long power;
    public @Nullable GlobalPos target;
    public static final int maxPower = 1_500_000;
    public static final int consumption = 1_000_000;
    private final @Nullable FluidTankNTM crossingTank;
    private final FluidTankNTM[] receivingTanks;
    private final int crossingCost;

    public TileEntityMachineTeleporter(BlockPos pos, BlockState state) {
        super(ModRadarUtilityBlockEntities.TELEPORTER.get(), pos, state);
        var fuel = crossingFuel;
        crossingTank = fuel == null ? null : new FluidTankNTM(fuel.fluid().get(), fuel.capacity());
        receivingTanks = crossingTank == null ? NO_TANKS : new FluidTankNTM[] {crossingTank};
        crossingCost = fuel == null ? 0 : fuel.cost();
    }

    public @Nullable FluidTankNTM getCrossingTank() { return crossingTank; }
    public int getCrossingCost() { return crossingCost; }
    @Override public FluidTankNTM[] getAllTanks() { return receivingTanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return receivingTanks; }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        var refused = IFluidStandardReceiverMK2.super.transferFluid(type, pressure, amount);
        if (refused != amount) setChanged();
        return refused;
    }

    public void setTarget(@Nullable GlobalPos target) {
        this.target = target;
        setChanged();
    }

    /** A carried teleporter follows its destination only when that block was carried too. */
    @Override
    public void relocate(SubLevels.Move move) {
        if (target == null) return;
        setTarget(GlobalPos.of(target.dimension(), IRelocatable.follow(move, target.pos())));
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) cachedPorts = TilePortShapes.around(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        return cachedPorts;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTeleporter tele) {
        tele.setupPowerPorts(tele.getPorts());
        if (tele.crossingTank != null) tele.setupFluidPorts(tele.getPorts());
        tele.updateAllPorts();
        tele.receivePower();
        if (tele.target != null) {
            List<Entity> entities = level.getEntities((Entity) null, new AABB(pos.getX() + 0.25, pos.getY(),
                    pos.getZ() + 0.25, pos.getX() + 0.75, pos.getY() + 2, pos.getZ() + 0.75));
            for (var entity : entities) tele.teleport(entity);
        }
        tele.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineTeleporter tele) {
        if (tele.target != null && tele.power >= consumption
                && (tele.crossingTank == null || tele.crossingTank.getFill() >= tele.crossingCost)) {
            var x = pos.getX() + 0.5 + level.getRandom().nextGaussian() * 0.25D;
            var y = pos.getY() + 1 + level.getRandom().nextDouble() * 2D;
            var z = pos.getZ() + 0.5 + level.getRandom().nextGaussian() * 0.25D;
            level.addParticle(Compat.dust(0x66CCFF, 1.0F), x, y, z, 0, 0, 0);
        }
    }

    public void teleport(Entity entity) { teleport((ServerLevel) level, entity); }

    /**
     * Source order: refuse missing target/power, require and debit crossing fuel, then attempt the move and spend HE.
     * The 1.7 cost remains charged if the destination is absent or the entity refuses the dimensional move.
     */
    public void teleport(ServerLevel level, Entity entity) {
        var exit = target;
        if (exit == null || power < consumption) return;
        var crossing = !entity.level().dimension().equals(exit.dimension());
        if (crossing && crossingTank != null) {
            if (crossingTank.getFill() < crossingCost) return;
            crossingTank.setFill(crossingTank.getFill() - crossingCost);
            setChanged();
        }

        level.playSound(null, worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 1.0F);
        var x = exit.pos().getX() + 0.5D;
        var y = exit.pos().getY() + 1.5D;
        var z = exit.pos().getZ() + 0.5D;

        if (!crossing) {
            if (entity instanceof ServerPlayer player) player.teleportTo(x, y, z);
            else {
                entity.teleportTo(x, y, z);
                Compat.syncPosition(entity);
            }
        } else {
            var destination = level.getServer().getLevel(exit.dimension());
            if (destination != null) {
                // Keep velocity and make the arrival's entity section visible and ticking, as the source crossing does.
                //? if >=26.1 {
                entity.teleport(new TeleportTransition(destination, new Vec3(x, y, z), entity.getDeltaMovement(),
                        entity.getYRot(), entity.getXRot(), TeleportTransition.PLACE_PORTAL_TICKET));
                //?} elif >=1.21.1 {
                /*entity.changeDimension(new DimensionTransition(destination, new Vec3(x, y, z), entity.getDeltaMovement(),
                        entity.getYRot(), entity.getXRot(), DimensionTransition.PLACE_PORTAL_TICKET));
                *///?} else {
                /*destination.getChunk(exit.pos());
                if (Teleports.toLevel(entity, destination, x, y, z)) {
                    destination.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.PORTAL,
                            new net.minecraft.world.level.ChunkPos(exit.pos()), 3, exit.pos());
                }
                *///?}
            }
        }
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.BLOCKS, 1.0F, 1.0F);
        power -= consumption;
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeLong(power);
        buf.writeBoolean(target != null);
        if (target != null) {
            buf.writeUtf(Ids.keyId(target.dimension()).toString());
            buf.writeBlockPos(target.pos());
        }
        if (crossingTank != null) crossingTank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        power = buf.readLong();
        target = buf.readBoolean() ? GlobalPos.of(ResourceKey.create(Registries.DIMENSION, Ids.parse(buf.readUtf())),
                buf.readBlockPos()) : null;
        if (crossingTank != null) crossingTank.deserialize(buf);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        target = input.read("target", GlobalPos.CODEC).orElse(null);
        if (crossingTank != null) crossingTank.readFromNBT(input, "tt");
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.storeNullable("target", GlobalPos.CODEC, target);
        if (crossingTank != null) crossingTank.writeToNBT(output, "tt");
    }

    @Override public void setPower(long amount) { power = amount; }
    @Override public long getPower() { return power; }
    @Override public long getMaxPower() { return maxPower; }
}
