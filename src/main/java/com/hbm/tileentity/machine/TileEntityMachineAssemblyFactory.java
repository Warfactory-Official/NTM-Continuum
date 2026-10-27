package com.hbm.tileentity.machine;

import com.hbm.api.fluidmk2.IFluidMultiUserMK2;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.client.sound.AssemblyFactorySoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineAssemblyFactory;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.AssemblyMachineRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.module.machine.AssemblyFactoryModule;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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

import java.util.HashMap;
import java.util.List;
import java.util.Random;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TilePort.PortDef;
import com.hbm.tileentity.TilePort;
import com.hbm.tileentity.TilePortShapes;
import com.hbm.uninos.networkproviders.FluidNetProvider;

/** The four-lane Assembly Factory controller, ported from the 1.12.2 tile entity. */
public class TileEntityMachineAssemblyFactory extends TileEntityMachineBase implements MenuProvider,
        IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer,
        IUpgradeInfoProvider, IRORValueProvider, IFluidMultiUserMK2 {

    /**
     * 1.7: a duct at a coolant position reached the coolant {@code delegate} (water in, low-pressure steam out), not
     * the factory, whose own tanks are the recipe tanks.
     */
    @Override
    public com.hbm.api.fluidmk2.@org.jspecify.annotations.Nullable IFluidUserMK2 fluidUserAt(BlockPos pos,
            net.minecraft.core.@org.jspecify.annotations.Nullable Direction face) {
        if (face == null) return null;
        BlockPos duct = pos.relative(face);
        for (DirPos cool : getCoolPos()) {
            if (cool.getPos().equals(duct) && cool.getDir().toDirection() == face) return coolantDelegate;
        }
        return null;
    }

    public static final int LANES = 4;
    public static final int LANE_SIZE = 14;
    public static final int MACHINE_SLOTS = 60;

    private static final int[] ITEM_SLOTS = {
            5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
            33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45,
            47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59
    };

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[LANES];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[LANES];
    public final FluidTankNTM water = new FluidTankNTM(Fluids.WATER, 4_000);
    public final FluidTankNTM lps = new FluidTankNTM(Fluids.SPENTSTEAM, 4_000);
    public final FluidTankNTM[] allTanks = new FluidTankNTM[LANES * 2 + 2];

    public long power;
    public long maxPower = 1_000_000L;
    public final boolean[] didProcess = new boolean[LANES];
    public boolean frame;

    public final AssemfacArm[] animations = {new AssemfacArm(0), new AssemfacArm(1)};
    public final AssemblyFactoryModule[] assemblerModule = new AssemblyFactoryModule[LANES];
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    private final CoolantDelegate coolantDelegate = new CoolantDelegate();

    public TileEntityMachineAssemblyFactory(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ASSEMBLY_FACTORY.get(), pos, state, MACHINE_SLOTS);

        for (int i = 0; i < LANES; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, 4_000);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, 4_000);
            AssemblyFactoryModule module = new AssemblyFactoryModule(i, this, inputTanks[i], outputTanks[i]);
            module.itemInput(5 + i * LANE_SIZE);
            module.itemOutput(17 + i * LANE_SIZE);
            assemblerModule[i] = module;
            allTanks[i] = inputTanks[i];
            allTanks[LANES + i] = outputTanks[i];
        }
        allTanks[LANES * 2] = water;
        allTanks[LANES * 2 + 1] = lps;
        AssemblyMachineRecipes.INSTANCE.registerRecipes();
    }

    public static int templateSlot(int lane) { return 4 + lane * LANE_SIZE; }
    public static int inputStart(int lane) { return 5 + lane * LANE_SIZE; }
    public static int outputSlot(int lane) { return 17 + lane * LANE_SIZE; }

    @Override
    public String getDefaultName() {
        return "container.machineAssemblyFactory";
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true; // The old tile accepted the battery slot before checking its item type.
        for (int i = 0; i < LANES; i++) if (slot == templateSlot(i)) {
            return stack.getItem() instanceof ItemBlueprints;
        }
        if (slot >= 1 && slot <= 3) return Library.isMachineUpgrade(stack);
        for (int i = 0; i < LANES; i++) {
            if (slot >= inputStart(i) && slot < inputStart(i) + 12
                    && assemblerModule[i].isItemValidForPool(
                    slot, stack, inventory.getStackInSlot(templateSlot(i)))) return true;
        }
        return false;
    }

    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        for (int i = 0; i < LANES; i++) {
            if (slot == outputSlot(i) || assemblerModule[i].isSlotClogged(slot)) return true;
        }
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ITEM_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isInputSlot(slot) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canExtractItem(slot, stack, 1);
    }

    /** The position-aware equivalent of the old IConditionalInvAccess query. */
    public int[] getAccessibleSlotsFor(BlockPos accessor) {
        DirPos[] io = getIOPos();
        for (int i = 0; i < io.length; i++) {
            Direction direction = io[i].getDir().toDirection();
            if (direction != null && io[i].getPos().equals(accessor.relative(direction))) {
                int[] slots = new int[16];
                for (int j = 0; j < 12; j++) slots[j] = inputStart(i) + j;
                // The old factory exposes every lane's output at each recipe-field proxy.
                for (int lane = 0; lane < LANES; lane++) slots[12 + lane] = outputSlot(lane);
                return slots;
            }
        }
        return ITEM_SLOTS;
    }

    private boolean isInputSlot(int slot) {
        for (int i = 0; i < LANES; i++) if (slot >= inputStart(i) && slot < inputStart(i) + 12) return true;
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAssemblyFactory factory) {
        factory.setupPowerPorts(factory.getPowerPorts());
        factory.setupFluidPorts(factory.getFluidPorts());
        if (factory.coolantInPort == null) factory.coolantInPort = new TilePort().setupOwner(factory.coolantDelegate).setupPort(factory.getCoolantPort());
        if (factory.coolantOutPort == null) factory.coolantOutPort = new TilePort().setupOwner(factory.coolantDelegate).setupPort(factory.getCoolantPort());
        factory.updatePortPIFIFO();
        factory.coolantInPort.setupFluids(factory.water, level);
        factory.coolantInPort.update(level);
        factory.coolantInPort.checkSubscribe(level);
        factory.coolantOutPort.setupFluids(factory.lps, level);
        factory.coolantOutPort.update(level);
        factory.coolantOutPort.checkProvide(level);

        if (factory.maxPower <= 0L) factory.maxPower = 10_000_000L;

        long nextMaxPower = 0L;
        for (AssemblyFactoryModule module : factory.assemblerModule) {
            GenericRecipe recipe = AssemblyMachineRecipes.INSTANCE.getRecipe(module.recipe);
            if (recipe != null) nextMaxPower += recipe.power * 100L;
        }
        factory.maxPower = Math.max(Math.max(factory.power, nextMaxPower), 1_000_000L);
        factory.power = Library.chargeTEFromItems(factory.inventory, 0, factory.power, factory.maxPower);
        factory.upgradeManager.checkSlots(factory.inventory, 1, 3);

        int speedLevel = Math.min(factory.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(factory.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(factory.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double consumption = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        boolean dirty = false;
        for (int i = 0; i < LANES; i++) {
            AssemblyFactoryModule module = factory.assemblerModule[i];
            module.update(speed * 2D, consumption * 2D, factory.canCool(),
                    factory.inventory.getStackInSlot(templateSlot(i)));
            factory.didProcess[i] = module.didProcess;
            if (module.didProcess) {
                factory.water.setFill(factory.water.getFill() - 100);
                factory.lps.setFill(factory.lps.getFill() + 100);
            }
            dirty |= module.markDirty;
        }

        if (dirty) factory.setChanged();
        factory.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineAssemblyFactory factory) {
        AssemblyFactorySoundHandler.tickLoop(factory);
        boolean working = factory.isProcessing();
        for (AssemfacArm animation : factory.animations) animation.update(working);
        if (TickPhase.every(level, pos, 20L)) factory.frame = !level.getBlockState(pos.above(3)).isAir();
    }

    public boolean isProcessing() {
        for (boolean processing : didProcess) if (processing) return true;
        return false;
    }

    public boolean canCool() {
        return water.getFill() >= 100 && lps.getFill() <= lps.getMaxFill() - 100;
    }

    public TilePort coolantInPort;
    public TilePort coolantOutPort;

    protected PortDef[] cachedPowerPorts;
    public PortDef[] getPowerPorts() { if (cachedPowerPorts == null) cachedPowerPorts = TilePortShapes.purex(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()); return cachedPowerPorts; }

    protected PortDef[] cachedFluidPorts;
    public PortDef[] getFluidPorts() {
        if (cachedFluidPorts == null) {
            ForgeDirection dir = facing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedFluidPorts = new PortDef[] {
                    PortDef.make(x + dir.offsetX + rot.offsetX * 2, y, z + dir.offsetZ + rot.offsetZ * 2, rot),
                    PortDef.make(x - dir.offsetX + rot.offsetX * 2, y, z - dir.offsetZ + rot.offsetZ * 2, rot),
                    PortDef.make(x + dir.offsetX - rot.offsetX * 2, y, z + dir.offsetZ - rot.offsetZ * 2, rot.getOpposite()),
                    PortDef.make(x - dir.offsetX - rot.offsetX * 2, y, z - dir.offsetZ - rot.offsetZ * 2, rot.getOpposite()),
                    PortDef.make(x + 2, y, z + 0, Library.POS_X),
                    PortDef.make(x + 2, y, z + 2, Library.POS_X, Library.POS_Z),
                    PortDef.make(x + 0, y, z + 2, Library.POS_Z),
                    PortDef.make(x - 2, y, z + 2, Library.NEG_X, Library.POS_Z),
                    PortDef.make(x - 2, y, z + 0, Library.NEG_X),
                    PortDef.make(x - 2, y, z - 2, Library.NEG_X, Library.NEG_Z),
                    PortDef.make(x + 0, y, z - 2, Library.NEG_Z),
                    PortDef.make(x + 2, y, z - 2, Library.POS_X, Library.NEG_Z),
            };
        }
        return cachedFluidPorts;
    }

    protected PortDef coolantPort;
    public PortDef getCoolantPort() {
        if (coolantPort == null) {
            ForgeDirection dir = facing();
            ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            coolantPort = PortDef.combine(
                    PortDef.make(x + rot.offsetX + dir.offsetX * 2, y, z + rot.offsetZ + dir.offsetZ * 2, dir),
                    PortDef.make(x - rot.offsetX + dir.offsetX * 2, y, z - rot.offsetZ + dir.offsetZ * 2, dir),
                    PortDef.make(x + rot.offsetX - dir.offsetX * 2, y, z + rot.offsetZ - dir.offsetZ * 2, dir.getOpposite()),
                    PortDef.make(x - rot.offsetX - dir.offsetX * 2, y, z - rot.offsetZ - dir.offsetZ * 2, dir.getOpposite())
            );
        }
        return coolantPort;
    }

    public DirPos[] getConPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 3, y, z - 2, Library.POS_X),
                new DirPos(x + 3, y, z, Library.POS_X),
                new DirPos(x + 3, y, z + 2, Library.POS_X),
                new DirPos(x - 3, y, z - 2, Library.NEG_X),
                new DirPos(x - 3, y, z, Library.NEG_X),
                new DirPos(x - 3, y, z + 2, Library.NEG_X),
                new DirPos(x - 2, y, z + 3, Library.POS_Z),
                new DirPos(x, y, z + 3, Library.POS_Z),
                new DirPos(x + 2, y, z + 3, Library.POS_Z),
                new DirPos(x - 2, y, z - 3, Library.NEG_Z),
                new DirPos(x, y, z - 3, Library.NEG_Z),
                new DirPos(x + 2, y, z - 3, Library.NEG_Z),
                // 1.7.10 has no top (y+3) ports here; those 10 belong to the chemical factory only.
                new DirPos(x + dir.offsetX + rot.offsetX * 3, y, z + dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x - dir.offsetX + rot.offsetX * 3, y, z - dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x + dir.offsetX - rot.offsetX * 3, y, z + dir.offsetZ - rot.offsetZ * 3, rot.getOpposite()),
                new DirPos(x - dir.offsetX - rot.offsetX * 3, y, z - dir.offsetZ - rot.offsetZ * 3, rot.getOpposite())
        };
    }

    public DirPos[] getIOPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + dir.offsetX + rot.offsetX * 3, y, z + dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x - dir.offsetX + rot.offsetX * 3, y, z - dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x + dir.offsetX - rot.offsetX * 3, y, z + dir.offsetZ - rot.offsetZ * 3, rot.getOpposite()),
                new DirPos(x - dir.offsetX - rot.offsetX * 3, y, z - dir.offsetZ - rot.offsetZ * 3, rot.getOpposite())
        };
    }

    public DirPos[] getCoolPos() {
        ForgeDirection dir = facing();
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + rot.offsetX + dir.offsetX * 3, y, z + rot.offsetZ + dir.offsetZ * 3, dir),
                new DirPos(x - rot.offsetX + dir.offsetX * 3, y, z - rot.offsetZ + dir.offsetZ * 3, dir),
                new DirPos(x + rot.offsetX - dir.offsetX * 3, y, z + rot.offsetZ - dir.offsetZ * 3, dir.getOpposite()),
                new DirPos(x - rot.offsetX - dir.offsetX * 3, y, z - rot.offsetZ - dir.offsetZ * 3, dir.getOpposite())
        };
    }

    @Override
    public void setRemoved() {
        if (level != null) {
            if (coolantInPort != null) coolantInPort.disableIfPresent(level);
            if (coolantOutPort != null) coolantOutPort.disableIfPresent(level);
        }
        super.setRemoved();
    }

    private ForgeDirection facing() {
        return MachineCore.facing(getBlockState());
    }

    @Override protected int[] getUpgradeSlotRange() { return new int[]{1, 3}; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_assembly_factory.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        } else if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink()
                    ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> result = new HashMap<>();
        result.put(UpgradeType.SPEED, 3);
        result.put(UpgradeType.POWER, 3);
        result.put(UpgradeType.OVERDRIVE, 3);
        return result;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() { return allTanks; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("index") || !data.contains("selection")) return;
        int index = Tags.getIntOr(data, "index", -1);
        if (index < 0 || index >= LANES) return;
        assemblerModule[index].setRecipe(Tags.getStringOr(data, "selection", "null"), false);
        markChanged();
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{PREFIX_VALUE + "progress1", PREFIX_VALUE + "progress2", PREFIX_VALUE + "progress3", PREFIX_VALUE + "progress4",
                PREFIX_VALUE + "recipe1", PREFIX_VALUE + "recipe2", PREFIX_VALUE + "recipe3", PREFIX_VALUE + "recipe4",
                PREFIX_VALUE + "anyactive", PREFIX_VALUE + "active1", PREFIX_VALUE + "active2", PREFIX_VALUE + "active3", PREFIX_VALUE + "active4"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "anyactive").equals(name)) return "" + (isProcessing() ? 1 : 0);
        for (int i = 0; i < LANES; i++) {
            if ((PREFIX_VALUE + "progress" + (i + 1)).equals(name)) return "" + (int) Math.round(assemblerModule[i].progress * 100);
            if ((PREFIX_VALUE + "recipe" + (i + 1)).equals(name)) return assemblerModule[i].recipe;
            if ((PREFIX_VALUE + "active" + (i + 1)).equals(name)) return "" + (didProcess[i] ? 1 : 0);
        }
        return null;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineAssemblyFactory(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.serialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.serialize(buf);
        water.serialize(buf);
        lps.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        for (boolean processing : didProcess) buf.writeBoolean(processing);
        for (AssemblyFactoryModule module : assemblerModule) module.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (FluidTankNTM tank : inputTanks) tank.deserialize(buf);
        for (FluidTankNTM tank : outputTanks) tank.deserialize(buf);
        water.deserialize(buf);
        lps.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        for (int i = 0; i < LANES; i++) didProcess[i] = buf.readBoolean();
        for (AssemblyFactoryModule module : assemblerModule) module.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < LANES; i++) {
            inputTanks[i].writeToNBT(output, "i" + i);
            // The old source used the input prefix for both arrays; separate keys preserve all eight tanks.
            outputTanks[i].writeToNBT(output, "o" + i);
        }
        water.writeToNBT(output, "w");
        lps.writeToNBT(output, "s");
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        for (AssemblyFactoryModule module : assemblerModule) module.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < LANES; i++) {
            inputTanks[i].readFromNBT(input, "i" + i);
            outputTanks[i].readFromNBT(input, "o" + i);
        }
        water.readFromNBT(input, "w");
        lps.readFromNBT(input, "s");
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 1_000_000L);
        for (AssemblyFactoryModule module : assemblerModule) module.load(input);
    }

    /** The coolant proxy capability from the old extra-dummy cells. */
    private final class CoolantDelegate implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2 {
        @Override public long getPower() { return TileEntityMachineAssemblyFactory.this.power; }
        @Override public void setPower(long power) { TileEntityMachineAssemblyFactory.this.power = power; }
        @Override public long getMaxPower() { return TileEntityMachineAssemblyFactory.this.maxPower; }
        @Override public boolean isLoaded() { return TileEntityMachineAssemblyFactory.this.isLoaded(); }
        @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{water}; }
        @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{lps}; }
        @Override public FluidTankNTM[] getAllTanks() { return allTanks; }
    }

    /** Two carriages, each with a striker and an inverted saw, matching the old renderer's state machine. */
    public final class AssemfacArm {
        public final AssemblerArm striker;
        public final AssemblerArm saw;
        private final Random random = new Random();
        private AFArmState state = AFArmState.WORKING;
        private double slider;
        private double prevSlider;
        private boolean direction;
        private int timeUntilReposition = 140 + random.nextInt(161);

        private AssemfacArm(int group) {
            striker = new AssemblerArm(group == 0 ? 0 : 3);
            saw = new AssemblerArm(group == 0 ? 1 : 2).asSaw();
        }

        private void update(boolean working) {
            prevSlider = slider;
            if (isProcessing() && (didProcess[striker.recipeIndex] || didProcess[saw.recipeIndex])) {
                switch (state) {
                    case WORKING -> { if (--timeUntilReposition <= 0) state = AFArmState.RETIRING; }
                    case RETIRING -> {
                        if (striker.state == ArmState.WAIT && saw.state == ArmState.WAIT) {
                            state = AFArmState.SLIDING;
                            direction = !direction;
                            if (!muffled) AssemblyFactorySoundHandler.playStart(level, worldPosition, getVolume(0.25F),
                                    1.25F + level.getRandom().nextFloat() * 0.25F);
                        }
                    }
                    case SLIDING -> {
                        double step = 1D / 10D;
                        slider += direction ? step : -step;
                        if (slider >= 1D || slider <= 0D) {
                            slider = Math.max(0D, Math.min(1D, slider));
                            state = AFArmState.WORKING;
                            timeUntilReposition = 140 + random.nextInt(161);
                        }
                    }
                }
            }
            striker.updateArm();
            saw.updateArm();
        }

        public double getSlider(float partialTicks) {
            return prevSlider + (slider - prevSlider) * partialTicks;
        }

        public final class AssemblerArm {
            public final double[] angles = new double[4];
            public final double[] prevAngles = new double[4];
            public final double[] targetAngles = new double[4];
            public final double[] speed = new double[4];
            public double sawAngle;
            public double prevSawAngle;
            public final int recipeIndex;
            private ArmState state = ArmState.REPOSITION;
            private int actionDelay;
            private boolean isSaw;
            private final Random random = new Random();

            private AssemblerArm(int recipeIndex) {
                this.recipeIndex = recipeIndex;
                resetSpeed();
                chooseNewArmPosition();
            }

            private AssemblerArm asSaw() {
                isSaw = true;
                resetSpeed();
                chooseNewArmPosition();
                return this;
            }

            private void resetSpeed() {
                speed[0] = 15D;
                speed[1] = 15D;
                speed[2] = 15D;
                speed[3] = isSaw ? 0.125D : 0.5D;
            }

            private void updateArm() {
                resetSpeed();
                System.arraycopy(angles, 0, prevAngles, 0, angles.length);
                prevSawAngle = sawAngle;

                int serviceIndex = recipeIndex;
                if (slider > 0.5D) serviceIndex += serviceIndex % 2 == 0 ? 1 : -1;
                if (!didProcess[serviceIndex]) state = ArmState.RETIRE;
                if (state == ArmState.CUT || state == ArmState.EXTEND) sawAngle += 45D;
                if (actionDelay > 0) { actionDelay--; return; }

                switch (state) {
                    case REPOSITION -> {
                        if (move()) { actionDelay = 2; state = ArmState.EXTEND; targetAngles[3] = isSaw ? -0.375D : -0.75D; }
                    }
                    case EXTEND -> {
                        if (move()) {
                            if (isSaw) {
                                state = ArmState.CUT;
                                targetAngles[2] = -targetAngles[2];
                                if (!muffled) AssemblyFactorySoundHandler.playCut(level, worldPosition, getVolume(0.5F));
                            } else {
                                state = ArmState.RETRACT;
                                targetAngles[3] = 0D;
                                if (!muffled) AssemblyFactorySoundHandler.playStrike(level, worldPosition, getVolume(0.5F));
                            }
                        }
                    }
                    case CUT -> {
                        speed[2] = Math.abs(targetAngles[2] / 20D);
                        if (move()) { state = ArmState.RETRACT; targetAngles[3] = 0D; }
                    }
                    case RETRACT -> {
                        if (move()) { actionDelay = 2 + random.nextInt(5); chooseNewArmPosition(); state = AssemfacArm.this.state == AFArmState.RETIRING ? ArmState.RETIRE : ArmState.REPOSITION; }
                    }
                    case RETIRE -> {
                        targetAngles[0] = targetAngles[1] = targetAngles[2] = targetAngles[3] = 0D;
                        if (move()) { actionDelay = 2 + random.nextInt(5); chooseNewArmPosition(); state = ArmState.WAIT; }
                    }
                    case WAIT -> { if (AssemfacArm.this.state == AFArmState.WORKING) state = ArmState.REPOSITION; }
                }
            }

            private void chooseNewArmPosition() {
                double[][] positions = isSaw ? new double[][]{
                        {-15, 15, -10}, {-15, 15, -15}, {-15, 15, 10},
                        {-15, 15, 15}, {-15, 15, 2}, {-15, 15, -2}
                } : new double[][]{
                        {10, 10, -10}, {15, 15, -15}, {25, 10, -15},
                        {30, 0, -10}, {-10, 10, 0}, {-20, 30, -15}
                };
                double[] selected = positions[random.nextInt(positions.length)];
                targetAngles[0] = selected[0];
                targetAngles[1] = selected[1];
                targetAngles[2] = selected[2];
            }

            private boolean move() {
                boolean moved = false;
                for (int i = 0; i < angles.length; i++) {
                    if (angles[i] == targetAngles[i]) continue;
                    moved = true;
                    double delta = targetAngles[i] - angles[i];
                    angles[i] = Math.abs(delta) <= speed[i] ? targetAngles[i] : angles[i] + Math.copySign(speed[i], delta);
                }
                return !moved;
            }

            public double[] getPositions(float partialTicks) {
                return new double[]{
                        lerp(prevAngles[0], angles[0], partialTicks),
                        lerp(prevAngles[1], angles[1], partialTicks),
                        lerp(prevAngles[2], angles[2], partialTicks),
                        lerp(prevAngles[3], angles[3], partialTicks),
                        lerp(prevSawAngle, sawAngle, partialTicks)
                };
            }

            private double lerp(double previous, double current, float partialTicks) {
                return previous + (current - previous) * partialTicks;
            }
        }
    }

    public enum AFArmState { WORKING, RETIRING, SLIDING }
    public enum ArmState { REPOSITION, EXTEND, CUT, RETRACT, RETIRE, WAIT }
}
