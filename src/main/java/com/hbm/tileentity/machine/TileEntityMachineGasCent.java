package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.GasCentrifugeRecipes.PseudoFluidType;
import com.hbm.items.ModItems;
import com.hbm.items.ModUpgradeItems;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.InventoryUtil;
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

import java.util.HashMap;
import java.util.List;

/** Gas centrifuge, including the legacy pseudo-fluid cascade between aligned machines. */
public class TileEntityMachineGasCent extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardReceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 100_000;
    public static final int PROCESSING_SPEED = 150;
    public static final int BASE_CONSUMPTION = 200;

    private static final int[] IO_SLOTS = {0, 1, 2, 3};

    public long power;
    public int progress;
    public boolean isProgressing;
    public int audioDuration;

    public final FluidTankNTM tank = new FluidTankNTM(Fluids.UF6, 2_000);
    public final PseudoFluidTank inputTank = new PseudoFluidTank(PseudoFluidType.NUF6, 8_000);
    public final PseudoFluidTank outputTank = new PseudoFluidTank(PseudoFluidType.LEUF6, 8_000);
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineGasCent(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_GAS_CENT.get(), pos, state, 7);
        GasCentrifugeRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.gasCentrifuge";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasCent te) {
        GasCentrifugeRecipes.register();
        for (DirPos port : te.getConPos()) te.trySubscribe(level, port);

        te.power = Library.chargeTEFromItems(te.inventory, 4, te.power, MAX_POWER);
        te.setTankTypeFromIdentifier();
        if (te.tank.getTankType() != Fluids.NONE) {
            for (DirPos port : te.getConPos()) te.trySubscribe(te.tank.getTankType(), level, port);
        }

        if (GasCentrifugeRecipes.FLUID_CONVERSIONS.containsValue(te.inputTank.getTankType())) {
            te.attemptConversion();
        }

        te.upgradeManager.checkSlots(te.inventory, 6, 6);
        if (te.canEnrich()) {
            te.isProgressing = true;
            te.progress++;
            te.power -= te.hasSpeedUpgrade() ? 300 : BASE_CONSUMPTION;
            if (te.power < 0) {
                te.power = 0;
                te.progress = 0;
            }
            if (te.progress >= te.getProcessingSpeed()) te.enrich();
        } else {
            te.isProgressing = false;
            te.progress = 0;
        }

        if (level.getGameTime() % 10 == 0) te.transferCascade(level);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineGasCent te) {
        te.audioDuration += te.isProgressing ? 2 : -3;
        te.audioDuration = Math.max(0, Math.min(60, te.audioDuration));
        com.hbm.client.sound.GasCentrifugeSoundHandler.tickLoop(te);
    }

    private boolean canEnrich() {
        PseudoFluidType type = inputTank.getTankType();
        ItemStack[] output = type.getOutput();
        if (power <= 0 || inputTank.getFill() < type.getFluidConsumed()
                || outputTank.getFill() + type.getFluidProduced() > outputTank.getMaxFill()
                || output == null || output.length == 0) return false;
        if (type.getIfHighSpeed() && !hasSpeedUpgrade()) return false;

        for (int i = 0; i < output.length; i++) {
            if (i >= 4 || !inventory.insertItem(i, output[i], true).isEmpty()) return false;
        }
        return true;
    }

    private void enrich() {
        PseudoFluidType type = inputTank.getTankType();
        progress = 0;
        inputTank.setFill(inputTank.getFill() - type.getFluidConsumed());
        outputTank.setFill(outputTank.getFill() + type.getFluidProduced());
        for (int i = 0; i < type.getOutput().length; i++) {
            InventoryUtil.tryAddItemToInventory(inventory, 0, 3, type.getOutput()[i].copy());
        }
        setChanged();
    }

    private void attemptConversion() {
        if (inputTank.getFill() >= inputTank.getMaxFill() || tank.getFill() <= 0) return;
        int fill = Math.min(inputTank.getMaxFill() - inputTank.getFill(), tank.getFill());
        tank.setFill(tank.getFill() - fill);
        inputTank.setFill(inputTank.getFill() + fill);
    }

    private void transferCascade(Level level) {
        ForgeDirection facing = ForgeDirection.getOrientation(getBlockState().getValue(com.hbm.blocks.BlockDummyable.META) - com.hbm.blocks.BlockDummyable.offset);
        BlockPos adjacent = worldPosition.offset(-facing.offsetX, 0, -facing.offsetZ);
        if (level.getBlockEntity(adjacent) instanceof TileEntityMachineGasCent target
                && target.tank.getFill() == 0 && target.tank.getTankType() == tank.getTankType()) {
            if (target.inputTank.getTankType() != outputTank.getTankType()
                    && outputTank.getTankType() != PseudoFluidType.NONE) {
                target.inputTank.setTankType(outputTank.getTankType());
                target.outputTank.setTankType(outputTank.getTankType().getOutputType());
            }
            int fill = Math.min(target.inputTank.getMaxFill() - target.inputTank.getFill(), outputTank.getFill());
            if (fill > 0) {
                outputTank.setFill(outputTank.getFill() - fill);
                target.inputTank.setFill(target.inputTank.getFill() + fill);
                target.setChanged();
            }
            return;
        }

        // The legacy machine turns the final LEUF6 output into fuel nuggets when no cascade target exists.
        if (inputTank.getTankType() == PseudoFluidType.LEUF6 && outputTank.getFill() >= 600) {
            ItemStack[] converted = {new ItemStack(ModItems.nugget_uranium_fuel.get(), 6),
                    new ItemStack(ModItems.fluorite.get())};
            for (int i = 0; i < converted.length; i++) {
                if (!inventory.insertItem(i, converted[i], true).isEmpty()) return;
            }
            outputTank.setFill(outputTank.getFill() - 600);
            for (ItemStack stack : converted) InventoryUtil.tryAddItemToInventory(inventory, 0, 3, stack);
            setChanged();
        }
    }

    private boolean hasSpeedUpgrade() {
        return inventory.getStackInSlot(6).is(ModUpgradeItems.upgrade_gc_speed.get());
    }

    public int getProcessingSpeed() {
        return hasSpeedUpgrade() ? PROCESSING_SPEED - 70 : PROCESSING_SPEED;
    }

    public int getProgressScaled(int scale) {
        return progress * scale / getProcessingSpeed();
    }

    public long getPowerScaled(long scale) {
        return power * scale / MAX_POWER;
    }

    private void setTankTypeFromIdentifier() {
        ItemStack id = inventory.getStackInSlot(5);
        if (!(id.getItem() instanceof IItemFluidId identifier)) return;
        com.hbm.inventory.fluid.FluidType type = identifier.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        PseudoFluidType pseudo = GasCentrifugeRecipes.FLUID_CONVERSIONS.get(type);
        if (pseudo == null || tank.getTankType() == type) return;
        tank.setTankType(type);
        inputTank.setTankType(pseudo);
        outputTank.setTankType(pseudo.getOutputType());
        setChanged();
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x, y - 1, z, Library.NEG_Y),
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z)
        };
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tank}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0, 1, 2, 3 -> true;
            case 4 -> Library.isBattery(stack);
            case 5 -> stack.getItem() instanceof IItemFluidId;
            case 6 -> Library.isMachineUpgrade(stack);
            default -> false;
        };
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public int[] getAccessibleSlotsFromSide(int side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot < 4; }

    @Override public Component getDisplayName() { return Component.translatable("container.gasCentrifuge"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new com.hbm.inventory.container.ContainerMachineGasCent(id, inventory, this);
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }

    @Override public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) { return false; }
    @Override public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {}
    @Override public HashMap<UpgradeType, Integer> getValidUpgrades() { return new HashMap<>(); }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeBoolean(isProgressing);
        buf.writeUtf(inputTank.getTankType().getName());
        buf.writeUtf(outputTank.getTankType().getName());
        buf.writeInt(inputTank.getFill());
        buf.writeInt(outputTank.getFill());
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        isProgressing = buf.readBoolean();
        inputTank.setTankType(PseudoFluidType.fromName(buf.readUtf()));
        outputTank.setTankType(PseudoFluidType.fromName(buf.readUtf()));
        inputTank.setFill(buf.readInt());
        outputTank.setFill(buf.readInt());
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
        tank.writeToNBT(output, "tank");
        inputTank.writeToNBT(output, "inputTank");
        outputTank.writeToNBT(output, "outputTank");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
        tank.readFromNBT(input, "tank");
        inputTank.readFromNBT(input, "inputTank");
        outputTank.readFromNBT(input, "outputTank");
    }

    public static final class PseudoFluidTank {
        private PseudoFluidType type;
        private int fluid;
        private final int maxFluid;

        public PseudoFluidTank(PseudoFluidType type, int maxFluid) {
            this.type = type;
            this.maxFluid = maxFluid;
        }

        public PseudoFluidType getTankType() { return type; }
        public int getFill() { return fluid; }
        public int getMaxFill() { return maxFluid; }
        public void setFill(int fill) { fluid = Math.max(0, Math.min(fill, maxFluid)); }
        public void setTankType(PseudoFluidType type) {
            PseudoFluidType next = type == null ? PseudoFluidType.NONE : type;
            if (this.type != next) {
                this.type = next;
                this.fluid = 0;
            }
        }
        public void writeToNBT(DataOut output, String name) {
            output.putInt(name, fluid);
            output.putInt(name + "_max", maxFluid);
            output.putString(name + "_type", type.getName());
        }
        public void readFromNBT(DataIn input, String name) {
            setTankType(PseudoFluidType.fromName(input.getStringOr(name + "_type", "NONE")));
            setFill(input.getIntOr(name, 0));
        }
    }
}
