package com.hbm.tileentity.machine.fusion;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerFusionKlystron;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Fusion klystron — the microwave emitter that ignites a torus.
 *
 * <p>It turns stored HE and compressed air into <b>klystron energy</b> (KyU), which is never stored:
 * it is produced and delivered in the same tick, and the torus consumes it in that same tick. The
 * player sets an <i>output target</i>; the machine then runs at whichever fraction its power buffer
 * and air supply can sustain, drawing both in proportion, so an under-supplied klystron emits weakly
 * rather than stalling.
 *
 * <p>One deliberate cliff: below 2% of the target the output drops to zero outright, so a starved
 * klystron cannot dribble out just enough to keep an expensive fuel cycle half-lit.
 */
public class TileEntityFusionKlystron extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, IControlReceiver {

    public static final long MAX_OUTPUT = 1_000_000;
    public static final int AIR_CONSUMPTION = 2_500;
    public static final float FAN_ACCELERATION = 0.125F;


    /** What the player asked for. */
    public long outputTarget;
    /** What is actually being emitted this tick. */
    public long output;
    public long power;
    public long maxPower = 1_000_000L;

    public final FluidTankNTM compair = new FluidTankNTM(Fluids.AIR, AIR_CONSUMPTION * 60);

    /** Client-side fan spin. */
    public float fan;
    public float prevFan;
    public float fanSpeed;

    public TileEntityFusionKlystron(BlockPos pos, BlockState state) {
        this(ModBlockEntities.FUSION_KLYSTRON.get(), pos, state);
    }

    protected TileEntityFusionKlystron(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override public String getDefaultName() { return "container.fusionKlystron"; }

    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFusionKlystron(containerId, playerInventory, this);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionKlystron te) {
        // The buffer scales with the target so a big klystron can actually hold a tick's worth.
        te.maxPower = Math.max(1_000_000L, te.outputTarget * 100L);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);


        te.output = 0;
        double powerFactor = TileEntityFusionTorus.getSpeedScaled(te.maxPower, te.power);
        double airFactor = TileEntityFusionTorus.getSpeedScaled(te.compair.getMaxFill(), te.compair.getFill());
        double factor = Math.min(powerFactor, airFactor);

        long powerReq = (long) Math.ceil(te.outputTarget * factor);
        int airReq = (int) Math.ceil(AIR_CONSUMPTION * factor);

        if (te.outputTarget > 0 && te.power >= powerReq && te.compair.getFill() >= airReq) {
            te.output = powerReq;
            te.power -= powerReq;
            te.compair.setFill(te.compair.getFill() - airReq);
        }
        // Below 2% of target, emit nothing at all rather than a useless trickle.
        if (te.output < te.outputTarget / 50) te.output = 0;

        FusionLink.claim(level, te, FusionNodes.emitter(pos, FusionNodes.facing(state)),
                MachineCaps.KLYSTRON_OUT);
        FusionNodes.provideKyU(level, pos, FusionNodes.facing(state), te.output);

        te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFusionKlystron te) {
        double mult = TileEntityFusionTorus.getSpeedScaled(te.outputTarget, te.output);
        te.spinFan(te.output > 0, (float) mult);
    }

    /** Ramps the fan toward its target speed and wraps the angle. Shared with the creative variant. */
    protected void spinFan(boolean running, float mult) {
        if (running) this.fanSpeed += FAN_ACCELERATION * mult;
        else this.fanSpeed -= FAN_ACCELERATION;
        this.fanSpeed = Mth.clamp(this.fanSpeed, 0F, 5F * mult);

        this.prevFan = this.fan;
        this.fan += this.fanSpeed;
        if (this.fan >= 360F) {
            this.fan -= 360F;
            this.prevFan -= 360F;
        }
    }

    public List<DirPos> getConPos() {
        ForgeDirection dir = FusionNodes.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x + dir.offsetX * 4, y + 2, z + dir.offsetZ * 4, dir),
                new DirPos(x + rot.offsetX * 3, y, z + rot.offsetZ * 3, rot),
                new DirPos(x - rot.offsetX * 3, y, z - rot.offsetZ * 3, rot.getOpposite()));
    }

    @Override
    public void setRemoved() {
        FusionLink.release(level, FusionNodes.emitter(worldPosition, FusionNodes.facing(getBlockState())));
        super.setRemoved();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == 0 && Library.isBattery(stack);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return Math.max(maxPower, 1_000_000L); }

    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{compair}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{compair}; }

    /** The GUI sits up the machine's tower, so permission is measured from two blocks above the core. */
    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 2.5,
                worldPosition.getZ() + 0.5) < 20 * 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("amount")) return;
        // Mth.clamp has no long overload below 26.1.
        long amount = dev.golem.core.data.Tags.getLongOr(data, "amount", 0L);
        this.outputTarget = Math.max(0L, Math.min(amount, MAX_OUTPUT));
        setChanged();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeLong(outputTarget);
        buf.writeLong(output);
        compair.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        outputTarget = buf.readLong();
        output = buf.readLong();
        compair.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putLong("power", power);
        out.putLong("maxPower", maxPower);
        out.putLong("outputTarget", outputTarget);
        compair.writeToNBT(out, "t");
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        power = in.getLongOr("power", 0L);
        maxPower = in.getLongOr("maxPower", 1_000_000L);
        outputTarget = in.getLongOr("outputTarget", 0L);
        compair.readFromNBT(in, "t");
    }

    @Override protected boolean syncFullTagOnChunkLoad() { return true; }
}
