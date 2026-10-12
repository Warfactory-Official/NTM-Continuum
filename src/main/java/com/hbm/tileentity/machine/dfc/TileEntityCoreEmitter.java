package com.hbm.tileentity.machine.dfc;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.blocks.machine.dfc.CoreComponent;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.interfaces.ILaserable;
import com.hbm.inventory.container.ContainerCoreEmitter;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.ModDamageSources;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.EntityDamageUtil;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.Tags;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The emitter: it turns stored HE into a beam of joules and fires it along its facing. The beam walks
 * cell by cell, handing its energy to the first {@link ILaserable} it meets, passing through every
 * {@link TileEntityCore} on the way — each one multiplying what continues — and otherwise burning
 * through whatever is in front of it. Running it at all costs cryogel; running dry turns the emitter
 * into lava.
 *
 * <p>The twelfth machine with a redstone-over-radio surface.
 */
public class TileEntityCoreEmitter extends TileEntityMachineBase
        implements MenuProvider, IControlReceiver, IEnergyReceiverMK2, IFluidStandardReceiverMK2, ILaserable,
        IRORInteractive {

    public static final long maxPower = 1_000_000_000L;
    public static final int range = 50;

    public long power;
    public int watts;
    public int beam;
    public long joules;
    public boolean isOn;
    public long prev;

    public FluidTankNTM tank;

    public TileEntityCoreEmitter(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DFC_EMITTER.get(), pos, state, 0);
        tank = new FluidTankNTM(Fluids.CRYOGEL, 64_000);
    }

    @Override
    public String getDefaultName() {
        return "container.dfcEmitter";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCoreEmitter emitter) {
        emitter.tickServer(level);
    }

    protected void tickServer(Level level) {
        BlockPos pos = getBlockPos();
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            trySubscribe(level, pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
        }
        subscribeToAllAround(tank.getTankType(), this);
        watts = Mth.clamp(watts, 1, 100);
        long demand = maxPower * watts / 2000;

        beam = 0;

        if (joules > 0 || prev > 0) {
            if (tank.getFill() >= 20) {
                tank.setFill(tank.getFill() - 20);
            } else {
                level.setBlockAndUpdate(pos, Blocks.LAVA.defaultBlockState());
                return;
            }
        }

        if (isOn) {
            if (power >= demand) {
                power -= demand;
                joules += watts * 100L;
            }
            prev = joules;

            if (joules > 0) fire(level, pos);
        } else {
            joules = 0;
            prev = 0;
        }

        setChanged();
        networkPackNT();
    }

    /** Walks the beam outward, then burns everything standing in the length it reached. */
    private void fire(Level level, BlockPos pos) {
        long out = joules * 95 / 100;
        ForgeDirection dir = facing();

        for (int i = 1; i <= range; i++) {
            beam = i;
            BlockPos target = pos.offset(dir.offsetX * i, dir.offsetY * i, dir.offsetZ * i);
            BlockState state = level.getBlockState(target);
            BlockEntity be = level.getBlockEntity(target);

            if (state.getBlock() instanceof ILaserable laserable) {
                laserable.addEnergy(level, target, out, dir.toDirection());
                break;
            }
            if (be instanceof ILaserable laserable) {
                laserable.addEnergy(level, target, out, dir.toDirection());
                break;
            }
            if (be instanceof TileEntityCore core) {
                out = core.burn(out);
                continue;
            }

            if (!state.isAir()) {
                if (!state.getFluidState().isEmpty()) {
                    level.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.removeBlock(target, false);
                    break;
                }
                if (state.getBlock().getExplosionResistance() < 6000 && level.getRandom().nextInt(20) == 0) {
                    level.playSound(null, target, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                    level.destroyBlock(target, false);
                }
                break;
            }
        }

        joules = 0;

        BlockPos end = pos.offset(dir.offsetX * beam, dir.offsetY * beam, dir.offsetZ * beam);
        AABB swept = new AABB(
                Math.min(pos.getX(), end.getX()) + 0.2, Math.min(pos.getY(), end.getY()) + 0.2,
                Math.min(pos.getZ(), end.getZ()) + 0.2,
                Math.max(pos.getX(), end.getX()) + 0.8, Math.max(pos.getY(), end.getY()) + 0.8,
                Math.max(pos.getZ(), end.getZ()) + 0.8);

        for (Entity e : level.getEntities(null, swept)) {
            EntityDamageUtil.attackEntityFromIgnoreIFrame(e, ModDamageSources.amsCore(level), 50F);
            Effects.ignite(e, 10);
        }
    }

    public ForgeDirection facing() {
        return ForgeDirection.getOrientation(getBlockState().getValue(CoreComponent.FACING).ordinal());
    }

    @Override
    public void addEnergy(Level level, BlockPos pos, long energy, Direction dir) {
        if (dir.getOpposite() != facing().toDirection()) joules += energy;
    }

    public long getPowerScaled(long i) {
        return (power * i) / maxPower;
    }

    public int getWattsScaled(int i) {
        return (watts * i) / 100;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { this.power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public boolean canConnect(ForgeDirection dir) { return dir != ForgeDirection.UNKNOWN; }
    @Override public boolean canConnect(FluidType type, ForgeDirection dir) { return type == tank.getTankType(); }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(watts);
        buf.writeLong(prev);
        buf.writeInt(beam);
        buf.writeBoolean(isOn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.watts = buf.readInt();
        this.prev = buf.readLong();
        this.beam = buf.readInt();
        this.isOn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("watts", watts);
        output.putLong("joules", joules);
        output.putLong("prev", prev);
        output.putBoolean("isOn", isOn);
        tank.writeToNBT(output, "tank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        watts = input.getIntOr("watts", 0);
        joules = input.getLongOr("joules", 0L);
        prev = input.getLongOr("prev", 0L);
        isOn = input.getBooleanOr("isOn", false);
        tank.readFromNBT(input, "tank");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerCoreEmitter(id, playerInventory, this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.hasInt(data, "watts")) this.watts = Mth.clamp(Tags.getIntOr(data, "watts", 1), 1, 100);
        if (Tags.getBooleanOr(data, "toggle", false)) this.isOn = !this.isOn;
        setChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(getBlockPos().getCenter()) <= 64D;
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_FUNCTION + "setpower" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "toggle",
                PREFIX_FUNCTION + "switch" + NAME_SEPARATOR + "on/off"};
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setpower").equals(name) && params.length > 0) {
            this.watts = IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "toggle").equals(name)) {
            this.isOn = !this.isOn;
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "switch").equals(name) && params.length > 0) {
            if ("on".equals(params[0])) {
                this.isOn = true;
                setChanged();
            } else if ("off".equals(params[0])) {
                this.isOn = false;
                setChanged();
            }
        }

        return null;
    }
}
