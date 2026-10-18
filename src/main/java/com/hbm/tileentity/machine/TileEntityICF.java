package com.hbm.tileentity.machine;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.container.ContainerICF;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.items.ModIcfItems;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemICFPellet;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.tileentity.ModIcfBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm.lib.Library;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * The ICF reactor's block entity (1.7 {@code TileEntityICF}). Slots: 0-4 fresh pellets in, 5 the pellet in the
 * chamber, 6-10 depleted pellets out, 11 the coolant's fluid identifier. Tanks: coolant in (sodium by default),
 * its heated form out, and stellar flux out.
 *
 * <p>Every tick the lasers that hit the reactor add their power to {@link #laser}. If that meets the loaded
 * pellet's fusing difficulty the pellet reacts: its depletion grows by the laser power, the reaction multiplies
 * the laser power into heat, and stellar flux trickles out in proportion to the heat. A laser that cannot fuse
 * the pellet still heats the chamber by a quarter of its power. The coolant then takes away up to a quarter of
 * the heat per tick (scaled by the fluid's ICF efficiency), and the rest decays by 0.1 %.
 *
 * <p>1.7's OpenComputers component is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.machine.ICFPeripheral}; its Energy Control readout has no target here.
 */
public class TileEntityICF extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer, IRORValueProvider {

    public static final int SLOTS = 12;

    public long laser;
    public long maxLaser;
    private long laserSync;
    private long maxLaserSync;
    public long heat;
    public static final long maxHeat = 1_000_000_000_000L;

    /** Last tick's laser input and capacity, as the client sees them; the live pair is zeroed every tick. */
    public long laserSync() { return laserSync; }

    public long maxLaserSync() { return maxLaserSync; }

    public long heatup;
    public int consumption;
    public int output;

    public final FluidTankNTM[] tanks;

    public TileEntityICF(BlockPos pos, BlockState state) {
        super(ModIcfBlockEntities.ICF.get(), pos, state, SLOTS);
        this.tanks = new FluidTankNTM[3];
        this.tanks[0] = new FluidTankNTM(Fluids.SODIUM, 512_000);
        this.tanks[1] = new FluidTankNTM(Fluids.SODIUM_HOT, 512_000);
        this.tanks[2] = new FluidTankNTM(Fluids.STELLAR_FLUX, 24_000);
    }

    @Override
    public String getDefaultName() {
        return "container.machineICF";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    private ForgeDirection facing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockMultiblockCore.FACING)
                ? ForgeDirection.getOrientation(state.getValue(BlockMultiblockCore.FACING)) : ForgeDirection.NORTH;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityICF icf) {
        icf.tick(level, pos);
    }

    /** One reactor tick, split out so a test can drive it with a chosen laser power. */
    public void tick(Level level, BlockPos pos) {
        this.setupFluidPorts(getPorts());
        this.updatePortFIFO();

        if (tanks[0].setType(inventory, 11, 11)) setChanged();

        boolean markDirty = false;

        //eject depleted pellet
        ItemStack chamber = inventory.getStackInSlot(5);
        if (!chamber.isEmpty() && chamber.getItem() == ModItems.icf_pellet_depleted.get()) {
            for (int i = 6; i < 11; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, chamber.copy());
                    inventory.setStackInSlot(5, ItemStack.EMPTY);
                    markDirty = true;
                    break;
                }
            }
        }

        //insert fresh pellet
        if (inventory.getStackInSlot(5).isEmpty()) {
            for (int i = 0; i < 5; i++) {
                ItemStack fresh = inventory.getStackInSlot(i);
                if (!fresh.isEmpty() && fresh.getItem() == ModIcfItems.icf_pellet.get()) {
                    inventory.setStackInSlot(5, fresh.copy());
                    inventory.setStackInSlot(i, ItemStack.EMPTY);
                    markDirty = true;
                    break;
                }
            }
        }

        this.heatup = 0;

        ItemStack pellet = inventory.getStackInSlot(5);
        if (!pellet.isEmpty() && pellet.getItem() == ModIcfItems.icf_pellet.get()) {
            if (ItemICFPellet.getFusingDifficulty(pellet) <= this.laser) {
                this.heatup = ItemICFPellet.react(pellet, this.laser);
                this.heat += heatup;
                if (ItemICFPellet.getDepletion(pellet) >= ItemICFPellet.getMaxDepletion(pellet)) {
                    inventory.setStackInSlot(5, new ItemStack(ModItems.icf_pellet_depleted.get()));
                    markDirty = true;
                }

                if (level.getGameTime() % 20 == 15) {
                    SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                            SatelliteRayScan.RayEvent.INFO_PARTICLE, 200);
                }

                tanks[2].setFill(tanks[2].getFill() + (int) Math.ceil(this.heat * 10D / maxHeat));
                if (tanks[2].getFill() > tanks[2].getMaxFill()) tanks[2].setFill(tanks[2].getMaxFill());

                // 1.7 AuxParticlePacketNT "hadron" to everyone within 25 blocks
                HbmEffectNT.Hadron.send(level, pos.getX() + 0.5, pos.getY() + 3.5, pos.getZ() + 0.5, null, 25);
            }
        }

        if (heatup == 0) {
            this.heat += this.laser * 0.25D;
        }

        this.consumption = 0;
        this.output = 0;

        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait != null) {
            HeatingStep step = trait.getFirstStep();
            tanks[1].setTankType(step.typeProduced);

            int coolingCycles = tanks[0].getFill() / step.amountReq;
            int heatingCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
            int heatCycles = (int) Math.min(this.heat / 4D / step.heatReq * trait.getEfficiency(HeatingType.ICF), this.heat / step.heatReq); //25% cooling per tick
            int cycles = Math.min(coolingCycles, Math.min(heatingCycles, heatCycles));

            tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
            tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
            this.heat -= step.heatReq * cycles; // int product, as 1.7 wrote it

            this.consumption = step.amountReq * cycles;
            this.output = step.amountProduced * cycles;
        }

        this.heat *= 0.999D;
        if (this.heat > maxHeat) this.heat = maxHeat;
        if (markDirty) this.setChanged();

        this.networkPackNT();
        this.laserSync = laser;
        this.maxLaserSync = maxLaser;
        this.laser = 0;
        this.maxLaser = 0;
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = facing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x, y + 5, z, Library.POS_Y),
                    PortDef.make(x, y, z, Library.NEG_Y),
                    PortDef.make(x + dir.offsetX * 2 + rot.offsetX * 6, y + 3, z + dir.offsetZ * 2 + rot.offsetZ * 6, dir),
                    PortDef.make(x + dir.offsetX * 2 - rot.offsetX * 6, y + 3, z + dir.offsetZ * 2 - rot.offsetZ * 6, dir),
                    PortDef.make(x - dir.offsetX * 2 + rot.offsetX * 6, y + 3, z - dir.offsetZ * 2 + rot.offsetZ * 6, dir.getOpposite()),
                    PortDef.make(x - dir.offsetX * 2 - rot.offsetX * 6, y + 3, z - dir.offsetZ * 2 - rot.offsetZ * 6, dir.getOpposite()),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    // --- sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(laser);
        buf.writeLong(maxLaser);
        buf.writeLong(heat);
        for (int i = 0; i < 3; i++) tanks[i].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.laser = buf.readLong();
        this.maxLaser = buf.readLong();
        this.heat = buf.readLong();
        for (int i = 0; i < 3; i++) tanks[i].deserialize(buf);
    }

    // --- inventory ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 5 && stack.getItem() == ModIcfItems.icf_pellet.get();
    }

    public static final int[] io = new int[]{0, 1, 2, 3, 4, 6, 7, 8, 9, 10};

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return io;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return io;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot > 5;
    }

    /** 1.7 {@code isUseableByPlayer}: within 16 blocks of the core. */
    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 256;
    }

    // --- persistence ---

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < 3; i++) tanks[i].readFromNBT(input, "t" + i);
        this.heat = input.getLongOr("heat", 0L);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < 3; i++) tanks[i].writeToNBT(output, "t" + i);
        output.putLong("heat", heat);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerICF(containerId, playerInventory, this);
    }

    // --- fluid ---

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "laser", PREFIX_VALUE + "heat", PREFIX_VALUE + "coldfluid",
                PREFIX_VALUE + "hotfluid", PREFIX_VALUE + "stellarflux"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "laser").equals(name)) return "" + laser;
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heat;
        if ((PREFIX_VALUE + "coldfluid").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "hotfluid").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "stellarflux").equals(name)) return "" + tanks[2].getFill();
        return null;
    }
}
