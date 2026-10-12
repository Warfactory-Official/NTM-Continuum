package com.hbm.tileentity.machine;

import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.data.Tags;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.sound.PrecisionAssemblerSoundHandler;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachinePrecAss;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PrecAssRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.ItemBlueprints;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.modules.machine.ModuleMachinePrecAss;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.TickPhase;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
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

/** Precision assembler tile entity, ported from the old four-striker machine. */
public class TileEntityMachinePrecAss extends TileEntityMachineBase implements MenuProvider,
        IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IControlReceiver, WorldlyContainer, IUpgradeInfoProvider {

    private static final int[] IO_SLOTS = {
            4, 5, 6, 7, 8, 9, 10, 11, 12,
            13, 14, 15, 16, 17, 18, 19, 20, 21
    };

    public final FluidTankNTM inputTank;
    public final FluidTankNTM outputTank;
    public long power;
    public long maxPower = 100_000;
    public boolean didProcess;
    public boolean frame;
    public final ModuleMachinePrecAss assemblerModule;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    // Client-only animation state. It intentionally keeps the old shared four-striker state machine.
    public double prevRing;
    public double ring;
    public double ringSpeed;
    public double ringTarget;
    public int ringDelay;
    public final double[] armAngles = {45, -15, -5};
    public final double[] prevArmAngles = {45, -15, -5};
    public final double[] strikers = new double[4];
    public final double[] prevStrikers = new double[4];
    public final boolean[] strikerDir = new boolean[4];
    private int strikerIndex;
    private int strikerDelay;

    private static final double[] NULL_POSITION = {45, -30, 45};
    private static final double[] WORKING_POSITION = {45, -15, -5};

    public TileEntityMachinePrecAss(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PREC_ASS.get(), pos, state, 22);
        PrecAssRecipes.INSTANCE.registerRecipes();
        inputTank = new FluidTankNTM(Fluids.NONE, 4_000);
        outputTank = new FluidTankNTM(Fluids.NONE, 4_000);
        assemblerModule = new ModuleMachinePrecAss(0, this, inventory)
                .itemInput(4).itemOutput(13)
                .fluidInput(inputTank).fluidOutput(outputTank);
    }

    @Override
    public String getDefaultName() {
        return "container.machinePrecAss";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePrecAss te) {
        GenericRecipe recipe = PrecAssRecipes.INSTANCE.getRecipe(te.assemblerModule.recipe);
        if (te.maxPower <= 0L) te.maxPower = 1_000_000L;
        if (recipe != null) te.maxPower = recipe.power * 100L;
        te.maxPower = Math.max(Math.max(te.power, te.maxPower), 100_000L);
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.maxPower);

        te.upgradeManager.checkSlots(te.inventory, 2, 3);
        for (DirPos connection : te.getConPos()) {
            te.trySubscribe(level, connection);
            if (te.inputTank.getTankType() != com.hbm.inventory.fluid.Fluids.NONE) te.trySubscribe(te.inputTank.getTankType(), level, connection);
            if (te.outputTank.getFill() > 0) te.tryProvide(te.outputTank, level, connection);
        }

        int speedLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int overdriveLevel = Math.min(te.upgradeManager.getLevel(UpgradeType.OVERDRIVE), 3);
        double speed = 1D + speedLevel / 3D + overdriveLevel;
        double consumption = 1D - powerLevel * 0.25D + speedLevel + overdriveLevel * 10D / 3D;

        te.assemblerModule.update(speed, consumption, true, te.inventory.getStackInSlot(1));
        te.didProcess = te.assemblerModule.didProcess;
        if (te.assemblerModule.markDirty) te.setChanged();
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePrecAss te) {
        boolean running = te.didProcess;
        PrecisionAssemblerSoundHandler.tickLoop(te);
        if (TickPhase.every(level, pos, 20L)) te.frame = !level.getBlockState(pos.above(3)).isAir();

        System.arraycopy(te.armAngles, 0, te.prevArmAngles, 0, 3);
        System.arraycopy(te.strikers, 0, te.prevStrikers, 0, 4);
        te.prevRing = te.ring;

        for (int i = 0; i < 4; i++) {
            if (te.strikerDir[i]) {
                te.strikers[i] = -0.75D;
                te.strikerDir[i] = false;
                if (!te.muffled) PrecisionAssemblerSoundHandler.playStrike(level, pos, te.getVolume(0.5F));
            } else {
                te.strikers[i] = Math.max(-0.75D, Math.min(te.strikers[i] + 0.5D, 0D));
            }
        }

        if (te.ring != te.ringTarget) {
            double delta = Math.abs(te.ringTarget - te.ring);
            if (delta <= te.ringSpeed) te.ring = te.ringTarget;
            if (te.ringTarget > te.ring) te.ring += te.ringSpeed;
            if (te.ringTarget < te.ring) te.ring -= te.ringSpeed;
            if (te.ringTarget == te.ring) {
                double sub = te.ringTarget >= 360 ? -360D : 360D;
                te.ringTarget += sub;
                te.ring += sub;
                te.prevRing += sub;
                te.ringDelay = 100 + level.getRandom().nextInt(21);
            }
        }

        if (running) {
            if (te.ring == te.ringTarget) {
                if (te.ringDelay > 0) te.ringDelay--;
                if (te.ringDelay <= 0) {
                    te.ringTarget += 45 * (level.getRandom().nextBoolean() ? -1 : 1);
                    te.ringSpeed = 10D + level.getRandom().nextDouble() * 5D;
                    if (!te.muffled) PrecisionAssemblerSoundHandler.playStart(level, pos, te.getVolume(0.25F),
                            1.25F + level.getRandom().nextFloat() * 0.25F);
                }
            }
            if (!te.isInWorkingPosition(te.armAngles) && te.canArmsMove()) te.move(WORKING_POSITION);
            if (te.isInWorkingPosition(te.armAngles)) {
                te.strikerDelay--;
                if (te.strikerDelay <= 0) {
                    te.strikerDir[te.strikerIndex] = true;
                    te.strikerIndex = (te.strikerIndex + 1) % te.strikers.length;
                    te.strikerDelay = te.strikerIndex == 3 ? 10 + level.getRandom().nextInt(3) : 2;
                }
            }
        } else {
            for (int i = 0; i < 4; i++) te.strikerDir[i] = false;
            if (te.canArmsMove()) te.move(NULL_POSITION);
        }

        if (!te.muffled && te.isInWorkingPosition(te.prevArmAngles) && !te.isInWorkingPosition(te.armAngles)) {
            PrecisionAssemblerSoundHandler.playStop(level, pos, te.getVolume(0.25F),
                    1.25F + level.getRandom().nextFloat() * 0.25F);
        }
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z - 1, Library.POS_X), new DirPos(x + 2, y, z, Library.POS_X), new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X), new DirPos(x - 2, y, z, Library.NEG_X), new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 1, y, z + 2, Library.POS_Z), new DirPos(x, y, z + 2, Library.POS_Z), new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z), new DirPos(x, y, z - 2, Library.NEG_Z), new DirPos(x + 1, y, z - 2, Library.NEG_Z)
        };
    }

    private boolean canArmsMove() {
        for (double striker : strikers) if (striker != 0) return false;
        return true;
    }

    private boolean isInWorkingPosition(double[] arms) {
        for (int i = 0; i < 3; i++) if (arms[i] != WORKING_POSITION[i]) return false;
        return true;
    }

    private void move(double[] targetAngles) {
        for (int i = 0; i < armAngles.length; i++) {
            if (armAngles[i] == targetAngles[i]) continue;
            double delta = Math.abs(armAngles[i] - targetAngles[i]);
            if (delta <= 15D) armAngles[i] = targetAngles[i];
            else armAngles[i] += armAngles[i] < targetAngles[i] ? 15D : -15D;
        }
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return true;
        if (slot == 1) return stack.getItem() instanceof ItemBlueprints;
        if (slot >= 2 && slot <= 3) return Library.isMachineUpgrade(stack);
        if (slot >= 13 && slot <= 21) return false;
        return assemblerModule.isItemValidForPool(slot, stack, inventory.getStackInSlot(1));
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 4 && slot <= 12 && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 13 || assemblerModule.isSlotClogged(slot);
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{2, 3};
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_precass.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_SPEED, "+" + (level * 100 / 3) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 50) + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
        } else if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink() ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public HashMap<UpgradeType, Integer> getValidUpgrades() {
        HashMap<UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(UpgradeType.SPEED, 3);
        upgrades.put(UpgradeType.POWER, 3);
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{inputTank}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{outputTank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{inputTank, outputTank}; }

    @Override
    public Component getDisplayName() {
        return hasCustomName() ? Component.literal(getName())
                : Component.translatable("container.machinePrecAss");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachinePrecAss(containerId, playerInventory, this);
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("index") && data.contains("selection") && Tags.getIntOr(data, "index", 0) == 0) {
            assemblerModule.recipe = Tags.getStringOr(data, "selection", "null");
            assemblerModule.progress = 0D;
            setChanged();
        }
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        inputTank.serialize(buf);
        outputTank.serialize(buf);
        buf.writeLong(power);
        buf.writeLong(maxPower);
        buf.writeBoolean(didProcess);
        assemblerModule.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        inputTank.deserialize(buf);
        outputTank.deserialize(buf);
        power = buf.readLong();
        maxPower = buf.readLong();
        didProcess = buf.readBoolean();
        assemblerModule.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        inputTank.writeToNBT(output, "i");
        outputTank.writeToNBT(output, "o");
        output.putLong("power", power);
        output.putLong("maxPower", maxPower);
        assemblerModule.save(output);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        inputTank.readFromNBT(input, "i");
        outputTank.readFromNBT(input, "o");
        power = input.getLongOr("power", 0L);
        maxPower = input.getLongOr("maxPower", 100_000L);
        assemblerModule.load(input);
    }
}
