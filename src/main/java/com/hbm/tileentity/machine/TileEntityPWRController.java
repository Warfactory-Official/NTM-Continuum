package com.hbm.tileentity.machine;

import dev.golem.core.world.Explosions;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModFluidNetBlocks;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm.inventory.fluid.trait.FT_Heatable.HeatingType;
import com.hbm.inventory.fluid.trait.FT_PWRModerator;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.ForgeDirection;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.hbm.blocks.ModPwrBlocks;
import com.hbm.blocks.ModPwrContent;
import com.hbm.inventory.container.ContainerPWR;
import com.hbm.items.ModPwrItems;
import com.hbm.items.machine.ItemPWRFuel;
import com.hbm.items.machine.ItemPWRPrinter;
import com.hbm.tileentity.ModPwrBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * The PWR controller's block entity (1.7 {@code TileEntityPWRController}): the reactor simulation and its
 * GUI state. Slots: 0 fresh fuel in, 1 hot fuel out, 2 fluid identifier for the coolant tank.
 *
 * <p>The tick is 1.7's, line for line: rods load one per tick while the loaded type matches; the control
 * rod level walks one percent per tick toward its target; the flux is 20 per neutron source plus the fuel's
 * reaction to last tick's flux per rod, scaled by the connection efficiency and the moderator; heat moves
 * core→hull through the heat exchangers, hull→coolant through the channels, both decay by 0.1 % a tick; the
 * core melts down past its capacity. Fission only runs once the area has been loaded for 40 ticks.
 *
 * <p>1.7's OpenComputers component is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.machine.PWRControllerPeripheral}.
 */
public class TileEntityPWRController extends TileEntityMachineBase
        implements MenuProvider, IControlReceiver, IFluidStandardTransceiverMK2, IRORValueProvider, IRORInteractive {

    public static final long coreHeatCapacityBase = 10_000_000;
    public static final long hullHeatCapacityBase = 10_000_000;

    public final FluidTankNTM[] tanks;
    public long coreHeat;
    public long coreHeatCapacity = coreHeatCapacityBase;
    public long hullHeat;
    public double flux;

    public double rodLevel = 100;
    public double rodTarget = 100;

    /**
     * The loaded fuel item as its runtime registry number (synced to the GUI; -1 none). Saved by item name: any item with
     * an {@code hbm:pwr_fuel} entry can be loaded, an addon's too.
     */
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

    public int unloadDelay = 0;
    public boolean assembled;

    /** Set by the printer; the next sync packet carries the structure snapshot instead of the reactor state. */
    public boolean isPrinting;

    private @Nullable AudioWrapper audio;

    public PortDef[] ports = new PortDef[0];
    protected final List<BlockPos> rods = new ArrayList<>();

    public TileEntityPWRController(BlockPos pos, BlockState state) {
        super(ModPwrBlockEntities.PWR_CONTROLLER.get(), pos, state, 3);
        this.tanks = new FluidTankNTM[2];
        this.tanks[0] = new FluidTankNTM(Fluids.COOLANT, 128_000);
        this.tanks[1] = new FluidTankNTM(Fluids.COOLANT_HOT, 128_000);
    }

    @Override
    public String getDefaultName() {
        return "container.pwrController";
    }

    /** The initial creation of the reactor, does all the pre-calculation and whatnot. */
    public void setup(Map<BlockPos, Block> partMap, Map<BlockPos, Block> rodMap) {
        Block fuel = ModPwrBlocks.PWR_FUEL.get();
        Block heatex = ModPwrBlocks.PWR_HEATEX.get();
        Block channel = ModPwrBlocks.PWR_CHANNEL.get();
        Block heatsink = ModPwrBlocks.PWR_HEATSINK.get();
        Block source = ModPwrBlocks.PWR_NEUTRON_SOURCE.get();
        Block port = ModPwrBlocks.PWR_PORT.get();
        Block casing = ModPwrBlocks.PWR_CASING.get();
        Block control = ModPwrBlocks.PWR_CONTROL.get();
        Block reflector = ModPwrBlocks.PWR_REFLECTOR.get();

        rodCount = 0;
        connections = 0;
        connectionsControlled = 0;
        heatexCount = 0;
        channelCount = 0;
        heatsinkCount = 0;
        sourceCount = 0;
        List<PortDef> portList = new ArrayList<>();
        rods.clear();

        int connectionsDouble = 0;
        int connectionsControlledDouble = 0;

        for (Map.Entry<BlockPos, Block> entry : partMap.entrySet()) {
            Block block = entry.getValue();
            if (block == fuel) rodCount++;
            if (block == heatex) heatexCount++;
            if (block == channel) channelCount++;
            if (block == heatsink) heatsinkCount++;
            if (block == source) sourceCount++;
            if (block == port) portList.add(PortDef.make(entry.getKey().immutable(), ForgeDirection.VALID_DIRECTIONS));
        }

        for (Map.Entry<BlockPos, Block> entry : rodMap.entrySet()) {
            BlockPos fuelPos = entry.getKey().immutable();
            rods.add(fuelPos);

            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                boolean controlled = false;

                for (int i = 1; i < 16; i++) {
                    BlockPos checkPos = fuelPos.offset(dir.offsetX * i, dir.offsetY * i, dir.offsetZ * i);
                    Block atPos = partMap.get(checkPos);
                    if (atPos == null || atPos == casing) break;
                    if (atPos == control) controlled = true;
                    if (atPos == fuel) {
                        if (controlled) connectionsControlledDouble++;
                        else connectionsDouble++;
                        break;
                    }
                    if (atPos == reflector) {
                        if (controlled) connectionsControlledDouble += 2;
                        else connectionsDouble += 2;
                        break;
                    }
                }
            }
        }

        this.ports = portList.toArray(new PortDef[0]);
        connections = connectionsDouble / 2;
        connectionsControlled = connectionsControlledDouble / 2;
        heatsinkCount = Math.min(heatsinkCount, 80);

        // int64: after 2127 heatsinks the capacity exceeds int32, well within the 4000+ block limit
        this.coreHeatCapacity = coreHeatCapacityBase + this.heatsinkCount * (coreHeatCapacityBase / 20);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPWRController te) {
        te.updateTypeFromFluidId();
        te.setupTanks();

        if (te.unloadDelay > 0) te.unloadDelay--;

        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;

        // since fluid sources are often not within 1 chunk, we just do 2 chunks distance and call it a day
        if (!level.hasChunk(chunkX, chunkZ)
                || !level.hasChunk(chunkX + 2, chunkZ + 2)
                || !level.hasChunk(chunkX + 2, chunkZ - 2)
                || !level.hasChunk(chunkX - 2, chunkZ + 2)
                || !level.hasChunk(chunkX - 2, chunkZ - 2)) {
            te.unloadDelay = 60;
        }

        if (te.assembled) {
            te.setupAllPorts(te.ports);
            te.updatePortFIFO();

            // only perform fission if the area has been loaded for 40 ticks or more
            if (te.unloadDelay <= 0) {
                te.runReactor(level, pos);
            } else {
                te.hullHeat = 0;
                te.coreHeat = 0;
            }
        } else {
            te.destroyAllPorts();
        }

        te.networkPackNT();
    }

    /** One fission step; split out of the tick so a test can drive it without the chunk-loading gate. */
    public void runReactor(Level level, BlockPos pos) {
        ItemStack input = inventory.getStackInSlot(0);
        int inputFuel = input.isEmpty() || ItemPWRFuel.stats(input.getItem()) == null ? -1
                : BuiltInRegistries.ITEM.getId(input.getItem());

        if ((typeLoaded == -1 || amountLoaded <= 0) && inputFuel != -1) {
            typeLoaded = inputFuel;
            amountLoaded++;
            inventory.extractItem(0, 1, false);
            this.markChanged();
        } else if (inputFuel != -1 && inputFuel == typeLoaded && amountLoaded < rodCount) {
            amountLoaded++;
            inventory.extractItem(0, 1, false);
            this.markChanged();
        }

        double diff = this.rodLevel - this.rodTarget;
        if (diff < 1 && diff > -1) this.rodLevel = this.rodTarget;
        if (this.rodTarget > this.rodLevel) this.rodLevel++;
        if (this.rodTarget < this.rodLevel) this.rodLevel--;

        double multiplier = 1D;
        FT_PWRModerator moderator = tanks[0].getTankType().getTrait(FT_PWRModerator.class);
        if (moderator != null) multiplier = moderator.getMultiplier();

        int newFlux = this.sourceCount * 20;

        ReactorDataMaps.PWRFuel fuel = typeLoaded == -1 ? null
                : ItemPWRFuel.stats(BuiltInRegistries.ITEM.byId(typeLoaded));
        if (fuel != null && amountLoaded > 0) {
            double usedRods = getTotalProcessMultiplier();
            double fluxPerRod = this.flux / this.rodCount;
            double outputPerRod = fuel.function().effonix(fluxPerRod);
            double totalOutput = outputPerRod * amountLoaded * usedRods;
            double totalHeatOutput = totalOutput * fuel.heat();

            if (tanks[0].getFill() > 0) totalHeatOutput *= multiplier;

            // compound long/int += double, exactly as 1.7 wrote it (the sum is truncated, not the addend)
            this.coreHeat += totalHeatOutput;
            newFlux += totalOutput;

            this.processTime = (int) fuel.yield();
            this.progress += totalOutput;

            if (this.progress >= this.processTime) {
                this.progress -= this.processTime;

                ItemStack out = inventory.getStackInSlot(1);
                Item hot = fuel.hotItem();
                if (out.isEmpty()) {
                    inventory.setStackInSlot(1, new ItemStack(hot, 1));
                } else if (out.getItem() == hot && out.getCount() < out.getMaxStackSize()) {
                    out.grow(1);
                    inventory.setStackInSlot(1, out);
                }

                this.amountLoaded--;
                this.markChanged();
            }
            // 1.7: a burning core shows up on the ray-scan satellite every 100 ticks.
            if (level.getGameTime() % 100 == 0) {
                SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                        SatelliteRayScan.RayEvent.INFO_NUCLEAR, 200);
            }
        }

        if (this.amountLoaded <= 0) this.typeLoaded = -1;
        if (amountLoaded > rodCount) amountLoaded = rodCount;

        /* CORE COOLING */
        double coreCoolingApproachNum = getXOverE((double) this.heatexCount * 5 / (double) getRodCountForCoolant(), 2) / 2D;
        long averageCoreHeat = (this.coreHeat + this.hullHeat) / 2;
        this.coreHeat -= (coreHeat - averageCoreHeat) * coreCoolingApproachNum;
        this.hullHeat -= (hullHeat - averageCoreHeat) * coreCoolingApproachNum;

        updateCoolant();

        this.coreHeat *= 0.999D;
        this.hullHeat *= 0.999D;

        this.flux = newFlux;
        if (tanks[0].getFill() > 0) this.flux *= multiplier;

        if (this.coreHeat > this.coreHeatCapacity) meltDown(level, pos);
    }

    /** 1.7 {@code meltDown}: the controller goes, every fuel rod becomes corium, and the core explodes. */
    protected void meltDown(Level level, BlockPos pos) {
        level.destroyBlock(pos, false);

        double x = 0;
        double y = 0;
        double z = 0;

        BlockState corium = ModFluidNetBlocks.corium_block.get().defaultBlockState();
        for (BlockPos rod : this.rods) {
            // 1.7 ran breakBlock (which re-set the fuel block) and then overwrote it with corium; same order.
            level.removeBlock(rod, false);
            level.setBlock(rod, corium, 3);

            x += rod.getX() + 0.5;
            y += rod.getY() + 0.5;
            z += rod.getZ() + 0.5;
        }

        if (rods.isEmpty()) return;
        x /= rods.size();
        y /= rods.size();
        z /= rods.size();

        Explosions.explode(level, null, x, y, z, 15F, true, Level.ExplosionInteraction.BLOCK);
    }

    /** Client side: the reactor loop plays while fuel is loaded (1.7 {@code updateEntity}'s remote branch). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityPWRController te) {
        if (te.amountLoaded > 0) {
            if (te.audio == null) {
                te.audio = te.createAudioLoop();
                if (te.audio != null) te.audio.startSound();
            } else if (!te.audio.isPlaying()) {
                te.audio = te.rebootAudio(te.audio);
            }
            if (te.audio != null) {
                te.audio.updateVolume(te.getVolume(1F));
                te.audio.keepAlive();
            }
        } else if (te.audio != null) {
            te.audio.stopSound();
            te.audio = null;
        }
    }

    public @Nullable AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(ModPwrContent.REACTOR_LOOP.get(), SoundSource.BLOCKS,
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 1F, 10F, 1.0F, 20);
    }

    private @Nullable AudioWrapper rebootAudio(AudioWrapper old) {
        old.stopSound();
        AudioWrapper fresh = createAudioLoop();
        if (fresh != null) fresh.startSound();
        return fresh;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (audio != null) {
            audio.stopSound();
            audio = null;
        }
    }

    protected void updateCoolant() {
        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) return;

        double coolingEff = (double) this.channelCount / (double) getRodCountForCoolant() * 0.1D; // 10% cooling if numbers match
        if (coolingEff > 1D) coolingEff = 1D;

        // no use in trying to convert everything to long since the internal tanks would never support it,
        // just cap the heat cycle count to prevent overflows in the math
        int heatToUse = (int) Math.min(Math.min(this.hullHeat, (long) (this.hullHeat * coolingEff * trait.getEfficiency(HeatingType.PWR))), 2_000_000_000);
        HeatingStep step = trait.getFirstStep();
        int coolCycles = tanks[0].getFill() / step.amountReq;
        int hotCycles = (tanks[1].getMaxFill() - tanks[1].getFill()) / step.amountProduced;
        int heatCycles = heatToUse / step.heatReq;
        int cycles = Math.min(coolCycles, Math.min(hotCycles, heatCycles));

        this.hullHeat -= step.heatReq * cycles;
        this.tanks[0].setFill(tanks[0].getFill() - step.amountReq * cycles);
        this.tanks[1].setFill(tanks[1].getFill() + step.amountProduced * cycles);
    }

    protected int getRodCountForCoolant() {
        return this.rodCount + (int) Math.ceil(this.heatsinkCount / 4D);
    }

    /** Slot 2 fluid identifier retypes the cold tank (1.7 {@code tanks[0].setType(2, slots)}), never while fuelled. */
    public void updateTypeFromFluidId() {
        if (amountLoaded <= 0 && tanks[0].setType(inventory, 2, 2)) setChanged();
    }

    public void setupTanks() {
        FT_Heatable trait = tanks[0].getTankType().getTrait(FT_Heatable.class);
        if (trait == null || trait.getEfficiency(HeatingType.PWR) <= 0) {
            tanks[0].setTankType(Fluids.NONE);
            tanks[1].setTankType(Fluids.NONE);
            return;
        }
        tanks[1].setTankType(trait.getFirstStep().typeProduced);
    }

    public double getTotalProcessMultiplier() {
        double totalConnections = this.connections + this.connectionsControlled * (1D - (this.rodLevel / 100D));
        return connectinFunc(totalConnections);
    }

    public double connectinFunc(double connections) {
        return connections / 10D * (1D - getXOverE(connections, 300D)) + connections / 150D * getXOverE(connections, 300D);
    }

    public double getXOverE(double x, double d) {
        return 1 - Math.pow(Math.E, -x / d);
    }

    // --- sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(isPrinting);
        if (isPrinting) {
            // piggybacking off of this packet so that we don't have to sync EVERY PWR block continuously to
            // the client for one tiny screenshot tool
            ItemPWRPrinter.serialize(buf);
            isPrinting = false;
            return;
        }

        super.serialize(buf);
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
        if (buf.readBoolean()) {
            if (level != null) ItemPWRPrinter.deserialize(level, buf);
            return;
        }

        super.deserialize(buf);
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

    // --- inventory access ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof ItemPWRFuel;
        if (slot == 2) return stack.getItem() instanceof IItemFluidId;
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return new int[]{0, 1};
    }

    public boolean canInsertItem(int slot, ItemStack stack) {
        return isItemValidForSlot(slot, stack);
    }

    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot == 1;
    }

    // --- persistence ---

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);

        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");

        this.assembled = input.getBooleanOr("assembled", false);
        this.coreHeat = Math.max(input.getIntOr("coreHeat", 0), input.getLongOr("coreHeatL", 0));
        this.hullHeat = Math.max(input.getIntOr("hullHeat", 0), input.getLongOr("hullHeatL", 0));
        this.flux = input.getDoubleOr("flux", 0);
        this.rodLevel = input.getDoubleOr("rodLevel", 0);
        this.rodTarget = input.getDoubleOr("rodTarget", 0);
        // saved by item name; a save from before the fuel data map holds the 1.7 enum name ("MEU")
        String saved = input.getStringOr("typeLoaded", "");
        if (!saved.isEmpty() && !saved.contains(":")) saved = "hbm:pwr_fuel_" + saved.toLowerCase(Locale.ROOT);
        Item savedFuel = saved.isEmpty() ? null : ReactorDataMaps.itemOf(saved);
        this.typeLoaded = savedFuel == null || ItemPWRFuel.stats(savedFuel) == null ? -1
                : BuiltInRegistries.ITEM.getId(savedFuel);
        this.amountLoaded = input.getIntOr("amountLoaded", 0);
        this.progress = input.getDoubleOr("progress", 0);
        this.processTime = input.getDoubleOr("processTime", 0);
        this.coreHeatCapacity = Math.max(input.getIntOr("coreHeatCapacity", 0), input.getLongOr("coreHeatCapacityL", 0));
        if (this.coreHeatCapacity < coreHeatCapacityBase) this.coreHeatCapacity = coreHeatCapacityBase;

        this.rodCount = input.getIntOr("rodCount", 0);
        this.connections = input.getIntOr("connections", 0);
        this.connectionsControlled = input.getIntOr("connectionsControlled", 0);
        this.heatexCount = input.getIntOr("heatexCount", 0);
        this.channelCount = input.getIntOr("channelCount", 0);
        this.sourceCount = input.getIntOr("sourceCount", 0);
        this.heatsinkCount = input.getIntOr("heatsinkCount", 0);

        int portCount = input.getIntOr("portCount", 0);
        List<PortDef> portList = new ArrayList<>();
        for (int i = 0; i < portCount; i++) {
            int[] port = input.getIntArrayOr("p" + i, null);
            if (port != null && port.length == 3) portList.add(PortDef.make(port[0], port[1], port[2], ForgeDirection.VALID_DIRECTIONS));
        }
        this.ports = portList.toArray(new PortDef[0]);

        rods.clear();
        int rodCount = input.getIntOr("rodCount", 0);
        for (int i = 0; i < rodCount; i++) {
            int[] rod = input.getIntArrayOr("r" + i, null);
            if (rod != null && rod.length == 3) rods.add(new BlockPos(rod[0], rod[1], rod[2]));
        }
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);

        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");

        output.putBoolean("assembled", assembled);
        output.putLong("coreHeatL", coreHeat);
        output.putLong("hullHeatL", hullHeat);
        output.putDouble("flux", flux);
        output.putDouble("rodLevel", rodLevel);
        output.putDouble("rodTarget", rodTarget);
        if (typeLoaded >= 0) output.putString("typeLoaded", BuiltInRegistries.ITEM
                .getKey(BuiltInRegistries.ITEM.byId(typeLoaded)).toString());
        output.putInt("amountLoaded", amountLoaded);
        output.putDouble("progress", progress);
        output.putDouble("processTime", processTime);
        output.putLong("coreHeatCapacityL", coreHeatCapacity);

        output.putInt("rodCount", rodCount);
        output.putInt("connections", connections);
        output.putInt("connectionsControlled", connectionsControlled);
        output.putInt("heatexCount", heatexCount);
        output.putInt("channelCount", channelCount);
        output.putInt("sourceCount", sourceCount);
        output.putInt("heatsinkCount", heatsinkCount);

        output.putInt("portCount", ports.length);
        for (int i = 0; i < ports.length; i++) {
            BlockPos pos = ports[i].portPositions[0];
            output.putIntArray("p" + i, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }

        // 1.7 wrote "rodCount" twice: the fuel-block count first, then rods.size(); the second wins. Same here.
        output.putInt("rodCount", rods.size());
        for (int i = 0; i < rods.size(); i++) {
            BlockPos pos = rods.get(i);
            output.putIntArray("r" + i, new int[]{pos.getX(), pos.getY(), pos.getZ()});
        }
    }

    // --- control ---

    @Override
    public boolean hasPermission(Player player) {
        return this.stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("control")) {
            this.rodTarget = Mth.clamp(Tags.getIntOr(data, "control", 0), 0, 100);
            this.markChanged();
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPWR(containerId, playerInventory, this);
    }

    // --- fluid ---

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }

    /** The controller block itself has no fluid faces; the port cells relay to it. */
    @Override
    public boolean canConnect(FluidType type, ForgeDirection dir) {
        return false;
    }

    // --- Redstone over Radio ---

    public static final String[] ROR = new String[]{ // not to be confused with RUR
            PREFIX_VALUE + "rods",
            PREFIX_VALUE + "coreheat",
            PREFIX_VALUE + "hullheat",
            PREFIX_VALUE + "coldbuf",
            PREFIX_VALUE + "hotbuf",
            PREFIX_VALUE + "flux",
            PREFIX_VALUE + "depletion",
            PREFIX_FUNCTION + "setrods" + NAME_SEPARATOR + "percent",
            PREFIX_FUNCTION + "jettison",
    };

    @Override
    public String[] getFunctionInfo() {
        return ROR;
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "rods").equals(name)) return "" + (int) (100 - this.rodLevel); // why the fuck did i invert this again?
        if ((PREFIX_VALUE + "coreheat").equals(name)) return "" + this.coreHeat;
        if ((PREFIX_VALUE + "hullheat").equals(name)) return "" + this.hullHeat;
        if ((PREFIX_VALUE + "coldbuf").equals(name)) return "" + this.tanks[0].getFill();
        if ((PREFIX_VALUE + "hotbuf").equals(name)) return "" + this.tanks[1].getFill();
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) this.flux;
        if ((PREFIX_VALUE + "depletion").equals(name)) return "" + (int) (this.progress * 100 / this.processTime);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setrods").equals(name) && params.length > 0) {
            int percent = IRORInteractive.parseInt(params[0], 0, 100);
            // rodTarget is the insertion, the command sets the extraction
            this.rodTarget = 100 - percent;
            this.markChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "jettison").equals(name)) {
            this.typeLoaded = -1;
            this.amountLoaded = 0;
            this.progress = 0;
            this.markChanged();
            return null;
        }
        return null;
    }
}
