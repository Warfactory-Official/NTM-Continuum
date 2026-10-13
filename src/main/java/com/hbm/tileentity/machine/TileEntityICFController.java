package com.hbm.tileentity.machine;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.ModIcfBlocks;
import com.hbm.blocks.machine.MachineICFController;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IConfigurableMachine;
import com.hbm.tileentity.ModIcfBlockEntities;
import com.hbm.tileentity.TileEntityLoadedBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import java.io.IOException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * The ICF laser controller's block entity (1.7 {@code TileEntityICFController}). An assembled array buffers HE
 * up to a capacity set by its capacitors and turbochargers; every tick it holds any power it fires the lot down
 * the line in front of it. A beam that reaches an ICF puts its power (and its capacity) into the reactor's laser
 * input for that tick; anything softer than 6000 resistance in the way is destroyed, and anything standing in the
 * beam takes 50 fire damage and burns.
 *
 * <p>Only cells in a straight line behind the controller count, only flash tubes touching a counted cell, only
 * capacitors touching a counted tube and only turbochargers touching a counted capacitor.
 *
 * <p>{@code capacitorPower}/{@code turboPower} are the {@code icfLaser} entry of {@code hbmConfig/hbmMachines.json}
 * ({@link #CONFIG}, read by {@link com.hbm.config.MachineDynConfig} at start, as 1.7).
 */
public class TileEntityICFController extends TileEntityLoadedBase implements IEnergyReceiverMK2 {

    public long power;
    public int laserLength;

    public int cellCount;
    public int emitterCount;
    public int capacitorCount;
    public int turbochargerCount;

    public static int capacitorPower = 2_500_000;
    public static int turboPower = 5_000_000;

    /** 1.7 {@code IConfigurableMachine} of this class: {@code icfLaser}, {@code I:capacitorPower}, {@code I:turboPower}. */
    public static final IConfigurableMachine CONFIG = new IConfigurableMachine() {
        @Override public String getConfigName() { return "icfLaser"; }

        @Override public void readIfPresent(JsonObject obj) {
            capacitorPower = IConfigurableMachine.grab(obj, "I:capacitorPower", capacitorPower);
            turboPower = IConfigurableMachine.grab(obj, "I:turboPower", turboPower);
        }

        @Override public void writeConfig(JsonWriter writer) throws IOException {
            writer.name("I:capacitorPower").value(capacitorPower);
            writer.name("I:turboPower").value(turboPower);
        }
    };

    protected final List<BlockPos> ports = new ArrayList<>();

    public boolean assembled;

    public TileEntityICFController(BlockPos pos, BlockState state) {
        super(ModIcfBlockEntities.ICF_CONTROLLER.get(), pos, state);
    }

    private ForgeDirection facing() {
        BlockState state = getBlockState();
        return state.hasProperty(MachineICFController.FACING)
                ? ForgeDirection.getOrientation(state.getValue(MachineICFController.FACING)) : ForgeDirection.NORTH;
    }

    public void setup(HashSet<BlockPos> ports, HashSet<BlockPos> cells, HashSet<BlockPos> emitters,
                      HashSet<BlockPos> capacitors, HashSet<BlockPos> turbochargers) {

        this.cellCount = 0;
        this.emitterCount = 0;
        this.capacitorCount = 0;
        this.turbochargerCount = 0;

        ForgeDirection dir = facing().getOpposite();
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        HashSet<BlockPos> validCells = new HashSet<>();
        HashSet<BlockPos> validEmitters = new HashSet<>();
        HashSet<BlockPos> validCapacitors = new HashSet<>();

        for (int i = 0; i < cells.size(); i++) {
            int j = i + 1;
            BlockPos pos = new BlockPos(x + dir.offsetX * j, y, z + dir.offsetZ * j);
            if (cells.contains(pos)) {
                this.cellCount++;
                validCells.add(pos);
            } else {
                break;
            }
        }

        for (BlockPos emitter : emitters) {
            for (ForgeDirection offset : ForgeDirection.VALID_DIRECTIONS) {
                if (validCells.contains(emitter.offset(offset.offsetX, offset.offsetY, offset.offsetZ))) {
                    this.emitterCount++;
                    validEmitters.add(emitter);
                    break;
                }
            }
        }

        for (BlockPos capacitor : capacitors) {
            for (ForgeDirection offset : ForgeDirection.VALID_DIRECTIONS) {
                if (validEmitters.contains(capacitor.offset(offset.offsetX, offset.offsetY, offset.offsetZ))) {
                    this.capacitorCount++;
                    validCapacitors.add(capacitor);
                    break;
                }
            }
        }

        for (BlockPos turbo : turbochargers) {
            for (ForgeDirection offset : ForgeDirection.VALID_DIRECTIONS) {
                if (validCapacitors.contains(turbo.offset(offset.offsetX, offset.offsetY, offset.offsetZ))) {
                    this.turbochargerCount++;
                    break;
                }
            }
        }

        // 1.7 appends without clearing, so a re-assembled array lists its ports again; only the save/load path
        // below clears. The list is informational here (the energy graph finds the port cells itself).
        this.ports.addAll(ports);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityICFController te) {
        te.networkPackNT();

        if (te.assembled) {
            // 1.7: each port's neighbours; the port cells front the controller (TileEntityBlockICF)
            for (BlockPos port : te.ports) {
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    if (te.getMaxPower() > 0) te.trySubscribe(level, port.getX() + dir.offsetX, port.getY() + dir.offsetY, port.getZ() + dir.offsetZ, dir);
                }
            }
            if (te.power > 0) {
                ForgeDirection dir = te.facing();

                for (int i = 1; i < 50; i++) {
                    te.laserLength = i;

                    BlockPos at = pos.offset(dir.offsetX * i, 0, dir.offsetZ * i);
                    BlockState b = level.getBlockState(at);
                    if (b.getBlock() == ModIcfBlocks.icf.get()) {
                        BlockEntity tile = level.getBlockEntity(pos.offset(dir.offsetX * (i + 8), -3, dir.offsetZ * (i + 8)));
                        if (tile instanceof TileEntityICF icf) {
                            icf.laser += te.getPower();
                            icf.maxLaser += te.getMaxPower();
                            break;
                        }
                    }

                    if (!b.isAir()) {
                        // 1.7 getExplosionResistance(null) is the modern resistance value
                        float hardness = b.getBlock().getExplosionResistance();
                        if (hardness < 6000) level.destroyBlock(at, false);
                        break;
                    }
                }

                int len = te.laserLength;
                double blx = Math.min(pos.getX(), pos.getX() + dir.offsetX * len) + 0.2;
                double bux = Math.max(pos.getX(), pos.getX() + dir.offsetX * len) + 0.8;
                double bly = Math.min(pos.getY(), pos.getY() + dir.offsetY * len) + 0.2;
                double buy = Math.max(pos.getY(), pos.getY() + dir.offsetY * len) + 0.8;
                double blz = Math.min(pos.getZ(), pos.getZ() + dir.offsetZ * len) + 0.2;
                double buz = Math.max(pos.getZ(), pos.getZ() + dir.offsetZ * len) + 0.8;

                List<Entity> list = level.getEntitiesOfClass(Entity.class, new AABB(blx, bly, blz, bux, buy, buz));
                for (Entity e : list) {
                    if (level instanceof ServerLevel server) Compat.hurt(e, server, server.damageSources().inFire(), 50);
                    e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 5 * 20)); // setFire(5)
                }

                te.setPower(0);
            } else {
                te.laserLength = 0;
            }
        } else {
            te.laserLength = 0;
        }
    }

    /** 1.7's remote branch: one in five ticks a red dust mote at the lens while the beam is on. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityICFController te) {
        if (te.laserLength > 0 && level.getRandom().nextInt(5) == 0) {
            ForgeDirection dir = te.facing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            double offXZ = level.getRandom().nextDouble() * 0.25 - 0.125;
            double offY = level.getRandom().nextDouble() * 0.25 - 0.125;
            double dist = 0.55;
            level.addParticle(DustParticleOptions.REDSTONE, pos.getX() + 0.5 + dir.offsetX * dist + rot.offsetX * offXZ,
                    pos.getY() + 0.5 + offY, pos.getZ() + 0.5 + dir.offsetZ * dist + rot.offsetZ * offXZ, 0, 0, 0);
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(capacitorCount);
        buf.writeInt(turbochargerCount);
        buf.writeInt(laserLength);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.capacitorCount = buf.readInt();
        this.turbochargerCount = buf.readInt();
        this.laserLength = buf.readInt();
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);

        this.power = input.getLongOr("power", 0L);

        this.assembled = input.getBooleanOr("assembled", false);
        this.cellCount = input.getIntOr("cellCount", 0);
        this.emitterCount = input.getIntOr("emitterCount", 0);
        this.capacitorCount = input.getIntOr("capacitorCount", 0);
        this.turbochargerCount = input.getIntOr("turbochargerCount", 0);

        ports.clear();
        int portCount = input.getIntOr("portCount", 0);
        for (int i = 0; i < portCount; i++) {
            int[] port = input.getIntArrayOr("p" + i, null);
            if (port != null && port.length == 3) ports.add(new BlockPos(port[0], port[1], port[2]));
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);

        output.putLong("power", power);

        output.putBoolean("assembled", assembled);
        output.putInt("cellCount", cellCount);
        output.putInt("emitterCount", emitterCount);
        output.putInt("capacitorCount", capacitorCount);
        output.putInt("turbochargerCount", turbochargerCount);

        output.putInt("portCount", ports.size());
        for (int i = 0; i < ports.size(); i++) {
            BlockPos pos = ports.get(i);
            output.putIntArray("p" + i, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    @Override
    public long getPower() {
        return Math.min(power, this.getMaxPower());
    }

    @Override
    public void setPower(long power) {
        this.power = power;
    }

    @Override
    public long getMaxPower() {
        return (long) (Math.sqrt(capacitorCount) * capacitorPower + Math.sqrt(Math.min(turbochargerCount, capacitorCount)) * turboPower);
    }

    /** The controller block itself takes no cable; only the array's port cells do (1.7 subscribed from them). */
    @Override
    public boolean canConnect(ForgeDirection dir) {
        return false;
    }
}
