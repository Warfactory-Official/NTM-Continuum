package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.BlockDummyable;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerPyroOven;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.HashMap;
import java.util.List;

/**
 * Pyro Oven — 1:1 port of the 1.12.2 {@code TileEntityMachinePyroOven} (ported without pollution — §20).
 * 6 slots (0 battery, 1 input item, 2 output item, 3 fluid identifier, 4-5 upgrades). Two 24k tanks
 * (input pipe-fed + type-settable, output sent). maxPower 10M. Progress increment
 * {@code 1/max((dur − speed·dur/4)/(overdrive·2+1),1)}; consumption {@code (10_000·(s+1)²)/(ps+1)} —
 * checked with {@code (speed, powerSaving)} but drained with {@code (speed + overdrive·2, powerSaving)}.
 * SPEED/POWER/OVERDRIVE ×3.
 *
 * <p>Deferred (§20): pollution/smoke tank + venting, the pyroOperate audio loop.
 */
public class TileEntityMachinePyroOven extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IUpgradeInfoProvider, WorldlyContainer {

    public long power;
    public static final long maxPower = 10_000_000;
    public boolean isProgressing;
    public float progress;
    public static final int consumption = 10_000;

    public int prevAnim;
    public int anim = 0;

    public FluidTankNTM[] tanks;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    protected PyroOvenRecipe lastValidRecipe;

    public TileEntityMachinePyroOven(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PYROOVEN.get(), pos, state, 6);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.NONE, 24_000);
        tanks[1] = new FluidTankNTM(Fluids.NONE, 24_000);
    }

    @Override public String getDefaultName() { return "container.machinePyroOven"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePyroOven te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.setTypeFromIdentifier();

        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(level, dp);
            if (te.tanks[0].getTankType() != Fluids.NONE) te.trySubscribe(te.tanks[0].getTankType(), level, dp);
            if (te.tanks[1].getFill() > 0) te.tryProvide(te.tanks[1], level, dp);
        }

        te.upgradeManager.checkSlots(te.inventory, 4, 5);
        int speed = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = te.upgradeManager.getLevel(UpgradeType.POWER);
        int overdrive = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        te.isProgressing = false;

        if (te.canProcess()) {
            PyroOvenRecipe recipe = te.getMatchingRecipe();
            te.progress += 1F / Math.max((recipe.duration - speed * (recipe.duration / 4)) / (overdrive * 2 + 1), 1);
            te.isProgressing = true;
            te.power -= getConsumption(speed + overdrive * 2, powerSaving);

            if (te.progress >= 1F) {
                te.progress = 0F;
                te.finishRecipe(recipe);
                te.setChanged();
            }
        } else {
            te.progress = 0F;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePyroOven te) {
        te.prevAnim = te.anim;
        if (te.isProgressing) {
            te.anim++;
            // Vanilla CLOUD stand-in for the old chimney vent particles (§20).
            if (level.getRandom().nextInt(20) == 0) {
                level.addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                        pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
            }
        }
    }

    public static int getConsumption(int speed, int powerSaving) {
        return (int) (consumption * Math.pow(speed + 1, 2)) / (powerSaving + 1);
    }

    public PyroOvenRecipe getMatchingRecipe() {
        if (lastValidRecipe != null && doesRecipeMatch(lastValidRecipe)) return lastValidRecipe;
        for (PyroOvenRecipe rec : PyroOvenRecipes.getAll()) {
            if (doesRecipeMatch(rec)) {
                lastValidRecipe = rec;
                return rec;
            }
        }
        return null;
    }

    public boolean doesRecipeMatch(PyroOvenRecipe recipe) {
        if (recipe.inputFluid != null && tanks[0].getTankType() != recipe.inputFluid.type) return false;
        if (recipe.inputItem != null) {
            if (inventory.getStackInSlot(1).isEmpty()) return false;
            return recipe.inputItem.matchesRecipe(inventory.getStackInSlot(1), true);
        } else {
            return inventory.getStackInSlot(1).isEmpty();
        }
    }

    public boolean canProcess() {
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = upgradeManager.getLevel(UpgradeType.POWER);
        if (power < getConsumption(speed, powerSaving)) return false;

        PyroOvenRecipe recipe = getMatchingRecipe();
        if (recipe == null) return false;
        if (recipe.inputFluid != null && tanks[0].getFill() < recipe.inputFluid.fill) return false;
        if (recipe.inputItem != null && inventory.getStackInSlot(1).getCount() < recipe.inputItem.stacksize) return false;
        if (recipe.outputFluid != null && recipe.outputFluid.fill + tanks[1].getFill() > tanks[1].getMaxFill() && recipe.outputFluid.type == tanks[1].getTankType()) return false;
        if (recipe.outputItem != null && !inventory.getStackInSlot(2).isEmpty() && recipe.outputItem.getCount() + inventory.getStackInSlot(2).getCount() > inventory.getStackInSlot(2).getMaxStackSize()) return false;
        if (recipe.outputItem != null && !inventory.getStackInSlot(2).isEmpty() && recipe.outputItem.getItem() != inventory.getStackInSlot(2).getItem()) return false;
        return true;
    }

    public void finishRecipe(PyroOvenRecipe recipe) {
        if (recipe.outputItem != null) {
            if (inventory.getStackInSlot(2).isEmpty()) inventory.setStackInSlot(2, recipe.outputItem.copy());
            else inventory.getStackInSlot(2).grow(recipe.outputItem.getCount());
        }
        if (recipe.outputFluid != null) {
            tanks[1].setTankType(recipe.outputFluid.type);
            tanks[1].setFill(tanks[1].getFill() + recipe.outputFluid.fill);
        }
        if (recipe.inputItem != null) inventory.getStackInSlot(1).shrink(recipe.inputItem.stacksize);
        if (recipe.inputFluid != null) tanks[0].setFill(tanks[0].getFill() - recipe.inputFluid.fill);
    }

    public DirPos[] getConPos() {
        int meta = getBlockState().hasProperty(BlockDummyable.META) ? getBlockState().getValue(BlockDummyable.META) : BlockDummyable.offset + 2;
        ForgeDirection dir = ForgeDirection.getOrientation(meta - BlockDummyable.offset);
        ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.offsetX * 2 + rot.offsetX * 3, y, z + dir.offsetZ * 2 + rot.offsetZ * 3, rot),
                new DirPos(x + dir.offsetX + rot.offsetX * 3, y, z + dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x + rot.offsetX * 3, y, z + rot.offsetZ * 3, rot),
                new DirPos(x - dir.offsetX + rot.offsetX * 3, y, z - dir.offsetZ + rot.offsetZ * 3, rot),
                new DirPos(x - dir.offsetX * 2 + rot.offsetX * 3, y, z - dir.offsetZ * 2 + rot.offsetZ * 3, rot)
        };
    }

    private void setTypeFromIdentifier() {
        ItemStack id = inventory.getStackInSlot(3);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidIdentifier ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tanks[0].getTankType()) {
            tanks[0].setTankType(t);
            setChanged();
        }
    }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (int) (progress * i); }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        if (slot == 3) return stack.getItem() instanceof IItemFluidIdentifier;
        if (slot == 4 || slot == 5) return Library.isMachineUpgrade(stack);
        return true;
    }

    @Override
    protected int[] getUpgradeSlotRange() { return new int[]{4, 5}; }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{1, 2}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 2; }

    @Override public long getPower() { return power; }
    @Override public void setPower(long i) { power = i; }
    @Override public long getMaxPower() { return maxPower; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_pyrooven.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (int) (Math.pow(level + 1, 2) * 100 - 100) + "%").withStyle(ChatFormatting.RED));
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
        upgrades.put(UpgradeType.OVERDRIVE, 3);
        return upgrades;
    }

    @Override
    public Component getDisplayName() { return Component.translatable("container.machinePyroOven"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerPyroOven(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        power = buf.readLong();
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
        output.putFloat("prog", progress);
        output.putLong("power", power);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
        progress = input.getFloatOr("prog", 0F);
        power = input.getLongOr("power", 0L);
    }
}
