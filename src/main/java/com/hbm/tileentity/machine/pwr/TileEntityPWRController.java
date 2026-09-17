package com.hbm.tileentity.machine.pwr;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModHazardBlocks;
import com.hbm.blocks.machine.pwr.ModPWRBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerPWR;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.Tags;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The pressurised water reactor's brain. Assembly hands it the block map it flood-filled; from that it
 * counts the parts once and keeps the derived numbers, so the running reactor never walks the structure
 * again — it only steps two heat pools and one flux figure per tick.
 *
 * <p>Flux is a whole-core figure: every fuel rod sees the same share of it, gives back what its curve
 * says, and the total becomes next tick's flux. Rods are coupled to each other through the neighbour
 * search done at assembly time, and a control block between two rods moves that pair's contribution
 * behind the control level, which is why withdrawing rods scales the reaction rather than gating it.
 *
 * <p>Deviation from CE: the OpenComputers component is absent.
 */
public class TileEntityPWRController extends TileEntityMachineBase
        implements MenuProvider, IControlReceiver, WorldlyContainer, IFluidStandardTransceiverMK2,
        IRORValueProvider, IRORInteractive {

    public static final long coreHeatCapacityBase = 10_000_000;
    public static final long hullHeatCapacityBase = 10_000_000;

    public FluidTankNTM[] tanks;
    public long coreHeat;
    public long coreHeatCapacity = coreHeatCapacityBase;
    public long hullHeat;
    public double flux;

    public double rodLevel = 100;
    public double rodTarget = 100;

    public int typeLoaded = -1;
    public int amountLoaded;
    public double progress;
    public double processTime;

    public int rodCount;
    public int connections;
    public int connectionsControlled;
    public int heatexCount;
    public int heatsinkCount;
    public int channelCount;
    public int sourceCount;

    public int unloadDelay;
    public boolean assembled;

    protected List<BlockPos> ports = new ArrayList<>();
    protected List<BlockPos> rods = new ArrayList<>();

    public TileEntityPWRController(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PWR_CONTROLLER.get(), pos, state, 3);
        this.tanks = new FluidTankNTM[2];
        this.tanks[0] = new FluidTankNTM(Fluids.COOLANT, 128_000);
        this.tanks[1] = new FluidTankNTM(Fluids.COOLANT_HOT, 128_000);
    }

    @Override
    public String getDefaultName() {
        return "container.pwrController";
    }

    /**
     * The one-time survey of the assembled structure: counts each part kind, then pairs up every fuel
     * rod with the rods and reflectors it can see along the six axes. A reflector counts double because
     * the neutrons bounce back to the same rod; the running total is halved because each pair is found
     * from both ends.
     */
    public void setup(Map<BlockPos, BlockState> partMap, Map<BlockPos, BlockState> rodMap) {
        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        ports.clear();
        rods.clear();

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (Map.Entry<BlockPos, BlockState> entry : partMap.entrySet()) {
            var block = entry.getValue().getBlock();

            if (block == ModPWRBlocks.pwr_fuelrod.get()) rodCount++;
            if (block == ModPWRBlocks.pwr_heatex.get()) heatexCount++;
            if (block == ModPWRBlocks.pwr_channel.get()) channelCount++;
            if (block == ModPWRBlocks.pwr_heatsink.get()) heatsinkCount++;
            if (block == ModPWRBlocks.pwr_neutron_source.get()) sourceCount++;
            if (block == ModPWRBlocks.pwr_port.get()) ports.add(entry.getKey());
        }

        for (Map.Entry<BlockPos, BlockState> entry : rodMap.entrySet()) {
            BlockPos fuelPos = entry.getKey();
            rods.add(fuelPos);

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                boolean controlled = false;

                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.offset(dir.offsetX * i, dir.offsetY * i, dir.offsetZ * i);
                    BlockState stateAtPos = partMap.get(checkPos);
                    var atPos = stateAtPos != null ? stateAtPos.getBlock() : null;

                    if (atPos == null || atPos == ModPWRBlocks.pwr_casing.get()) break;
                    if (atPos == ModPWRBlocks.pwr_control.get()) controlled = true;
                    if (atPos == ModPWRBlocks.pwr_fuelrod.get()) {
                        if (controlled) connectionsControlledDouble++;
                        else connectionsDouble++;
                        break;
                    }
                    if (atPos == ModPWRBlocks.pwr_reflector.get()) {
                        if (controlled) connectionsControlledDouble += 2;
                        else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);

        this.coreHeatCapacity = coreHeatCapacityBase + this.heatsinkCount * (coreHeatCapacityBase / 20);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPWRController pwr) {
        pwr.tickServer(level);
    }

    protected void tickServer(Level level) {
        if (this.amountLoaded <= 0) setTypeFromFluidId();
        setupTanks();

        if (unloadDelay > 0) unloadDelay--;

        if (!chunksLoaded(level)) this.unloadDelay = 60;

        if (this.assembled) {
            for (DirPos connection : getConPos()) {
                if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, connection);
            }

            if (this.unloadDelay <= 0) {
                runReaction(level);
            } else {
                this.hullHeat = 0;
                this.coreHeat = 0;
            }
        }

        networkPackNT();
    }

    private void runReaction(Level level) {
        ItemStack rodStack = inventory.getStackInSlot(0);
        ItemStack rodHotStack = inventory.getStackInSlot(1);

        if ((typeLoaded == -1 || amountLoaded <= 0) && rodStack.getItem() instanceof ItemPWRFuel fuel) {
            typeLoaded = fuel.getVariant(rodStack);
            amountLoaded++;
            rodStack.shrink(1);
            setChanged();
        } else if (rodStack.getItem() instanceof ItemPWRFuel fuel
                && fuel.getVariant(rodStack) == typeLoaded && amountLoaded < rodCount) {
            amountLoaded++;
            rodStack.shrink(1);
            setChanged();
        }

        double diff = this.rodLevel - this.rodTarget;
        if (diff < 1 && diff > -1) this.rodLevel = this.rodTarget;
        if (this.rodTarget > this.rodLevel) this.rodLevel++;
        if (this.rodTarget < this.rodLevel) this.rodLevel--;

        double multiplier = 1D;
        FT_PWRModerator moderator = tanks[0].getTankType().getTrait(FT_PWRModerator.class);
        if (moderator != null) multiplier = moderator.getMultiplier();

        int newFlux = this.sourceCount * 20;

        if (typeLoaded != -1 && amountLoaded > 0) {
            EnumPWRFuel fuel = EnumPWRFuel.VALUES[Math.floorMod(typeLoaded, EnumPWRFuel.VALUES.length)];
            double usedRods = getTotalProcessMultiplier();
            double fluxPerRod = this.rodCount > 0 ? this.flux / this.rodCount : 0;
            double outputPerRod = fuel.function.effonix(fluxPerRod);
            double totalOutput = outputPerRod * amountLoaded * usedRods;
            double totalHeatOutput = totalOutput * fuel.heatEmission;

            if (tanks[0].getFill() > 0) totalHeatOutput *= multiplier;

            this.coreHeat += (long) totalHeatOutput;
            newFlux += (int) totalOutput;

            this.processTime = (int) fuel.yield;
            this.progress += totalOutput;

            if (this.progress >= this.processTime) {
                this.progress -= this.processTime;

                if (rodHotStack.isEmpty()) {
                    inventory.setStackInSlot(1, ModPWRBlocks.pwr_fuel_hot.get().of(fuel));
                } else if (rodHotStack.getItem() == ModPWRBlocks.pwr_fuel_hot.get()
                        && ModPWRBlocks.pwr_fuel_hot.get().getVariant(rodHotStack) == typeLoaded
                        && rodHotStack.getCount() < rodHotStack.getMaxStackSize()) {
                    rodHotStack.grow(1);
                }

                this.amountLoaded--;
                setChanged();
            }

            if (level.getGameTime() % 100 == 0) {
                BlockPos pos = getBlockPos();
                SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                        SatelliteRayScan.RayEvent.INFO_NUCLEAR, 200);
            }
        }

        if (this.amountLoaded <= 0) this.typeLoaded = -1;
        if (amountLoaded > rodCount) amountLoaded = rodCount;

        double coreCoolingApproachNum = getXOverE((double) this.heatexCount * 5 / getRodCountForCoolant(), 2) / 2D;
        long averageCoreHeat = (this.coreHeat + this.hullHeat) / 2;
        this.coreHeat -= (long) ((coreHeat - averageCoreHeat) * coreCoolingApproachNum);
        this.hullHeat -= (long) ((hullHeat - averageCoreHeat) * coreCoolingApproachNum);

        updateCoolant();

        this.coreHeat *= 0.999D;
        this.hullHeat *= 0.999D;

        this.flux = newFlux;
        if (tanks[0].getFill() > 0) this.flux *= multiplier;

        if (this.coreHeat > this.coreHeatCapacity) meltDown(level);
    }

    /**
     * Upstream widens the check to the four chunks two out from the controller, so a reactor whose far
     * edge has fallen out of the loaded area idles instead of running on a structure half the server
     * cannot see.
     */
    private boolean chunksLoaded(Level level) {
        BlockPos pos = getBlockPos();
        for (int dx = -32; dx <= 32; dx += 32) {
            for (int dz = -32; dz <= 32; dz += 32) {
                if (!level.hasChunkAt(pos.offset(dx, 0, dz))) return false;
            }
        }
        return true;
    }

    public DirPos[] getConPos() {
        if (!this.assembled || ports.isEmpty()) return new DirPos[0];
        DirPos[] result = new DirPos[ports.size() * 6];
        int idx = 0;
        for (BlockPos portPos : ports) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                result[idx++] = new DirPos(portPos.getX() + dir.offsetX, portPos.getY() + dir.offsetY,
                        portPos.getZ() + dir.offsetZ, dir);
            }
        }
        return result;
    }

    /** Turns the whole fuel column to corium and detonates over the rods' centre of mass. */
    protected void meltDown(Level level) {
        level.removeBlock(getBlockPos(), false);

        if (rods.isEmpty()) return;

        double x = 0, y = 0, z = 0;

        for (BlockPos rodPos : this.rods) {
            level.setBlockAndUpdate(rodPos, ModHazardBlocks.block_corium.get().defaultBlockState());
            x += rodPos.getX() + 0.5;
            y += rodPos.getY() + 0.5;
            z += rodPos.getZ() + 0.5;
        }

        x /= rods.size();
        y /= rods.size();
        z /= rods.size();

        level.explode(null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
    }

    /**
     * Moves heat out of the hull and into the coolant. The cooling fraction is how well the coolant
     * channels keep up with the rods, capped at one, so past parity extra channels buy nothing.
     */
    private void updateCoolant() {
        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        double coolingEff = (double) this.channelCount / getRodCountForCoolant() * 0.1D;
        if (coolingEff > 1D) coolingEff = 1D;

        long heatToUse = Math.min(Math.min(this.hullHeat,
                (long) (this.hullHeat * coolingEff * trait.getEfficiency(HeatingType.PWR))), 2_000_000_000L);
        HeatingStep step = trait.getFirstStep();
        if (step.amountReq <= 0 || step.heatReq <= 0) return;

        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        int heatCycles = (int) (heatToUse / step.heatReq);
        int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));

        this.hullHeat -= (long) step.heatReq * cycles;
        this.tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
        this.tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
    }

    protected int getRodCountForCoolant() {
        return this.rodCount + (int) Math.ceil(this.heatsinkCount / 4D);
    }

    private void setupTanks() {
        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) {
            tanks[0].setTankType(Fluids.NONE);
            tanks[1].setTankType(Fluids.NONE);
            return;
        }
        tanks[1].setTankType(trait.getFirstStep().typeProduced);
    }

    /** Slot 2 fluid identifier retypes the coolant tank (upstream's {@code tank.setType(2, inventory)}). */
    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(2);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType type = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (type != tanks[0].getTankType()) {
            tanks[0].setTankType(type);
            setChanged();
        }
    }

    public double getTotalProcessMultiplier() {
        double totalConnections = this.connections + this.connectionsControlled * (1D - (this.rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    /** Diminishing returns on rod coupling: near-linear while small, a tenth of that once past ~300. */
    public double connectinFunc(double connections) {
        return connections / 10D * (1D - getXOverE(connections, 300D)) + connections / 150D * getXOverE(connections, 300D);
    }

    public double getXOverE(double x, double d) {
        return 1 - Math.pow(Math.E, -x / d);
    }

    // --- inventory ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ItemPWRFuel;
        if (slot == 2) return stack.getItem() instanceof IItemFluidId;
        return false;
    }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return new int[]{0, 1};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == 1;
    }

    // --- fluid ---

    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return type == tanks[0].getTankType() || type == tanks[1].getTankType();
    }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }

    // --- sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(this.assembled);
        buf.writeInt(this.rodCount);
        buf.writeLong(this.coreHeat);
        buf.writeLong(this.hullHeat);
        buf.writeDouble(this.flux);
        buf.writeDouble(this.processTime);
        buf.writeDouble(this.progress);
        buf.writeInt(this.typeLoaded);
        buf.writeInt(this.amountLoaded);
        buf.writeDouble(this.rodLevel);
        buf.writeDouble(this.rodTarget);
        buf.writeLong(this.coreHeatCapacity);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.assembled = buf.readBoolean();
        this.rodCount = buf.readInt();
        this.coreHeat = buf.readLong();
        this.hullHeat = buf.readLong();
        this.flux = buf.readDouble();
        this.processTime = buf.readDouble();
        this.progress = buf.readDouble();
        this.typeLoaded = buf.readInt();
        this.amountLoaded = buf.readInt();
        this.rodLevel = buf.readDouble();
        this.rodTarget = buf.readDouble();
        this.coreHeatCapacity = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    // --- persistence ---

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
        output.putBoolean("assembled", assembled);
        output.putLong("coreHeat", coreHeat);
        output.putLong("hullHeat", hullHeat);
        output.putDouble("flux", flux);
        output.putDouble("rodLevel", rodLevel);
        output.putDouble("rodTarget", rodTarget);
        output.putInt("typeLoaded", typeLoaded);
        output.putInt("amountLoaded", amountLoaded);
        output.putDouble("progress", progress);
        output.putDouble("processTime", processTime);
        output.putLong("coreHeatCapacity", coreHeatCapacity);

        output.putInt("rodCount", rodCount);
        output.putInt("connections", connections);
        output.putInt("connectionsControlled", connectionsControlled);
        output.putInt("heatexCount", heatexCount);
        output.putInt("channelCount", channelCount);
        output.putInt("sourceCount", sourceCount);
        output.putInt("heatsinkCount", heatsinkCount);

        writePositions(output, "ports", ports);
        writePositions(output, "rods", rods);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
        assembled = input.getBooleanOr("assembled", false);
        coreHeat = input.getLongOr("coreHeat", 0L);
        hullHeat = input.getLongOr("hullHeat", 0L);
        flux = input.getDoubleOr("flux", 0D);
        rodLevel = input.getDoubleOr("rodLevel", 100D);
        rodTarget = input.getDoubleOr("rodTarget", 100D);
        typeLoaded = input.getIntOr("typeLoaded", -1);
        amountLoaded = input.getIntOr("amountLoaded", 0);
        progress = input.getDoubleOr("progress", 0D);
        processTime = input.getDoubleOr("processTime", 0D);
        coreHeatCapacity = Math.max(input.getLongOr("coreHeatCapacity", coreHeatCapacityBase), coreHeatCapacityBase);

        rodCount = input.getIntOr("rodCount", 0);
        connections = input.getIntOr("connections", 0);
        connectionsControlled = input.getIntOr("connectionsControlled", 0);
        heatexCount = input.getIntOr("heatexCount", 0);
        channelCount = input.getIntOr("channelCount", 0);
        sourceCount = input.getIntOr("sourceCount", 0);
        heatsinkCount = input.getIntOr("heatsinkCount", 0);

        ports = readPositions(input, "ports");
        rods = readPositions(input, "rods");
    }

    private static void writePositions(DataOut output, String name, List<BlockPos> positions) {
        DataOut.ChildList list = output.childrenList(name);
        for (BlockPos pos : positions) {
            DataOut child = list.addChild();
            child.putInt("x", pos.getX());
            child.putInt("y", pos.getY());
            child.putInt("z", pos.getZ());
        }
    }

    private static List<BlockPos> readPositions(DataIn input, String name) {
        List<BlockPos> positions = new ArrayList<>();
        for (DataIn child : input.childrenListOrEmpty(name)) {
            positions.add(new BlockPos(child.getIntOr("x", 0), child.getIntOr("y", 0), child.getIntOr("z", 0)));
        }
        return positions;
    }

    // --- GUI ---

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerPWR(id, playerInventory, this);
    }

    @Override
    public boolean hasPermission(Player player) {
        return stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (Tags.hasInt(data, "control")) {
            this.rodTarget = Mth.clamp(Tags.getIntOr(data, "control", 100), 0, 100);
            setChanged();
        }
    }

    // --- redstone over radio ---

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "rods", PREFIX_VALUE + "coreheat", PREFIX_VALUE + "hullheat",
                PREFIX_VALUE + "flux", PREFIX_VALUE + "depletion",
                PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent", PREFIX_FUNCTION + "jettison"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rods").equals(name)) return "" + (int) this.rodLevel;
        if ((PREFIX_VALUE + "coreheat").equals(name)) return "" + this.coreHeat;
        if ((PREFIX_VALUE + "hullheat").equals(name)) return "" + this.hullHeat;
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) this.flux;
        if ((PREFIX_VALUE + "depletion").equals(name)) {
            return "" + (this.processTime == 0D ? 0 : (int) (this.progress * 100 / this.processTime));
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            this.rodTarget = IRORInteractive.parseInt(params[0], 0, 100);
            setChanged();
            return null;
        }

        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0;
            setChanged();
            return null;
        }

        return null;
    }
}
