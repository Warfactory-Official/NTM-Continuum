package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.chemicalfactory.ChemicalFactoryRegistrations;
import com.hbm.client.sound.ChemicalFactorySoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.ChemicalPlantRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachineChemplant;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
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

/** Four-lane Chemical Factory, ported from the legacy TileEntityMachineChemicalFactory. */
public class TileEntityMachineChemicalFactory extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver,
        WorldlyContainer, IUpgradeInfoProvider, IRORValueProvider {

    public static final int LANES = 4;
    public static final int MACHINE_SLOTS = 32;
    public static final int TANK_CAPACITY = 24_000;
    public static final int COOLANT_CAPACITY = 4_000;
    public static final long BASE_MAX_POWER = 1_000_000L;

    private static final int[] IO_SLOTS = {
            5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 16, 17,
            19, 20, 21, 22, 23, 24, 26, 27, 28, 29, 30, 31
    };

    public final FluidTankNTM[] inputTanks = new FluidTankNTM[12];
    public final FluidTankNTM[] outputTanks = new FluidTankNTM[12];
    public final FluidTankNTM[] allTanks = new FluidTankNTM[26];
    public final FluidTankNTM water;
    public final FluidTankNTM lps;

    public long power;
    public long maxPower = BASE_MAX_POWER;
    public final boolean[] didProcess = new boolean[LANES];

    // Client-only state; the legacy renderer reads the interpolated counter and frame flag.
    public boolean frame;
    public int anim;
    public int prevAnim;

    public final ModuleMachineChemplant[] chemplantModule = new ModuleMachineChemplant[LANES];
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    public final ChemicalFactoryCoolantDelegate coolantDelegate = new ChemicalFactoryCoolantDelegate(this);

    private DirPos[] conPos;
    private DirPos[] coolPos;
    private DirPos[] ioPos;

    public TileEntityMachineChemicalFactory(BlockPos pos, BlockState state) {
        super(ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY_BE.get(), pos, state, MACHINE_SLOTS);
        ChemicalPlantRecipes.INSTANCE.registerRecipes();

        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY);
            outputTanks[i] = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY);
            allTanks[i] = inputTanks[i];
            allTanks[12 + i] = outputTanks[i];
        }
        water = new FluidTankNTM(Fluids.WATER, COOLANT_CAPACITY);
        lps = new FluidTankNTM(Fluids.SPENTSTEAM, COOLANT_CAPACITY);
        allTanks[24] = water;
        allTanks[25] = lps;

        for (int i = 0; i < LANES; i++) {
            chemplantModule[i] = new ModuleMachineChemplant(i, this, inventory)
                    .itemInput(inputStart(i), inputStart(i) + 1, inputStart(i) + 2)
                    .itemOutput(outputStart(i), outputStart(i) + 1, outputStart(i) + 2)
                    .fluidInput(inputTanks[i * 3], inputTanks[i * 3 + 1], inputTanks[i * 3 + 2])
                    .fluidOutput(outputTanks[i * 3], outputTanks[i * 3 + 1], outputTanks[i * 3 + 2]);
        }
    }

    public static int blueprintSlot(int lane) { return 4 + lane * 7; }
    public static int inputStart(int lane) { return 5 + lane * 7; }
    public static int outputStart(int lane) { return 8 + lane * 7; }

    public static boolean isInputSlot(int slot) {
        for (int lane = 0; lane < LANES; lane++) if (slot >= inputStart(lane) && slot < inputStart(lane) + 3) return true;
        return false;
    }

    public static boolean isOutputSlot(int slot) {
        for (int lane = 0; lane < LANES; lane++) if (slot >= outputStart(lane) && slot < outputStart(lane) + 3) return true;
        return false;
    }

    private static int laneForInput(int slot) {
        for (int lane = 0; lane < LANES; lane++) if (slot >= inputStart(lane) && slot < inputStart(lane) + 3) return lane;
        return -1;
    }

    @Override public String getDefaultName() { return "container.machineChemicalFactory"; }

    public boolean isProcessing() {
        for (boolean process : didProcess) if (process) return true;
        return false;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineChemicalFactory te) {
        for (DirPos port : te.getConPos()) {
            for (FluidTankNTM tank : te.inputTanks) {
                if (tank.getTankType() != Fluids.NONE) te.trySubscribe(tank.getTankType(), level, port);
            }
            for (FluidTankNTM tank : te.outputTanks) {
                if (tank.getFill() > 0) te.tryProvide(tank, level, port);
            }
        }

        for (DirPos port : te.getCoolPos()) {
            te.coolantDelegate.trySubscribe(te.water.getTankType(), level, port);
            te.coolantDelegate.tryProvide(te.lps, level, port);
        }

        if (te.maxPower <= 0) te.maxPower = 10_000_000L;
        long nextMaxPower = 0L;
        for (ModuleMachineChemplant module : te.chemplantModule) {
            GenericRecipe recipe = ChemicalPlantRecipes.INSTANCE.recipeNameMap.get(module.recipe);
            if (recipe != null) nextMaxPower += recipe.power * 100L;
        }
        te.maxPower = Math.max(te.power, Math.max(nextMaxPower, BASE_MAX_POWER));
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);
        te.upgradeManager.checkSlots(te.inventory, 1, 3);
        boolean containerChanged = false;
        for (int i = 0; i < 3; i++) {
            containerChanged |= MachineFluidContainerIO.load(te.inputTanks[i], te.inventory, 10 + i, 13 + i);
            containerChanged |= MachineFluidContainerIO.unload(te.outputTanks[i], te.inventory, 16 + i, 19 + i);
        }
        if (containerChanged) te.setChanged();

        double speed = 1D;
        double consumption = 1D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3) / 3D;
        speed += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        consumption -= Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3) * 0.25D;
        consumption += Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        consumption += Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3) * 10D / 3D;

        boolean dirty = false;
        for (int i = 0; i < LANES; i++) {
            ModuleMachineChemplant module = te.chemplantModule[i];
            module.update(speed * 2D, consumption * 2D, te.canCool(), te.inventory.getStackInSlot(blueprintSlot(i)));
            te.didProcess[i] = module.didProcess;
            dirty |= module.markDirty;
            if (module.didProcess) {
                te.water.setFill(te.water.getFill() - 100);
                te.lps.setFill(te.lps.getFill() + 100);
            }
        }

        // The original lets a matching output stream return to any matching input stream at 50 mB/t.
        for (FluidTankNTM input : te.inputTanks) if (input.getTankType() != Fluids.NONE) {
            for (FluidTankNTM output : te.outputTanks) {
                if (output.getTankType() == Fluids.NONE || output.getTankType() != input.getTankType()) continue;
                int move = Math.min(input.getMaxFill() - input.getFill(), Math.min(output.getFill(), 50));
                if (move > 0) {
                    input.setFill(input.getFill() + move);
                    output.setFill(output.getFill() - move);
                }
            }
        }

        if (dirty) te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineChemicalFactory te) {
        te.prevAnim = te.anim;
        if (te.isProcessing()) te.anim++;
        if (level.getGameTime() % 20L == 0L) te.frame = !level.getBlockState(pos.above(3)).isAir();
        ChemicalFactorySoundHandler.tickLoop(te);
    }

    public boolean canCool() {
        return water.getFill() >= 100 && lps.getFill() <= lps.getMaxFill() - 100;
    }

    public DirPos[] getConPos() {
        if (conPos == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            ForgeDirection dir = facing();
            ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
            conPos = new DirPos[]{
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
                    new DirPos(worldPosition.getX() + dir.offsetX * 2 + rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() + dir.offsetZ * 2 + rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() + dir.offsetX + rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() + dir.offsetZ + rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() + rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() + rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() - dir.offsetX + rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() - dir.offsetZ + rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() - dir.offsetX * 2 + rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() - dir.offsetZ * 2 + rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() + dir.offsetX * 2 - rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() + dir.offsetZ * 2 - rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() + dir.offsetX - rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() + dir.offsetZ - rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() - rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() - rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() - dir.offsetX - rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() - dir.offsetZ - rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() - dir.offsetX * 2 - rotation.offsetX * 2, y + 3,
                            worldPosition.getZ() - dir.offsetZ * 2 - rotation.offsetZ * 2, Library.POS_Y),
                    new DirPos(worldPosition.getX() + dir.offsetX + rotation.offsetX * 3, y,
                            worldPosition.getZ() + dir.offsetZ + rotation.offsetZ * 3, rotation),
                    new DirPos(worldPosition.getX() - dir.offsetX + rotation.offsetX * 3, y,
                            worldPosition.getZ() - dir.offsetZ + rotation.offsetZ * 3, rotation),
                    new DirPos(worldPosition.getX() + dir.offsetX - rotation.offsetX * 3, y,
                            worldPosition.getZ() + dir.offsetZ - rotation.offsetZ * 3, rotation.getOpposite()),
                    new DirPos(worldPosition.getX() - dir.offsetX - rotation.offsetX * 3, y,
                            worldPosition.getZ() - dir.offsetZ - rotation.offsetZ * 3, rotation.getOpposite())
            };
        }
        return conPos;
    }

    public DirPos[] getCoolPos() {
        if (coolPos == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            ForgeDirection dir = facing();
            ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
            coolPos = new DirPos[]{
                    new DirPos(x + rotation.offsetX + dir.offsetX * 3, y,
                            z + rotation.offsetZ + dir.offsetZ * 3, dir),
                    new DirPos(x - rotation.offsetX + dir.offsetX * 3, y,
                            z - rotation.offsetZ + dir.offsetZ * 3, dir),
                    new DirPos(x + rotation.offsetX - dir.offsetX * 3, y,
                            z + rotation.offsetZ - dir.offsetZ * 3, dir.getOpposite()),
                    new DirPos(x - rotation.offsetX - dir.offsetX * 3, y,
                            z - rotation.offsetZ - dir.offsetZ * 3, dir.getOpposite())
            };
        }
        return coolPos;
    }

    public DirPos[] getIOPos() {
        if (ioPos == null) {
            ForgeDirection dir = facing();
            ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
            ioPos = new DirPos[]{
                    new DirPos(worldPosition.getX() + dir.offsetX + rotation.offsetX * 3, worldPosition.getY(),
                            worldPosition.getZ() + dir.offsetZ + rotation.offsetZ * 3, rotation),
                    new DirPos(worldPosition.getX() - dir.offsetX + rotation.offsetX * 3, worldPosition.getY(),
                            worldPosition.getZ() - dir.offsetZ + rotation.offsetZ * 3, rotation),
                    new DirPos(worldPosition.getX() + dir.offsetX - rotation.offsetX * 3, worldPosition.getY(),
                            worldPosition.getZ() + dir.offsetZ - rotation.offsetZ * 3, rotation.getOpposite()),
                    new DirPos(worldPosition.getX() - dir.offsetX - rotation.offsetX * 3, worldPosition.getY(),
                            worldPosition.getZ() - dir.offsetZ - rotation.offsetZ * 3, rotation.getOpposite())
            };
        }
        return ioPos;
    }

    private ForgeDirection facing() {
        int meta = getBlockState().getValue(com.hbm.blocks.BlockDummyable.META);
        return ForgeDirection.getOrientation(meta - com.hbm.blocks.BlockDummyable.offset);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot >= 1 && slot <= 3) return Library.isMachineUpgrade(stack);
        if (slot >= 0 && slot < MACHINE_SLOTS && isOutputSlot(slot)) return false;
        if (slot >= 0 && slot < MACHINE_SLOTS && slot % 7 == 4) return stack.getItem() instanceof ItemBlueprints;
        int lane = laneForInput(slot);
        return lane >= 0 && chemplantModule[lane].isItemValidForPool(slot, stack,
                inventory.getStackInSlot(blueprintSlot(lane)));
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS.clone(); }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return IO_SLOTS.clone(); }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return isInputSlot(slot) && isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return isOutputSlot(slot); }

    @Override protected int[] getUpgradeSlotRange() { return new int[]{1, 3}; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return outputTanks; }
    @Override public FluidTankNTM[] getAllTanks() { return allTanks; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ChemicalFactoryRegistrations.MACHINE_CHEMICAL_FACTORY.get()));
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
        HashMap<UpgradeType, Integer> valid = new HashMap<>();
        valid.put(UpgradeType.SPEED, 3);
        valid.put(UpgradeType.POWER, 3);
        valid.put(UpgradeType.OVERDRIVE, 3);
        return valid;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (!data.contains("index") || !data.contains("selection")) return;
        int index = Tags.getIntOr(data, "index", -1);
        if (index >= 0 && index < LANES) {
            chemplantModule[index].setRecipe(Tags.getStringOr(data, "selection", "null"), false);
            markChanged();
        }
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.containerMenu instanceof com.hbm.inventory.container.ContainerMachineChemicalFactory menu
                && menu.getFactory() == this && menu.stillValid(player);
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
            if ((PREFIX_VALUE + "progress" + (i + 1)).equals(name)) return "" + (int) Math.round(chemplantModule[i].progress * 100);
            if ((PREFIX_VALUE + "recipe" + (i + 1)).equals(name)) return chemplantModule[i].recipe;
            if ((PREFIX_VALUE + "active" + (i + 1)).equals(name)) return "" + (didProcess[i] ? 1 : 0);
        }
        return null;
    }

    @Override public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName())
                : Component.translatable("block.hbm.machine_chemical_factory");
    }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.hbm.inventory.container.ContainerMachineChemicalFactory(id, inventory, this);
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
        for (boolean process : didProcess) buf.writeBoolean(process);
        for (ModuleMachineChemplant module : chemplantModule) module.serialize(buf);
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
        for (ModuleMachineChemplant module : chemplantModule) module.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].writeToNBT(output, "i" + i);
            // The old source used i* for both arrays; separate keys preserve all 24 tank values.
            outputTanks[i].writeToNBT(output, "o" + i);
        }
        water.writeToNBT(output, "w");
        lps.writeToNBT(output, "s");
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        for (ModuleMachineChemplant module : chemplantModule) module.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < inputTanks.length; i++) {
            inputTanks[i].readFromNBT(input, "i" + i);
            outputTanks[i].readFromNBT(input, "o" + i);
        }
        water.readFromNBT(input, "w");
        lps.readFromNBT(input, "s");
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", BASE_MAX_POWER);
        for (ModuleMachineChemplant module : chemplantModule) module.load(input);
    }
}
