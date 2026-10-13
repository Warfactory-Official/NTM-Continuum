package com.hbm.tileentity.machine;

import com.hbm.advancement.AwardRegions;
import com.hbm.advancement.DetonationTrigger;
import com.hbm.advancement.HbmCriteria;
import com.hbm.api.data.ReactorDataMaps;
import com.hbm.blocks.ModHazardBlocks;
import dev.golem.core.data.Tags;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.blocks.machine.Watz;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.container.ContainerWatz;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.HeatRecipes;
import com.hbm.items.ModWatzItems;
import com.hbm.items.machine.EnumWatzType;
import com.hbm.items.machine.ItemWatzPellet;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The Watz power plant: a stack of 24-channel fuel segments sharing one coolant and mud volume.
 *
 * <p>Only the <b>bottom</b> segment of a column simulates: it collects every segment below-to-above,
 * merges their tanks into one shared set, runs cooling bottom-up and the reaction top-down, then
 * redistributes the shared fluid back into the individual tanks. A segment that has another Watz core
 * three blocks above it does nothing ({@link #updateLock}). This mirrors upstream exactly, including
 * the ordering, which is load-bearing: reaction flux propagates downward between segments.
 *
 * <p>1.7's OpenComputers component is served to CC: Tweaked by
 * {@code com.hbm.compat.computercraft.machine.WatzPeripheral}. Known deviation, recorded in IMPLEMENTATION.md
 * §20: the meltdown's shrapnel spray is currently the mud columns alone.
 */
public class TileEntityWatz extends TileEntityMachineBase
        implements IFluidStandardTransceiverMK2, WorldlyContainer, MenuProvider, IControlReceiver, IRORValueProvider {

    public static final int SLOTS = 24;
    private static final int[] ALL_SLOTS = IntStream.range(0, SLOTS).toArray();
    /** Vertical spacing between the cores of stacked segments. */
    private static final int SEGMENT_HEIGHT = 3;

    public final FluidTankNTM[] tanks;
    /** The merged column tanks, rebuilt every tick by the bottom segment. */
    private FluidTankNTM[] sharedTanks;
    /** A copy of the shared tanks kept purely so the GUI can show column-wide totals. */
    private final FluidTankNTM[] sharedTanksSync;

    public int heat;
    /** Flux from the previous passive emission; display only. */
    private double fluxLastBase;
    /** Flux from the previous reaction; feeds the next one. */
    private double fluxLastReaction;
    /** Client-side total flux, straight off the wire. */
    public double fluxDisplay;
    public boolean isOn;

    /**
     * Slot filters: when locked, each slot only accepts the pellet type it held at lock time. Upstream
     * stored a whole {@code ItemStack} per slot, but the only thing it ever compared was item + meta,
     * which here is exactly the pellet type, so the lock is the type, and null means "no filter".
     */
    public boolean isLocked = false;
    /** Per slot, the fresh pellet item locked there (saved by item name). */
    private final Item[] locks = new Item[SLOTS];

    /** 1.7 {@code fluxLastBase + fluxLastReaction}, what its OpenComputers {@code getFlux} answered. */
    public double getFluxLast() {
        return fluxLastBase + fluxLastReaction;
    }

    public TileEntityWatz(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATZ.get(), pos, state, SLOTS);
        this.tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.COOLANT, 64_000),
                new FluidTankNTM(Fluids.COOLANT_HOT, 64_000),
                new FluidTankNTM(Fluids.WATZ, 64_000),
        };
        this.sharedTanksSync = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.COOLANT, 0),
                new FluidTankNTM(Fluids.COOLANT_HOT, 0),
                new FluidTankNTM(Fluids.WATZ, 0),
        };
        resetSharedTanks();
    }

    @Override public String getDefaultName() { return "container.watz"; }

    @Override public Component getDisplayName() { return Component.translatable(getDefaultName()); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerWatz(containerId, playerInventory, this);
    }

    private void resetSharedTanks() {
        this.sharedTanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.COOLANT, 64_000),
                new FluidTankNTM(Fluids.COOLANT_HOT, 64_000),
                new FluidTankNTM(Fluids.WATZ, 64_000),
        };
        for (int i = 0; i < 3; i++) this.sharedTanks[i].setFill(tanks[i].getFill());
    }

    /** The tanks the GUI reads: column totals on the client, this segment's own on the server. */
    public FluidTankNTM[] getDisplayTanks() {
        return sharedTanksSync;
    }

    // --- simulation ---

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityWatz te) {
        te.resetSharedTanks();
        if (te.updateLock()) return; // a segment above us drives the whole column

        boolean turnedOn = level.getBlockState(pos.above(3)).getBlock() == ModMachineBlocks.watz_pump.get()
                && level.hasNeighborSignal(pos.above(5));

        List<TileEntityWatz> segments = new ArrayList<>();
        segments.add(te);
        for (DirPos port : te.topPorts()) te.trySubscribe(te.tanks[0].getTankType(), level, port); // 1.7 subscribeToTop

        // Accumulate the column downward. Upstream scans to y=0; a 26.1 world can start lower.
        for (int y = pos.getY() - SEGMENT_HEIGHT; y >= level.getMinY(); y -= SEGMENT_HEIGHT) {
            if (!(level.getBlockEntity(new BlockPos(pos.getX(), y, pos.getZ())) instanceof TileEntityWatz below)) break;
            segments.add(below);
        }

        // Merge every segment's tanks into one shared set sized to the whole column.
        te.sharedTanks = new FluidTankNTM[3];
        for (int i = 0; i < 3; i++) te.sharedTanks[i] = new FluidTankNTM(te.tanks[i].getTankType(), 0);
        for (TileEntityWatz segment : segments) {
            segment.setupCoolant();
            for (int i = 0; i < 3; i++) {
                te.sharedTanks[i].changeTankSize(te.sharedTanks[i].getMaxFill() + segment.tanks[i].getMaxFill());
                te.sharedTanks[i].setFill(te.sharedTanks[i].getFill() + segment.tanks[i].getFill());
            }
        }

        // Cooling runs bottom to top, the reaction top to bottom.
        for (int i = segments.size() - 1; i >= 0; i--) segments.get(i).updateCoolant(te.sharedTanks);

        te.updateReaction(null, te.sharedTanks, turnedOn);
        for (int i = 1; i < segments.size(); i++) {
            segments.get(i).updateReaction(segments.get(i - 1), te.sharedTanks, turnedOn);
        }

        for (TileEntityWatz segment : segments) {
            for (int i = 0; i < 3; i++) {
                segment.sharedTanksSync[i].changeTankSize(te.sharedTanks[i].getMaxFill());
                segment.sharedTanksSync[i].setFill(te.sharedTanks[i].getFill());
            }
            segment.sharedTanks = te.sharedTanks;
            segment.isOn = turnedOn;
            segment.setChanged();
            segment.networkPackNT();
            segment.heat *= 0.99; // cool 1% per tick
        }

        // Push the shared fluid back down into the real tanks, bottom first.
        for (int i = segments.size() - 1; i >= 0; i--) {
            TileEntityWatz segment = segments.get(i);
            for (int j = 0; j < 3; j++) {
                int min = Math.min(segment.tanks[j].getMaxFill(), te.sharedTanks[j].getFill());
                te.sharedTanks[j].setFill(te.sharedTanks[j].getFill() - min);
                segment.tanks[j].setFill(min);
            }
        }

        segments.get(segments.size() - 1).sendOutBottom(level);

        // Mud that no tank could absorb has nowhere to go: the column ruptures.
        if (te.sharedTanks[2].getFill() > 0) {
            te.meltdown(level, pos);
        }
    }

    /** Basic sanity checking; usually a no-op except when NBT loading borks. */
    private void setupCoolant() {
        tanks[0].setTankType(Fluids.COOLANT);
        HeatRecipes.HeatStep step = HeatRecipes.getReactorStep(tanks[0].getTankType());
        if (step != null) tanks[1].setTankType(step.output());
    }

    private void updateCoolant(FluidTankNTM[] shared) {
        HeatRecipes.HeatStep step = HeatRecipes.getReactorStep(shared[0].getTankType());
        if (step == null) return;

        double coolingFactor = 0.2D; // 20% per tick
        double heatToUse = this.heat * coolingFactor;

        int heatCycles = (int) (heatToUse / step.heatReq());
        int coolCycles = shared[0].getFill() / step.amountReq();
        int hotCycles = (shared[1].getMaxFill() - shared[1].getFill()) / step.amountProduced();

        int cycles = Math.min(heatCycles, Math.min(hotCycles, coolCycles));
        this.heat -= cycles * step.heatReq();
        shared[0].setFill(shared[0].getFill() - cycles * step.amountReq());
        shared[1].setFill(shared[1].getFill() + cycles * step.amountProduced());
    }

    /**
     * Runs this segment's reaction. Called in strict top-to-bottom order so flux propagates the same
     * way every tick instead of following whatever order the segments happened to be placed in.
     *
     * @param above the segment directly above, or null for the topmost one
     */
    private void updateReaction(TileEntityWatz above, FluidTankNTM[] shared, boolean turnedOn) {
        if (turnedOn) {
            List<ItemStack> pellets = new ArrayList<>();
            for (int i = 0; i < SLOTS; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                if (isFreshPellet(stack)) pellets.add(stack);
            }

            double baseFlux = 0D;
            for (ItemStack stack : pellets) baseFlux += typeOf(stack).passive();

            double inputFlux = baseFlux + fluxLastReaction;
            double addedFlux = 0D;
            double addedHeat = 0D;

            for (ItemStack stack : pellets) {
                ReactorDataMaps.WatzPellet type = typeOf(stack);
                if (type.burn().isEmpty()) continue;
                double div = type.heatDiv().isPresent() ? type.heatDiv().get().effonix(heat) : 1D;
                double burn = type.burn().get().effonix(inputFlux) / div;
                ItemWatzPellet.setYield(stack, ItemWatzPellet.getYield(stack) - burn);
                addedFlux += burn;
                addedHeat += type.heat() * burn;
                shared[2].setFill(shared[2].getFill() + (int) Math.round(type.mud() * burn));
            }

            for (ItemStack stack : pellets) {
                ReactorDataMaps.WatzPellet type = typeOf(stack);
                if (type.absorb().isEmpty()) continue;
                double absorb = type.absorb().get().effonix(baseFlux + fluxLastReaction);
                addedHeat += absorb;
                ItemWatzPellet.setYield(stack, ItemWatzPellet.getYield(stack) - absorb);
                shared[2].setFill(shared[2].getFill() + (int) Math.round(type.mud() * absorb));
            }

            this.heat += (int) addedHeat;
            this.fluxLastBase = baseFlux;
            this.fluxLastReaction = addedFlux;
        } else {
            this.fluxLastBase = 0;
            this.fluxLastReaction = 0;
        }

        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (isFreshPellet(stack) && ItemWatzPellet.getEnrichment(stack) <= 0) {
                // Depleted pellets may persist for one tick, as upstream.
                inventory.setStackInSlot(i, new ItemStack(ReactorDataMaps.itemOf(typeOf(stack).depleted())));
            }
        }

        if (above != null) {
            for (int i = 0; i < SLOTS; i++) {
                ItemStack bottom = inventory.getStackInSlot(i);
                ItemStack top = above.inventory.getStackInSlot(i);

                // Pellets fall into an empty slot below...
                if (bottom.isEmpty() && !top.isEmpty()) {
                    inventory.setStackInSlot(i, top.copy());
                    above.inventory.setStackInSlot(i, ItemStack.EMPTY);
                    continue;
                }
                // ...and swap upward when the slot above has gone spent, so fresh fuel sinks.
                if (isFreshPellet(bottom) && isDepletedPellet(top)) {
                    ItemStack buf = top.copy();
                    above.inventory.setStackInSlot(i, bottom.copy());
                    inventory.setStackInSlot(i, buf);
                }
            }
        }
    }

    private static boolean isFreshPellet(ItemStack stack) {
        return stack.getItem() instanceof ItemWatzPellet pellet && !pellet.depleted && ItemWatzPellet.stats(pellet) != null;
    }

    private static boolean isDepletedPellet(ItemStack stack) {
        return stack.getItem() instanceof ItemWatzPellet pellet && pellet.depleted;
    }

    private static ReactorDataMaps.WatzPellet typeOf(ItemStack stack) {
        return ItemWatzPellet.stats(stack.getItem());
    }

    /** True when another segment sits directly above, which means that one drives the column. */
    private boolean updateLock() {
        return level != null
                && level.getBlockEntity(worldPosition.above(SEGMENT_HEIGHT)) instanceof TileEntityWatz;
    }

    // --- fluid I/O: coolant enters at the top of the column, hot coolant and mud leave at the bottom ---


    private void sendOutBottom(Level level) {
        for (DirPos port : bottomPorts()) {
            if (tanks[1].getFill() > 0) tryProvide(tanks[1], level, port);
            if (tanks[2].getFill() > 0) tryProvide(tanks[2], level, port);
        }
    }

    private List<DirPos> topPorts() {
        int x = worldPosition.getX(), y = worldPosition.getY() + 3, z = worldPosition.getZ();
        return List.of(
                new DirPos(x, y, z, ForgeDirection.UP),
                new DirPos(x + 2, y, z, ForgeDirection.UP),
                new DirPos(x - 2, y, z, ForgeDirection.UP),
                new DirPos(x, y, z + 2, ForgeDirection.UP),
                new DirPos(x, y, z - 2, ForgeDirection.UP));
    }

    private List<DirPos> bottomPorts() {
        int x = worldPosition.getX(), y = worldPosition.getY() - 1, z = worldPosition.getZ();
        return List.of(
                new DirPos(x, y, z, ForgeDirection.DOWN),
                new DirPos(x + 2, y, z, ForgeDirection.DOWN),
                new DirPos(x - 2, y, z, ForgeDirection.DOWN),
                new DirPos(x, y, z + 2, ForgeDirection.DOWN),
                new DirPos(x, y, z - 2, ForgeDirection.DOWN));
    }

    public List<DirPos> getConPos() {
        List<DirPos> all = new ArrayList<>(topPorts());
        all.addAll(bottomPorts());
        return all;
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    // --- meltdown ---

    /**
     * Mud overflow: the column's fluid volume is full of reaction sludge with nowhere to go. Upstream
     * clears the space above the core, replaces the reactor with mud and broken component columns,
     * dumps 1000 rads and plays the RBMK detonation.
     */
    private void meltdown(Level level, BlockPos pos) {
        for (int x = -3; x <= 3; x++) {
            for (int y = 3; y < 6; y++) {
                for (int z = -3; z <= 3; z++) {
                    level.removeBlock(pos.offset(x, y, z), false);
                }
            }
        }
        disassemble(level, pos);

        ChunkRadiationManager.proxy.incrementRad(level, pos.above(), 1_000F);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5,
                ModSounds.RBMK_EXPLOSION.get(), SoundSource.BLOCKS, 50.0F, 1.0F);
    }

    private void disassemble(Level level, BlockPos pos) {
        // The reactor is being destroyed, not mined: no component drop.
        Watz.drop = false;
        BlockMultiblockCore.withoutTeardown(() -> {
            Block mud = ModHazardBlocks.mud_block.get();
            for (int i = 0; i < 3; i++) level.setBlock(pos.above(i), mud.defaultBlockState(), 3);

            Block element = ModBlocks.watz_element.get();
            Block cooler = ModBlocks.watz_cooler.get();
            Block casing = ModMachineBlocks.watz_casing_tooled.get();

            int[][] elements = {{1, 0}, {2, 0}, {0, 1}, {0, 2}, {-1, 0}, {-2, 0}, {0, -1}, {0, -2},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
            for (int[] c : elements) setBrokenColumn(level, pos, 0, element, c[0], c[1]);

            int[][] coolers = {{2, 1}, {2, -1}, {1, 2}, {-1, 2}, {-2, 1}, {-2, -1}, {1, -2}, {-1, -2}};
            for (int[] c : coolers) setBrokenColumn(level, pos, 0, cooler, c[0], c[1]);

            for (int j = -1; j < 2; j++) {
                setBrokenColumn(level, pos, 1, casing, 3, j);
                setBrokenColumn(level, pos, 1, casing, j, 3);
                setBrokenColumn(level, pos, 1, casing, -3, j);
                setBrokenColumn(level, pos, 1, casing, j, -3);
            }
            setBrokenColumn(level, pos, 1, casing, 2, 2);
            setBrokenColumn(level, pos, 1, casing, 2, -2);
            setBrokenColumn(level, pos, 1, casing, -2, 2);
            setBrokenColumn(level, pos, 1, casing, -2, -2);
        });
        Watz.drop = true;

        // 1.7 achWatzBoom (TileEntityWatz:529): every player in the core's box grown by 50
        if (level instanceof ServerLevel server) {
            AwardRegions.within(server, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 50D,
                    player -> HbmCriteria.detonation(player, DetonationTrigger.Kind.WATZ));
        }
    }

    /** A three-tall column that survives to a random height and is mud above that. */
    private void setBrokenColumn(Level level, BlockPos pos, int minHeight, Block block, int x, int z) {
        int height = minHeight + level.getRandom().nextInt(3 - minHeight);
        for (int i = 0; i < 3; i++) {
            level.setBlock(pos.offset(x, i, z),
                    (i <= height ? block : ModHazardBlocks.mud_block.get()).defaultBlockState(), 3);
        }
    }

    // --- inventory ---

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (!isFreshPellet(stack)) return false;
        if (!isLocked) return true;
        return locks[slot] != null && locks[slot] == ItemWatzPellet.freshOf(stack.getItem());
    }

    @Override public int[] getSlotsForFace(Direction side) { return ALL_SLOTS; }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return !isFreshPellet(stack);
    }

    // --- GUI control ---

    @Override public boolean hasPermission(Player player) {
        return player.containerMenu instanceof ContainerWatz menu
                && menu.getWatz() == this && menu.stillValid(player);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("lock")) return;
        if (isLocked) {
            Arrays.fill(locks, null);
        } else {
            for (int i = 0; i < SLOTS; i++) {
                ItemStack stack = inventory.getStackInSlot(i);
                locks[i] = ItemWatzPellet.freshOf(stack.getItem());
            }
        }
        isLocked = !isLocked;
        setChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "heat", PREFIX_VALUE + "flux", PREFIX_VALUE + "mud",
                PREFIX_VALUE + "coolant_hot", PREFIX_VALUE + "coolant_cold"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name)) return "" + heat;
        if ((PREFIX_VALUE + "flux").equals(name)) return "" + (int) (fluxLastBase + fluxLastReaction);
        if ((PREFIX_VALUE + "mud").equals(name)) return "" + tanks[2].getFill();
        if ((PREFIX_VALUE + "coolant_hot").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "coolant_cold").equals(name)) return "" + tanks[0].getFill();
        return null;
    }

    // --- persistence + sync ---

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(heat);
        buf.writeBoolean(isOn);
        buf.writeBoolean(isLocked);
        buf.writeDouble(fluxLastReaction + fluxLastBase);
        // The column totals, so every segment's GUI shows the same shared volume.
        for (FluidTankNTM tank : sharedTanksSync) tank.serialize(buf);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        heat = buf.readInt();
        isOn = buf.readBoolean();
        isLocked = buf.readBoolean();
        fluxDisplay = buf.readDouble();
        for (FluidTankNTM tank : sharedTanksSync) tank.deserialize(buf);
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
        output.putInt("heat", heat);
        output.putDouble("lastFluxB", fluxLastBase);
        output.putDouble("lastFluxR", fluxLastReaction);
        output.putBoolean("isLocked", isLocked);
        for (int i = 0; i < SLOTS; i++) {
            if (locks[i] != null) output.putString("lock" + i, BuiltInRegistries.ITEM.getKey(locks[i]).toString());
        }
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
        heat = input.getIntOr("heat", 0);
        fluxLastBase = input.getDoubleOr("lastFluxB", 0D);
        fluxLastReaction = input.getDoubleOr("lastFluxR", 0D);
        isLocked = input.getBooleanOr("isLocked", false);
        for (int i = 0; i < SLOTS; i++) {
            // by item name; a save from before the pellet data map holds the 1.7 type name ("SCHRABIDIUM")
            String lock = input.getStringOr("lock" + i, "");
            if (!lock.isEmpty() && !lock.contains(":")) lock = "hbm:watz_pellet_" + lock.toLowerCase(Locale.ROOT);
            Item item = lock.isEmpty() ? null : ReactorDataMaps.itemOf(lock);
            locks[i] = item instanceof ItemWatzPellet ? item : null;
        }
        resetSharedTanks();
    }

    @Override protected boolean syncFullTagOnChunkLoad() { return true; }
}
