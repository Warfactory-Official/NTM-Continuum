package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.data.DepositDataMaps;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.ModNuclearBlocks;
import com.hbm.blocks.ModOilBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.tileentity.machine.MachineFluidContainerIO;
import com.hbm.util.TickPhase;
import java.util.HashMap;
import dev.golem.core.world.SubLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;
import dev.golem.core.Ids;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import com.hbm.tileentity.TilePort.PortDef;

/**
 * Shared base of the oil derrick / pumpjack, a 1:1 port of the 1.12.2 {@code TileEntityOilDrillBase}.
 * 8 slots (0 battery, 1 to 2 oil container unload, 3 to 4 gas container unload, 5-7 upgrades); tanks
 * {@code [0] OIL 64k}, {@code [1] GAS 64k}, both send-only. Each {@link #getDelayEff()} ticks the drill
 * walks its column from {@code y-1} down to {@link #getDrillDepth()}: the first non-{@code oil_pipe}
 * block is sucked ({@link #trySuck}, a 256-node BFS through {@code ore_oil_empty} to the nearest live
 * {@code ore_oil}) or drilled through ({@link #tryDrill}, replaced by {@code oil_pipe} when explosion
 * resistance &lt; 1000). Power drains every eligible tick; the AFTERBURN upgrade burns tanked gas
 * ({@code level*10} mB/t gives {@code +5 HE/mB}).
 *
 * <p>Skipped from the old class (see IMPLEMENTATION.md §20): IPersistentNBT (power/tanks don't survive item form),
 * IConfigurableMachine, IFluidCopiable, the {@code ore_bedrock_oil} BFS target (deferred with the
 * fracking tower), and the oreUranium/oreAsbestos gas-pocket easter eggs ({@code onDrill} is an empty
 * seam: the gas blocks are unported).
 */
public abstract class TileEntityOilDrillBase extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IUpgradeInfoProvider, WorldlyContainer {

    private static final TagKey<Item> ORE_URANIUM = TagKey.create(Registries.ITEM, Ids.of("c", "ores/uranium"));
    private static final TagKey<Item> ORE_ASBESTOS = TagKey.create(Registries.ITEM, Ids.of("c", "ores/asbestos"));

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    public long power;
    public int indicator = 0;
    public FluidTankNTM[] tanks;
    public int speedLevel;
    public int energyLevel;
    public int overLevel;
    private final HashSet<BlockPos> processed = new HashSet<>();

    public TileEntityOilDrillBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 8);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.OIL, 64_000);
        tanks[1] = new FluidTankNTM(Fluids.GAS, 64_000);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityOilDrillBase te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        MachineFluidContainerIO.unload(te.tanks[0], te.inventory, 1, 2);
        MachineFluidContainerIO.unload(te.tanks[1], te.inventory, 3, 4);

        te.upgradeManager.checkSlots(te.inventory, 5, 7);
        te.speedLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        te.energyLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3);
        te.overLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) + 1;
        int abLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.AFTERBURN), 3);

        int toBurn = Math.min(te.tanks[1].getFill(), abLevel * 10);
        if (toBurn > 0) {
            te.tanks[1].setFill(te.tanks[1].getFill() - toBurn);
            te.power += toBurn * 5L;
            if (te.power > te.getMaxPower()) te.power = te.getMaxPower();
        }

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.getMaxPower());

        // port: on a moving structure (Sable) the pipe column would grow through the structure's own plot
        if (!SubLevels.isOnSubLevel(level, pos)
                && te.power >= te.getPowerReqEff()
                && te.tanks[0].getFill() < te.tanks[0].getMaxFill()
                && te.tanks[1].getFill() < te.tanks[1].getMaxFill()) {

            te.power -= te.getPowerReqEff();

            if (TickPhase.every(level, pos, te.getDelayEff())) {
                te.indicator = 0;

                for (int y = pos.getY() - 1; y >= te.getDrillDepth(); y--) {
                    if (level.getBlockState(new BlockPos(pos.getX(), y, pos.getZ())).getBlock() != ModOilBlocks.oil_pipe.get()) {
                        if (!te.trySuck(y)) {
                            te.tryDrill(y);
                        }
                        break;
                    }
                    if (y == te.getDrillDepth()) te.indicator = 1;
                }
            }
        } else {
            te.indicator = 2;
        }

        te.networkPackNT();
    }

    public boolean canPump() {
        return true;
    }

    public int getPowerReqEff() {
        int req = this.getPowerReq();
        return (req + (req / 4 * this.speedLevel) - (req / 4 * this.energyLevel)) * this.overLevel;
    }

    public int getDelayEff() {
        int delay = getDelay();
        return Math.max((delay - (delay / 4 * this.speedLevel) + (delay / 10 * this.energyLevel)) / this.overLevel, 1);
    }

    public abstract int getPowerReq();

    public abstract int getDelay();

    public void tryDrill(int y) {
        BlockPos posD = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
        Block b = level.getBlockState(posD).getBlock();

        if (b.getExplosionResistance() < 1000) {
            onDrill(y);
            level.setBlockAndUpdate(posD, ModOilBlocks.oil_pipe.get().defaultBlockState());
        } else {
            this.indicator = 2;
        }
    }

    /** Called with the y of each block the drill goes through; the derrick and pumpjack release gas from some ores. */
    public void onDrill(int y) {}

    public int getDrillDepth() {
        return 5;
    }

    public boolean trySuck(int y) {
        BlockPos startPos = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
        if (!canSuckState(level.getBlockState(startPos))) return false;
        if (!this.canPump()) return true;
        Queue<BlockPos> queue = new ArrayDeque<>();
        processed.clear();
        queue.offer(startPos);
        processed.add(startPos);

        int nodesVisited = 0;
        while (!queue.isEmpty() && nodesVisited < 256) {
            BlockPos currentPos = queue.poll();
            nodesVisited++;
            BlockState currentState = level.getBlockState(currentPos);
            Block currentBlock = currentState.getBlock();
            // 1.7: a live deposit (ore_oil, or ore_bedrock_oil for the fracking tower) is sucked, a drained one searched
            // through; both are hbm:fluid_deposit data (DepositDataMaps), by block state where an addon's one block
            // carries several deposits (hbm:fluid_deposit_states)
            DepositDataMaps.Deposit deposit = DepositDataMaps.deposit(currentState);
            if (deposit != null && reaches(deposit)) {
                doSuck(currentPos);
                return true;
            }
            if (!DepositDataMaps.isEmptyDeposit(currentBlock)) continue;
            for (ForgeDirection dir : getShuffledDirs()) {
                BlockPos neighborPos = currentPos.offset(dir.offsetX, dir.offsetY, dir.offsetZ);
                if (!processed.contains(neighborPos) && canSuckState(level.getBlockState(neighborPos))) {
                    processed.add(neighborPos);
                    queue.offer(neighborPos);
                }
            }
        }
        return false;
    }

    /** Old {@code BobMathUtil.getShuffledDirs}: the 6 directions in random order. */
    private List<ForgeDirection> getShuffledDirs() {
        List<ForgeDirection> dirs = new ArrayList<>(List.of(ForgeDirection.VALID_DIRECTIONS));
        Collections.shuffle(dirs);
        return dirs;
    }

    /** Whether this drill reaches fracking-only deposits (1.7: the fracking tower adds {@code ore_bedrock_oil}). */
    protected boolean canFrack() {
        return false;
    }

    private boolean reaches(DepositDataMaps.Deposit deposit) {
        return canFrack() || !deposit.frackingOnly();
    }

    public boolean canSuckBlock(Block b) {
        DepositDataMaps.Deposit deposit = DepositDataMaps.deposit(b);
        if (deposit != null) return reaches(deposit);
        return DepositDataMaps.isEmptyDeposit(b);
    }

    /** {@link #canSuckBlock} for one state: a state deposit ({@code hbm:fluid_deposit_states}) decides first. */
    public boolean canSuckState(BlockState state) {
        DepositDataMaps.Deposit deposit = DepositDataMaps.deposit(state);
        if (deposit != null) return reaches(deposit);
        return DepositDataMaps.isEmptyDeposit(state.getBlock());
    }

    public void doSuck(BlockPos pos) {
        DepositDataMaps.Deposit deposit = DepositDataMaps.deposit(level.getBlockState(pos));
        if (deposit != null && reaches(deposit)) onSuck(pos, deposit);
    }

    public abstract void onSuck(BlockPos pos, DepositDataMaps.Deposit deposit);

    /**
     * 1.7's suck, shared by the three drills: {@code primary} of the deposit's first fluid, a secondary amount in
     * [{@code secondaryMin}, {@code secondaryMax}] of its second (both the drill's 1.7 amounts scaled by the deposit's
     * yield), then a {@code drainChance} (times the deposit's drain) of the deposit becoming its empty block. The random
     * draws are 1.7's, in 1.7's order (the gas range, then the drain).
     */
    protected void suckDeposit(BlockPos pos, DepositDataMaps.Deposit deposit, int primary, int secondaryMin,
                               int secondaryMax, double drainChance) {
        FluidType first = deposit.primaryFluid();
        FluidType second = deposit.secondaryFluid();
        tanks[0].setTankType(first);
        tanks[0].setFill(Math.min(tanks[0].getFill() + deposit.primaryAmount(primary), tanks[0].getMaxFill()));
        if (second != Fluids.NONE) {
            tanks[1].setTankType(second);
            int min = deposit.secondaryAmount(secondaryMin);
            int max = deposit.secondaryAmount(secondaryMax);
            tanks[1].setFill(Math.min(tanks[1].getFill() + min + level.getRandom().nextInt(max - min + 1), tanks[1].getMaxFill()));
        }
        Block empty = deposit.emptyBlock();
        if (empty != null && level.getRandom().nextDouble() < drainChance * deposit.drain()) {
            level.setBlock(pos, DepositDataMaps.emptyState(level.getBlockState(pos), empty), 3);
        }
    }

    /** 1.7 {@code writeNBT}: a drill with power or fluid in it travels with its item (1.7 shows no tooltip for it). */
    @Override
    protected CompoundTag collectCarriedData() {
        boolean empty = power == 0;
        for (FluidTankNTM tank : tanks) if (tank.getFill() > 0) empty = false;
        if (empty) return null;
        CompoundTag data = new CompoundTag();
        data.putLong("power", power);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeCarried(data, "t" + i);
        return data;
    }

    @Override
    protected void applyCarriedData(CompoundTag data) {
        power = Tags.getLongOr(data, "power", 0L);
        for (int i = 0; i < tanks.length; i++) tanks[i].readCarried(data, "t" + i);
    }

    @Override
    protected void removeSavedComponents(DataOut output) {
        super.removeSavedComponents(output);
        output.discard("inventory");
        output.discard("power");
        for (int i = 0; i < tanks.length; i++) {
            for (String suffix : new String[] {"", "_max", "_type", "_p"}) output.discard("t" + i + suffix);
        }
    }

    /** The gas 1.7 lets out of the ore the drill just went through, or null: uranium ore dense radon, asbestos ore asbestos. */
    protected @Nullable Block gasOfDrilledOre(int y) {
        ItemStack ore = new ItemStack(level.getBlockState(new BlockPos(worldPosition.getX(), y, worldPosition.getZ())).getBlock());
        if (ore.is(ORE_URANIUM)) return ModNuclearBlocks.gas_radon_dense.get();
        if (ore.is(ORE_ASBESTOS)) return ModNuclearBlocks.gas_asbestos.get();
        return null;
    }

    /** Puts {@code gas} at {@code pos} when whatever is there can be replaced. */
    protected void releaseGas(BlockPos pos, Block gas) {
        if (level.getBlockState(pos).canBeReplaced()) level.setBlockAndUpdate(pos, gas.defaultBlockState());
    }

    public abstract PortDef[] getPorts();

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        // Slots 2/4 (filled-container outs) stay TRUE, because the machine writes them via
        // inventory.insertItem, which validates against this method; the menu's take-only slots block
        // the player.
        if (slot >= 5 && slot <= 7) return Library.isMachineUpgrade(stack);
        return true;
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{5, 7};
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }

    @Override public FluidTankNTM[] getSendingTanks() { return tanks; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[0]; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER
                || type == UpgradeType.OVERDRIVE || type == UpgradeType.AFTERBURN;
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.AFTERBURN, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(indicator);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        this.power = buf.readLong();
        this.indicator = buf.readInt();
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        this.power = input.getLongOr("power", 0L);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
    }
    @Override public int[] getSlotsForFace(Direction side) { return new int[0]; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
}
