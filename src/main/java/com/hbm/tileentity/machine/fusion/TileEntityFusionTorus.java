package com.hbm.tileentity.machine.fusion;

import dev.golem.core.data.Tags;

import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerFusionTorus;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FusionRecipe;
import dev.golem.core.graph.Domains;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineFusion;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityCooledBase;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.MachineAudio;
import com.hbm.sound.ModSounds;
import net.minecraft.sounds.SoundSource;

/**
 * The fusion torus — a tokamak that burns a fluid fuel cycle into plasma energy.
 *
 * <p>It sits between two networks. Four <b>klystron</b> nodes, one per horizontal direction seven
 * blocks out, feed it microwave energy; the torus needs at least the running cycle's
 * {@link FusionRecipe#ignitionTemp} in a tick to light at all. Four <b>plasma</b> nodes at the same
 * positions carry the output to boilers, MHD turbines, collectors and breeders — and the plasma half
 * is <i>shared</i>: the more receivers hang off the reactor, the less each one gets, though the total
 * goes up (see {@link #getOutputIntensity}).
 *
 * <p>Actual throughput is the minimum of four fractions — stored power, and the fill of each of the
 * three fuel tanks — so a half-fed reactor runs at half rate rather than stalling. On top of that it
 * must be cryogenically cool ({@link TileEntityCooledBase}) and lit.
 *
 * <p>Not ported: the OpenComputers component, the redstone-over-radio provider, the tilt check (the
 * accelerator's structural-stress system, which no ported machine has yet), and the looping reactor
 * hum (no positional looped-audio facade). Recorded in IMPLEMENTATION.md §20.
 */
public class TileEntityFusionTorus extends TileEntityCooledBase implements MenuProvider, IControlReceiver, IRORValueProvider {

    public static final int SLOTS = 3;
    /** Slot 0 battery, slot 1 blueprint, slot 2 product. */
    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final long MAX_POWER = 10_000_000L;
    public static final float MAGNET_ACCELERATION = 0.25F;

    public boolean didProcess = false;
    public final FluidTankNTM[] tanks;
    public final ModuleMachineFusion fusionModule;

    /** Which of the four arms has something attached; drives the port glow in the renderer. */
    public final boolean[] connections = new boolean[4];

    /** Klystron energy handed to us this tick; reset every tick after it is consumed. */
    public long klystronEnergy;
    private long klystronEnergySync;
    public long plasmaEnergy;
    public double fuelConsumption;

    /** Client-side magnet ring rotation. */
    public float magnet;
    public float prevMagnet;
    public float magnetSpeed;

    public TileEntityFusionTorus(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_TORUS.get(), pos, state, SLOTS);
        com.hbm.inventory.recipes.FusionRecipes.INSTANCE.registerRecipes(); // idempotent; registries are flushed by now
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.NONE, 4_000),
                new FluidTankNTM(Fluids.NONE, 4_000),
                new FluidTankNTM(Fluids.NONE, 4_000),
                new FluidTankNTM(Fluids.NONE, 4_000),
        };
        this.fusionModule = new ModuleMachineFusion(0, this, inventory)
                .fluidInput(tanks[0], tanks[1], tanks[2])
                .fluidOutput(tanks[3])
                .itemOutput(SLOT_OUTPUT);
    }

    @Override public String getDefaultName() { return "container.fusionTorus"; }

    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }

    @Override public long getMaxPower() { return MAX_POWER; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFusionTorus(containerId, playerInventory, this);
    }

    /** Called by a klystron over the klystron network; accumulates until the tick consumes it. */
    public void receiveKlystronEnergy(long energy) {
        this.klystronEnergy += energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionTorus te) {
        for (int i = 0; i < 4; i++) {
            FusionLink.claim(level, te, te.nodePos(ForgeDirection.getOrientation(i + 2)),
                    FusionLink.PLASMA_OUT | FusionLink.KLYSTRON_IN);
        }

        te.updateCooling();

        for (DirPos port : te.getConPos()) {
            if (te.coolantTanks[1].getFill() > 0) te.tryProvide(te.coolantTanks[1], level, port.getPos(), port.getDir().toDirection());
            if (te.tanks[3].getFill() > 0) te.tryProvide(te.tanks[3], level, port.getPos(), port.getDir().toDirection());
        }

        te.power = Library.chargeTEFromItems(te.inventory, SLOT_BATTERY, te.power, te.getMaxPower());

        // Plasma receivers share the output, so they have to be counted before anything is sent.
        int receiverCount = 0;
        int collectors = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos beam = te.linkPos(ForgeDirection.getOrientation(i + 2));
            BlockEntity consumer = FusionLink.at(level, beam, FusionLink.PLASMA_IN);
            te.connections[i] = consumer != null
                    || FusionLink.at(level, beam, FusionLink.KLYSTRON_OUT) != null;

            // Upstream counts one receiver per arm, which a claim per position gives for nothing.
            if (!(consumer instanceof IFusionPowerReceiver receiver) || consumer.isRemoved()) continue;
            if (receiver.receivesFusionPower()) receiverCount++;
            if (receiver.providesCollectorBonus()) collectors++;
        }

        FusionRecipe recipe = te.fusionModule.getRecipe() instanceof FusionRecipe fusion ? fusion : null;

        double powerFactor = getSpeedScaled(te.getMaxPower(), te.power);
        double fuel0 = recipe != null && recipe.inputFluid.length > 0 ? getSpeedScaled(te.tanks[0].getMaxFill(), te.tanks[0].getFill()) : 1D;
        double fuel1 = recipe != null && recipe.inputFluid.length > 1 ? getSpeedScaled(te.tanks[1].getMaxFill(), te.tanks[1].getFill()) : 1D;
        double fuel2 = recipe != null && recipe.inputFluid.length > 2 ? getSpeedScaled(te.tanks[2].getMaxFill(), te.tanks[2].getFill()) : 1D;
        double factor = Math.min(powerFactor, Math.min(fuel0, Math.min(fuel1, fuel2)));

        boolean ignition = recipe == null || recipe.ignitionTemp <= te.klystronEnergy;

        te.plasmaEnergy = 0;
        te.fuelConsumption = 0;
        te.fusionModule.preUpdate(factor, collectors * 0.5D);
        te.fusionModule.update(1D, 1D, te.isCool() && ignition, te.inventory.getStackInSlot(SLOT_BLUEPRINT));
        te.didProcess = te.fusionModule.didProcess;
        if (te.fusionModule.markDirty) te.setChanged();

        if (te.didProcess && recipe != null) {
            te.plasmaEnergy = (long) Math.ceil(recipe.outputTemp * factor);
            te.fuelConsumption = factor;
        }

        if (te.plasmaEnergy > 0) {
            double intensity = getOutputIntensity(receiverCount);
            double flux = recipe != null ? recipe.neutronFlux * factor : 0D;
            float r = recipe != null ? recipe.r : 0F;
            float g = recipe != null ? recipe.g : 0F;
            float b = recipe != null ? recipe.b : 0F;
            long share = (long) Math.ceil(te.plasmaEnergy * intensity);

            for (int i = 0; i < 4; i++) {
                if (FusionLink.at(level, te.linkPos(ForgeDirection.getOrientation(i + 2)),
                        FusionLink.PLASMA_IN) instanceof IFusionPowerReceiver receiver) {
                    receiver.receiveFusionPower(share, flux, r, g, b);
                }
            }
        }

        te.klystronEnergySync = te.klystronEnergy;
        te.setChanged();
        te.syncState();
        te.klystronEnergy = 0;
    }

    private AudioWrapper audio;

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFusionTorus te) {
        double powerFactor = getSpeedScaled(te.getMaxPower(), te.power);
        if (te.didProcess) te.magnetSpeed += MAGNET_ACCELERATION;
        else te.magnetSpeed -= MAGNET_ACCELERATION;
        te.magnetSpeed = Mth.clamp(te.magnetSpeed, 0F, 30F * (float) powerFactor);

        te.prevMagnet = te.magnet;
        te.magnet += te.magnetSpeed;
        if (te.magnet >= 360F) {
            te.magnet -= 360F;
            te.prevMagnet -= 360F;
        }

        float speed = te.magnetSpeed / 30F;
        te.audio = MachineAudio.tick(te.audio, te.magnetSpeed > 0F, ModSounds.FUSION_REACTOR_RUNNING.get(),
                SoundSource.BLOCKS, pos.getX() + 0.5F, pos.getY() + 2.5F, pos.getZ() + 0.5F,
                te.getVolume(speed), 30F, speed, 20);
    }

    /** A fill fraction, but never zero while anything is in the buffer — the reactor idles, not stalls. */
    public static double getSpeedScaled(long max, long current) {
        if (max <= 0) return 0D;
        return Math.min(1D, (double) current / (double) max);
    }

    /**
     * How much of the plasma output each receiver gets. Sharing is deliberately lossy per receiver but
     * a net gain overall: one receiver takes 100%, two take 62.5% each (125% total), three 50% (150%),
     * four or more 43.75% (175%).
     */
    public static double getOutputIntensity(int receiverCount) {
        if (receiverCount == 1) return 1D;
        if (receiverCount == 2) return 0.625D;
        if (receiverCount == 3) return 0.5D;
        return 0.4375D;
    }

    // --- network nodes: one per arm, seven blocks out at the torus's mid height ---

    private BlockPos nodePos(ForgeDirection dir) {
        return new BlockPos(worldPosition.getX() + dir.offsetX * 7, worldPosition.getY() + 2,
                worldPosition.getZ() + dir.offsetZ * 7);
    }

    /** Where a klystron or a consumer meets this arm: one block beyond the position this torus claims. */
    private BlockPos linkPos(ForgeDirection dir) {
        return new BlockPos(worldPosition.getX() + dir.offsetX * 8, worldPosition.getY() + 2,
                worldPosition.getZ() + dir.offsetZ * 8);
    }

    @Override
    public void setRemoved() {
        for (int i = 0; i < 4; i++) FusionLink.release(level, nodePos(ForgeDirection.getOrientation(i + 2)));
        super.setRemoved();
    }

    /**
     * Where pipes attach: <b>vertically</b>, under the bottom ring and over the top one.
     *
     * <p>This is not a stylistic choice. The reactor's mask reaches a full seven blocks on both
     * horizontal axes, so a sideways port would put the pipe inside the machine's own mass — there is
     * simply no room out to the side. Upstream routes every connection up or down for that reason, and
     * the ports sit above/below the promoted cells at &plusmn;6 plus the centre column.
     */
    public List<DirPos> getConPos() {
        List<DirPos> ports = new ArrayList<>();
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        // The centre column, through the core itself at the bottom and the crown cell at the top.
        ports.add(new DirPos(x, y - 1, z, ForgeDirection.DOWN));
        ports.add(new DirPos(x, y + 5, z, ForgeDirection.UP));

        for (int off : new int[]{0, 2, -2}) {
            for (int side : new int[]{6, -6}) {
                ports.add(new DirPos(x + side, y - 1, z + off, ForgeDirection.DOWN));
                ports.add(new DirPos(x + side, y + 5, z + off, ForgeDirection.UP));
                ports.add(new DirPos(x + off, y - 1, z + side, ForgeDirection.DOWN));
                ports.add(new DirPos(x + off, y + 5, z + side, ForgeDirection.UP));
            }
        }
        return ports;
    }

    @Override public boolean hasPermission(Player player) {
        return player.containerMenu instanceof com.hbm.inventory.container.ContainerFusionTorus menu
                && menu.getTorus() == this && menu.stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (dev.golem.core.data.Tags.getIntOr(data, "index", -1) != 0) return;
        String selection = dev.golem.core.data.Tags.getStringOr(data, "selection", "null");
        fusionModule.setRecipe(com.hbm.inventory.recipes.FusionRecipes.INSTANCE.getRecipe(selection) != null
                ? selection : "null", false);
        fusionModule.progress = 0D;
        fusionModule.setupTanks(fusionModule.getRecipe());
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        if (slot == SLOT_BLUEPRINT) return true;
        return false;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(didProcess);
        buf.writeLong(klystronEnergySync);
        buf.writeLong(plasmaEnergy);
        buf.writeDouble(fuelConsumption);
        fusionModule.serialize(buf);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
        for (boolean connection : connections) buf.writeBoolean(connection);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        didProcess = buf.readBoolean();
        klystronEnergy = buf.readLong();
        plasmaEnergy = buf.readLong();
        fuelConsumption = buf.readDouble();
        fusionModule.deserialize(buf);
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
        for (int i = 0; i < connections.length; i++) connections[i] = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "ft" + i);
        fusionModule.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "ft" + i);
        fusionModule.load(input);
    }

    @Override protected boolean syncFullTagOnChunkLoad() { return true; }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "plasma", PREFIX_VALUE + "consumption"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "plasma").equals(name)) return "" + plasmaEnergy;
        if ((PREFIX_VALUE + "consumption").equals(name)) return "" + (int) (fuelConsumption * 100);
        return null;
    }

    @Override public FluidTankNTM[] getReceivingTanks() {
        return new FluidTankNTM[]{coolantTanks[0], tanks[0], tanks[1], tanks[2]};
    }

    @Override public FluidTankNTM[] getSendingTanks() {
        return new FluidTankNTM[]{coolantTanks[1], tanks[3]};
    }

    @Override public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[]{coolantTanks[0], coolantTanks[1], tanks[0], tanks[1], tanks[2], tanks[3]};
    }
}
