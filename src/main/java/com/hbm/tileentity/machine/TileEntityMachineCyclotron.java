package com.hbm.tileentity.machine;

import dev.golem.core.Ids;
import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineCyclotron;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CyclotronRecipes;
import com.hbm.inventory.recipes.CyclotronRecipes.CyclotronRecipe;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.List;
import com.hbm.tileentity.TilePort.PortDef;

/** Standard cyclotron, ported from 1.12.2 {@code TileEntityMachineCyclotron}. */
public class TileEntityMachineCyclotron extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 100_000_000L;
    public static final int BASE_CONSUMPTION = 1_000_000;
    public static final int PROCESSING_TIME = 690;

    private static final int[] OUTPUT_SLOTS = {6, 7, 8};

    public long power;
    public int progress;
    public boolean isProgressing;
    private byte plugs;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.WATER, 32_000),
            new FluidTankNTM(Fluids.SPENTSTEAM, 32_000),
            new FluidTankNTM(Fluids.AMAT, 8_000)
    };
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineCyclotron(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CYCLOTRON.get(), pos, state, 12);
        CyclotronRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.cyclotron";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCyclotron te) {
        te.setupAllPorts(te.getPorts());
        te.updatePortPIFIFO();

        te.power = Library.chargeTEFromItems(te.inventory, 9, te.power, MAX_POWER);
        te.upgradeManager.checkSlots(te.inventory, 10, 11);

        te.isProgressing = te.canProcess();
        if (te.isProgressing) {
            te.progress += te.getSpeed();
            te.power -= te.getConsumption();

            int coolant = te.getCoolantConsumption();
            te.tanks[0].setFill(te.tanks[0].getFill() - coolant);
            te.tanks[1].setFill(te.tanks[1].getFill() + coolant);

            if (te.progress >= PROCESSING_TIME) {
                te.process();
                te.progress = 0;
                te.setChanged();
            }
        } else {
            te.progress = 0;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCyclotron te) {}

    public boolean canProcess() {
        if (power < getConsumption()) return false;

        int coolant = getCoolantConsumption();
        if (tanks[0].getFill() < coolant || tanks[1].getFill() + coolant > tanks[1].getMaxFill()) return false;

        for (int i = 0; i < 3; i++) {
            CyclotronRecipe recipe = CyclotronRecipes.getRecipe(inventory.getStackInSlot(i), inventory.getStackInSlot(i + 3));
            if (recipe != null && canFitOutput(i + 6, recipe.output)) return true;
        }
        return false;
    }

    private boolean canFitOutput(int slot, ItemStack output) {
        ItemStack existing = inventory.getStackInSlot(slot);
        return existing.isEmpty()
                || (ItemStacks.sameItemAndData(existing, output)
                && existing.getCount() < output.getMaxStackSize());
    }

    private void process() {
        for (int i = 0; i < 3; i++) {
            CyclotronRecipe recipe = CyclotronRecipes.getRecipe(inventory.getStackInSlot(i), inventory.getStackInSlot(i + 3));
            if (recipe == null) continue;

            ItemStack output = inventory.getStackInSlot(i + 6);
            if (output.isEmpty()) {
                inventory.extractItem(i, 1, false);
                inventory.extractItem(i + 3, 1, false);
                inventory.setStackInSlot(i + 6, recipe.output.copy());
                tanks[2].setFill(tanks[2].getFill() + recipe.antimatter);
            } else if (ItemStacks.sameItemAndData(output, recipe.output)
                    && output.getCount() < output.getMaxStackSize()) {
                inventory.extractItem(i, 1, false);
                inventory.extractItem(i + 3, 1, false);
                output.grow(1);
                tanks[2].setFill(tanks[2].getFill() + recipe.antimatter);
            }
        }
        if (tanks[2].getFill() > tanks[2].getMaxFill()) tanks[2].setFill(tanks[2].getMaxFill());
    }

    public int getSpeed() {
        return upgradeManager.getLevel(UpgradeType.SPEED) + 1;
    }

    public int getConsumption() {
        return BASE_CONSUMPTION - 100_000 * upgradeManager.getLevel(UpgradeType.POWER);
    }

    public int getCoolantConsumption() {
        return 500 / (upgradeManager.getLevel(UpgradeType.EFFECT) + 1) * getSpeed();
    }

    public long getPowerScaled(long scale) {
        return power * scale / MAX_POWER;
    }

    public int getProgressScaled(int scale) {
        return progress * scale / PROCESSING_TIME;
    }

    protected PortDef[] cachedPorts;
    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + 2, y, z - 1, Library.POS_X),
                    PortDef.make(x + 2, y, z, Library.POS_X),
                    PortDef.make(x + 2, y, z + 1, Library.POS_X),
                    PortDef.make(x - 2, y, z - 1, Library.NEG_X),
                    PortDef.make(x - 2, y, z, Library.NEG_X),
                    PortDef.make(x - 2, y, z + 1, Library.NEG_X),
                    PortDef.make(x - 1, y, z + 2, Library.POS_Z),
                    PortDef.make(x, y, z + 2, Library.POS_Z),
                    PortDef.make(x + 1, y, z + 2, Library.POS_Z),
                    PortDef.make(x - 1, y, z - 2, Library.NEG_Z),
                    PortDef.make(x, y, z - 2, Library.NEG_Z),
                    PortDef.make(x + 1, y, z - 2, Library.NEG_Z),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    /** Recreates the old proxy-relative hopper rows for the modern WorldlyContainerHolder adapter. */
    public int[] getAccessibleSlotsFromSide(BlockPos accessor) {
        int dx = accessor.getX() - worldPosition.getX();
        int dz = accessor.getZ() - worldPosition.getZ();
        for (int i = 2; i < 6; i++) {
            ForgeDirection side = ForgeDirection.getOrientation(i);
            ForgeDirection rotated = side.getRotation(ForgeDirection.UP);
            if (matches(dx, dz, side.offsetX * 2 + rotated.offsetX, side.offsetZ * 2 + rotated.offsetZ)) {
                return new int[]{0, 3, 6, 7, 8};
            }
            if (matches(dx, dz, side.offsetX * 2, side.offsetZ * 2)) {
                return new int[]{1, 4, 6, 7, 8};
            }
            if (matches(dx, dz, side.offsetX * 2 - rotated.offsetX, side.offsetZ * 2 - rotated.offsetZ)) {
                return new int[]{2, 5, 6, 7, 8};
            }
        }
        return OUTPUT_SLOTS;
    }

    private static boolean matches(int x, int z, int expectedX, int expectedZ) {
        return x == expectedX && z == expectedZ;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot < 3) return CyclotronRecipes.isAnyParticle(stack);
        if (slot < 6) return CyclotronRecipes.isAnyInput(stack);
        if (slot == 9) return Library.isBattery(stack);
        return slot == 10 || slot == 11 ? Library.isMachineUpgrade(stack) : false;
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{10, 11};
    }

    @Override public int[] getSlotsForFace(Direction side) { return OUTPUT_SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return OUTPUT_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot < 6 && isItemValidForSlot(slot, stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 6 && slot <= 8;
    }

    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot >= 6 && slot <= 8;
    }

    public void setPlug(int index) {
        plugs |= (byte) (1 << index);
        setChanged();
    }

    public boolean getPlug(int index) {
        return (plugs & (1 << index)) != 0;
    }

    /** Returns an installed plug's canonical item, or null while that content is not registered. */
    public static Item getItemForPlug(int index) {
        if (index < 0 || index >= PLUG_IDS.length) return null;
        return BuiltInRegistries.ITEM
                .getOptional(Ids.of("hbm", PLUG_IDS[index])).orElse(null);
    }

    private static final String[] PLUG_IDS = {"powder_balefire", "book_of_", "diamond_gavel", "coin_maskman"};

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], tanks[2]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public long getReceiverSpeed() { return MAX_POWER; }

    @Override public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.EFFECT;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_cyclotron.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")
                    .withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_COOLANT_CONSUMPTION, "+" + level * 100 + "%")
                    .withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + level * 10 + "%")
                    .withStyle(ChatFormatting.GREEN));
        } else if (type == UpgradeType.EFFECT) {
            info.add(Component.translatable(KEY_COOLANT_CONSUMPTION,
                    "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.EFFECT, 3);
        return upgrades;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineCyclotron(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeByte(plugs);
        for (FluidTankNTM tank : tanks) tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        plugs = buf.readByte();
        isProgressing = progress > 0;
        for (FluidTankNTM tank : tanks) tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        for (int i = 0; i < tanks.length; i++) tanks[i].writeToNBT(output, "t" + i);
        output.putInt("progress", progress);
        output.putLong("power", power);
        output.putByte("plugs", plugs);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        for (int i = 0; i < tanks.length; i++) tanks[i].readFromNBT(input, "t" + i);
        progress = input.getIntOr("progress", 0);
        power = input.getLongOr("power", 0L);
        plugs = input.getByteOr("plugs", (byte) 0);
        isProgressing = progress > 0;
    }
}
