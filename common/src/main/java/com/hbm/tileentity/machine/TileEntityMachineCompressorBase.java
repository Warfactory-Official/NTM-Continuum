package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerCompressor;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CompressorRecipes;
import com.hbm.inventory.recipes.CompressorRecipes.CompressorRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.List;

/**
 * Shared base of the compressor — 1:1 port of the 1.12.2 {@code TileEntityMachineCompressorBase}. Raises
 * a fluid's <b>pressure</b>, the gate the vacuum refinery (OIL @2) and hydrotreater (HYDROGEN @1) sit
 * behind. 4 slots (0 fluid identifier, 1 battery, 2-3 upgrades); tanks {@code [0] 16k} input (type via
 * slot 0, pressure via the GUI's 5-way compression selector) → {@code [1] 16k} output.
 *
 * <p>Per {@link CompressorRecipes}: with no recipe it's the generic 1000 mB → 1000 mB of the same type at
 * {@code pressure + 1}; with one, the recipe's own amounts/type/pressure. maxPower 100k, base process
 * 100t @ 2500 HE/t. SPEED/POWER ×3 + OVERDRIVE ×<b>9</b> (note: not 3), formulas verbatim.
 *
 * <p>Deferred (§20): the compact variant ({@code machine_compressor_compact}), IFluidCopiable paste,
 * the operating sound.
 */
public abstract class TileEntityMachineCompressorBase extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IUpgradeInfoProvider {

    public FluidTankNTM[] tanks;
    public long power;
    public static final long maxPower = 100_000;
    public boolean isOn;
    public int progress;
    public int processTime = 100;
    public static final int processTimeBase = 100;
    public int powerRequirement;
    public static final int powerRequirementBase = 2_500;
    /** The compression selector's range (old GUI draws 5 buttons, j = 0..4). */
    public static final int MAX_COMPRESSION = 4;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineCompressorBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.NONE, 16_000);
        tanks[1] = new FluidTankNTM(Fluids.NONE, 16_000).withPressure(1);
    }

    @Override public String getDefaultName() { return "container.machineCompressor"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCompressorBase te) {
        if (level.getGameTime() % 20 == 0) te.updateConnections();

        te.power = Library.chargeTEFromItems(te.inventory, 1, te.power, maxPower);
        te.setTypeFromIdentifier();
        te.setupTanks();

        te.upgradeManager.checkSlots(te.inventory, 1, 3);
        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        CompressorRecipe rec = CompressorRecipes.getRecipe(te.tanks[0].getTankType(), te.tanks[0].getPressure());
        int timeBase = rec != null ? rec.duration() : processTimeBase;

        // Old quirk kept verbatim: the generic (recipe-less) path uses a hardcoded speed ladder, the
        // recipe path a plain divisor.
        if (rec == null) te.processTime = speedLevel == 3 ? 10 : speedLevel == 2 ? 20 : speedLevel == 1 ? 60 : timeBase;
        else te.processTime = timeBase / (speedLevel + 1);
        te.powerRequirement = powerRequirementBase / (powerLevel + 1);
        te.processTime = te.processTime / (overLevel + 1);
        te.powerRequirement = te.powerRequirement * ((overLevel * 2) + 1);

        if (te.processTime <= 0) te.processTime = 1;

        if (te.canProcess()) {
            te.progress++;
            te.isOn = true;
            te.power -= te.powerRequirement;

            if (te.progress >= te.processTime) {
                te.progress = 0;
                te.process();
                te.setChanged();
            }
        } else {
            te.progress = 0;
            te.isOn = false;
        }

        for (DirPos dp : te.getConPos()) te.tryProvide(te.tanks[1], level, dp);

        te.networkPackNT();
    }

    protected void updateConnections() {
        for (DirPos dp : getConPos()) {
            this.trySubscribe(level, dp);
            this.trySubscribe(tanks[0].getTankType(), level, dp);
        }
    }

    public abstract DirPos[] getConPos();

    public boolean canProcess() {
        if (this.power <= powerRequirement) return false;

        CompressorRecipe recipe = CompressorRecipes.getRecipe(tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            return tanks[0].getFill() >= 1000 && tanks[1].getFill() + 1000 <= tanks[1].getMaxFill();
        }
        return tanks[0].getFill() >= recipe.inputAmount() && tanks[1].getFill() + recipe.output().fill <= tanks[1].getMaxFill();
    }

    public void process() {
        CompressorRecipe recipe = CompressorRecipes.getRecipe(tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            tanks[0].setFill(tanks[0].getFill() - 1_000);
            tanks[1].setFill(tanks[1].getFill() + 1_000);
        } else {
            tanks[0].setFill(tanks[0].getFill() - recipe.inputAmount());
            tanks[1].setFill(tanks[1].getFill() + recipe.output().fill);
        }
    }

    /** Old setupTanks: no recipe → same type at pressure+1; else the recipe's output type/pressure. */
    protected void setupTanks() {
        CompressorRecipe recipe = CompressorRecipes.getRecipe(tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            tanks[1].withPressure(tanks[0].getPressure() + 1).setTankType(tanks[0].getTankType());
        } else {
            tanks[1].withPressure(recipe.output().pressure).setTankType(recipe.output().type);
        }
    }

    /** Slot 0 fluid identifier types the input tank (old {@code tanks[0].setType(0, inventory)}). */
    private void setTypeFromIdentifier() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidIdentifier ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tanks[0].getTankType()) {
            tanks[0].setTankType(t);
            setChanged();
        }
    }

    /** Old {@code receiveControl}'s "compression" — the GUI's 5-way input-pressure selector (button id = level). */
    public void setCompression(int compression) {
        if (compression < 0 || compression > MAX_COMPRESSION) return;
        if (compression == tanks[0].getPressure()) return;

        tanks[0].withPressure(compression);
        CompressorRecipe recipe = CompressorRecipes.getRecipe(tanks[0].getTankType(), compression);
        if (recipe == null) {
            tanks[1].withPressure(compression + 1);
        } else {
            tanks[1].withPressure(recipe.output().pressure).setTankType(recipe.output().type);
        }
        setChanged();
    }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return processTime <= 0 ? 0 : progress * i / processTime; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 1) return Library.isBattery(stack);
        if (slot == 2 || slot == 3) return Library.isMachineUpgrade(stack);
        return true;
    }

    @Override
    protected int[] getUpgradeSlotRange() { return new int[]{2, 3}; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_compressor.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.literal("Generic compression: ").append(
                    Component.translatable(KEY_DELAY, "-" + (level == 3 ? 90 : level == 2 ? 80 : level == 1 ? 40 : 0) + "%")).withStyle(ChatFormatting.GREEN));
            info.add(Component.literal("Recipe: ").append(
                    Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")).withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 9);
        return upgrades;
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.machineCompressor"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCompressor(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeInt(powerRequirement);
        buf.writeLong(power);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeBoolean(isOn);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        progress = buf.readInt();
        processTime = buf.readInt();
        powerRequirement = buf.readInt();
        power = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        isOn = buf.readBoolean();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tanks[0].writeToNBT(output, "0");
        tanks[1].writeToNBT(output, "1");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
        tanks[0].readFromNBT(input, "0");
        tanks[1].readFromNBT(input, "1");
    }
}
