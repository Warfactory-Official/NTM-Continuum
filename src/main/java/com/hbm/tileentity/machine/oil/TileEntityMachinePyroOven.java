package com.hbm.tileentity.machine.oil;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.sound.PyroOvenSoundHandler;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerPyroOven;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.PyroOvenRecipes;
import com.hbm.inventory.recipes.PyroOvenRecipes.PyroOvenRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.TileEntityMachinePolluting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
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
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.HashMap;
import java.util.List;

/**
 * Pyro Oven, 1:1 port of the 1.12.2 {@code TileEntityMachinePyroOven}.
 * 6 slots (0 battery, 1 input item, 2 output item, 3 fluid identifier, 4-5 upgrades). Two 24k tanks
 * (input pipe-fed + type-settable, output sent). maxPower 10M. Progress increment
 * {@code 1/max((dur − speed·dur/4)/(overdrive·2+1),1)}; consumption {@code (10_000·(s+1)²)/(ps+1)}, checked
 * with {@code (speed, powerSaving)} but drained with {@code (speed + overdrive·2, powerSaving)}.
 * SPEED/POWER/OVERDRIVE ×3.
 *
 * <p>Smoke: every processing tick adds 1.7's {@code SOOT_PER_SECOND} to the 50 mB smoke tank (per tick, not
 * per second, 1.7 does not gate it), which leaves upward through the vent cell above the flank. What
 * does not fit goes straight into the region and sets {@link #isVenting}, which draws the dark plume.
 */
public class TileEntityMachinePyroOven extends TileEntityMachinePolluting
        implements MenuProvider, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IUpgradeInfoProvider, WorldlyContainer {

    public long power;
    public static final long maxPower = 10_000_000;
    public boolean isProgressing;
    public boolean isVenting;
    public float progress;
    public static final int consumption = 10_000;

    public int prevAnim;
    public int anim = 0;

    public FluidTankNTM[] tanks;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    protected PyroOvenRecipe lastValidRecipe;

    public TileEntityMachinePyroOven(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_PYROOVEN.get(), pos, state, 6, 50);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.NONE, 24_000);
        tanks[1] = new FluidTankNTM(Fluids.NONE, 24_000);
    }

    @Override public String getDefaultName() { return "container.machinePyroOven"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePyroOven te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        te.setTypeFromFluidId();

        for (DirPos dp : te.getConPos()) {
            te.trySubscribe(level, dp);
            if (te.tanks[0].getTankType() != Fluids.NONE) te.trySubscribe(te.tanks[0].getTankType(), level, dp);
            if (te.tanks[1].getFill() > 0) te.tryProvide(te.tanks[1], level, dp);
        }

        if (te.smoke.getFill() > 0) te.tryProvide(te.smoke, level, te.getSmokePos());

        te.upgradeManager.checkSlots(te.inventory, 4, 5);
        int speed = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerSaving = te.upgradeManager.getLevel(UpgradeType.POWER);
        int overdrive = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        te.isProgressing = false;
        te.isVenting = false;

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

            te.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
        } else {
            te.progress = 0F;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachinePyroOven te) {
        te.prevAnim = te.anim;
        if (te.isProgressing) {
            te.anim++;
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5) < 2_500D) {
                ForgeDirection dir = ForgeDirection.getOrientation(MachineCore.facingOrdinal(te.getBlockState(), 2));
                ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
                if (level.getRandom().nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 - rot.offsetX - dir.offsetX * 0.875, pos.getY() + 3,
                        pos.getZ() + 0.5 - rot.offsetZ - dir.offsetZ * 0.875, 0.0, 0.05, 0.0);
                if (level.getRandom().nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 - rot.offsetX - dir.offsetX * 2.375, pos.getY() + 3,
                        pos.getZ() + 0.5 - rot.offsetZ - dir.offsetZ * 2.375, 0.0, 0.05, 0.0);
                if (level.getRandom().nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 - rot.offsetX + dir.offsetX * 0.875, pos.getY() + 3,
                        pos.getZ() + 0.5 - rot.offsetZ + dir.offsetZ * 0.875, 0.0, 0.05, 0.0);
                if (level.getRandom().nextInt(20) == 0) level.addParticle(ParticleTypes.CLOUD,
                        pos.getX() + 0.5 - rot.offsetX + dir.offsetX * 2.375, pos.getY() + 3,
                        pos.getZ() + 0.5 - rot.offsetZ + dir.offsetZ * 2.375, 0.0, 0.05, 0.0);
            }
        }

        if (te.isVenting && level.getGameTime() % 2 == 0) {
            ForgeDirection dir = ForgeDirection.getOrientation(MachineCore.facingOrdinal(te.getBlockState(), 2));
            ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
            CompoundTag fx = new CompoundTag();
            fx.putFloat("lift", 10F);
            fx.putFloat("base", 0.25F);
            fx.putFloat("max", 2.5F);
            fx.putInt("life", 100 + level.getRandom().nextInt(20));
            fx.putInt("color", 0x202020);
            HbmEffectNT.Tower.summonParticle(level, pos.getX() + 0.5 - rot.offsetX, pos.getY() + 3, pos.getZ() + 0.5 - rot.offsetZ, fx);
        }
        PyroOvenSoundHandler.tickLoop(te);
    }

    /** 1.7's override: an overflow vents into the region without the hiss and lights the plume instead. */
    @Override
    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank = type == PollutionType.SOOT ? smoke : type == PollutionType.HEAVYMETAL ? smoke_leaded : smoke_poison;

        int fluidAmount = (int) Math.ceil(amount * 100);
        tank.setFill(tank.getFill() + fluidAmount);

        if (tank.getFill() > tank.getMaxFill()) {
            int overflow = tank.getFill() - tank.getMaxFill();
            tank.setFill(tank.getMaxFill());
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);
            this.isVenting = true;
        }
    }

    @Override
    public int countMufflers() {
        if (level == null) return 0;
        int count = 0;
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(worldPosition.relative(direction)).is(ModBlocks.muffler.get())) count++;
        }
        return count;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && level.isClientSide()) PyroOvenSoundHandler.onRemoved(this);
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
        return recipe.outputItem == null || inventory.getStackInSlot(2).isEmpty()
                || recipe.outputItem.getDamageValue() == inventory.getStackInSlot(2).getDamageValue();
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
        int meta = MachineCore.facingOrdinal(getBlockState(), 2);
        ForgeDirection dir = ForgeDirection.getOrientation(meta);
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

    /**
     * 1.7's smoke target, {@code sendFluid(smoke, x - rot.x, y + 3, z - rot.z, POS_Y)}: the vent on top of the
     * cell at {@code (-rot, 2)}. Declared as a port getter so the folded face table is checked against it.
     */
    public DirPos getSmokePos() {
        ForgeDirection rot = ForgeDirection.getOrientation(MachineCore.facingOrdinal(getBlockState(), 2))
                .getRotation(ForgeDirection.DOWN);
        return new DirPos(worldPosition.getX() - rot.offsetX, worldPosition.getY() + 3,
                worldPosition.getZ() - rot.offsetZ, ForgeDirection.UP);
    }

    /** {@link #getSmokePos()} as a port list, for the folded-port check. */
    public DirPos[] getSmokePorts() {
        return new DirPos[]{getSmokePos()};
    }

    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(3);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
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
        if (slot == 3) return stack.getItem() instanceof IItemFluidId;
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

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1], smoke}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tanks[0], tanks[1], smoke}; }

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
        buf.writeBoolean(isVenting);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        power = buf.readLong();
        isVenting = buf.readBoolean();
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
        output.putFloat("prog", progress);
        output.putLong("power", power);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
        progress = input.getFloatOr("prog", 0F);
        power = input.getLongOr("power", 0L);
    }
}
