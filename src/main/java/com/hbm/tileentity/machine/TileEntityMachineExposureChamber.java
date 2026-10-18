package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerMachineExposureChamber;
import com.hbm.inventory.recipes.ExposureChamberRecipes;
import com.hbm.inventory.recipes.ExposureChamberRecipes.ExposureChamberRecipe;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import dev.golem.core.world.ItemStacks;

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

import java.util.HashMap;
import java.util.List;
import com.hbm.tileentity.TilePort.PortDef;

/** Eight-cycle particle exposure machine, ported from the 1.12.2 implementation. */
public class TileEntityMachineExposureChamber extends TileEntityMachineBase implements MenuProvider,
        IEnergyReceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 1_000_000L;
    public static final int PROCESS_TIME_BASE = 200;
    public static final int CONSUMPTION_BASE = 10_000;
    public static final int MAX_PARTICLES = 8;

    private static final int[] IO_SLOTS = {0, 2, 3, 4};

    public long power;
    public int progress;
    public int processTime = PROCESS_TIME_BASE;
    public int consumption = CONSUMPTION_BASE;
    public int savedParticles;
    public boolean isOn;
    public float rotation;
    public float prevRotation;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineExposureChamber(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_EXPOSURE_CHAMBER.get(), pos, state, 8);
        ExposureChamberRecipes.INSTANCE.registerRecipes();
    }

    @Override
    public String getDefaultName() {
        return "container.exposureChamber";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineExposureChamber te) {
        te.setupPowerPorts(te.getPorts());
        te.updatePortPIFIFO();

        te.isOn = false;
        te.power = Library.chargeTEFromItems(te.inventory, 5, te.power, MAX_POWER);

        te.upgradeManager.checkSlots(te.inventory, 6, 7);
        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);
        int overdriveLevel = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        te.consumption = CONSUMPTION_BASE;
        te.processTime = PROCESS_TIME_BASE - PROCESS_TIME_BASE / 4 * speedLevel;
        te.consumption *= speedLevel / 2 + 1;
        te.processTime *= powerLevel / 2 + 1;
        te.consumption /= powerLevel + 1;
        te.processTime /= overdriveLevel + 1;
        te.consumption *= overdriveLevel * 2 + 1;

        // Slot 1 is the internal eight-cycle buffer. The source catalyst stays in slot 0 until it is loaded.
        if (te.inventory.getStackInSlot(1).isEmpty() && !te.inventory.getStackInSlot(0).isEmpty()
                && !te.inventory.getStackInSlot(3).isEmpty() && te.savedParticles <= 0) {
            ExposureChamberRecipe recipe = getRecipe(te.inventory.getStackInSlot(0), te.inventory.getStackInSlot(3));
            if (recipe != null) {
                ItemStack remainder = Compat.craftingRemainder(te.inventory.getStackInSlot(0));
                boolean canStore = remainder.isEmpty();
                if (!remainder.isEmpty()) {
                    if (te.inventory.getStackInSlot(2).isEmpty()) {
                        te.inventory.setStackInSlot(2, remainder.copy());
                        canStore = true;
                    } else if (te.inventory.insertItem(2, remainder, true).isEmpty()) {
                        te.inventory.insertItem(2, remainder, false);
                        canStore = true;
                    }
                }

                if (canStore) {
                    ItemStack internal = te.inventory.getStackInSlot(0).copy();
                    internal.setCount(1);
                    te.inventory.setStackInSlot(1, internal);
                    te.inventory.extractItem(0, 1, false);
                    te.savedParticles = MAX_PARTICLES;
                }
            }
        }

        if (!te.inventory.getStackInSlot(1).isEmpty() && te.savedParticles > 0
                && te.power >= te.consumption) {
            ExposureChamberRecipe recipe = getRecipe(te.inventory.getStackInSlot(1), te.inventory.getStackInSlot(3));
            if (recipe != null && te.inventory.insertItem(4, recipe.output, true).isEmpty()) {
                te.progress++;
                te.power -= te.consumption;
                te.isOn = true;

                if (te.progress >= te.processTime) {
                    te.progress = 0;
                    te.savedParticles--;
                    te.inventory.extractItem(3, 1, false);
                    te.inventory.insertItem(4, recipe.output.copy(), false);
                }
            } else {
                te.progress = 0;
            }
        } else {
            te.progress = 0;
        }

        if (te.savedParticles <= 0) te.inventory.setStackInSlot(1, ItemStack.EMPTY);
        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineExposureChamber te) {
        te.prevRotation = te.rotation;
        if (!te.isOn) return;
        te.rotation += 10F;
        if (te.rotation >= 720F) {
            te.rotation -= 720F;
            te.prevRotation -= 720F;
        }
    }

    protected PortDef[] cachedPorts;

    public PortDef[] getPorts() {
        if (cachedPorts == null) {
            ForgeDirection dir = MachineCore.facing(getBlockState());
            ForgeDirection rot = dir.getRotation(ForgeDirection.DOWN);
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            cachedPorts = new PortDef[] {
                    PortDef.make(x + rot.offsetX * 7 + dir.offsetX, y, z + rot.offsetZ * 7 + dir.offsetZ, dir),
                    PortDef.make(x + rot.offsetX * 7 - dir.offsetX, y, z + rot.offsetZ * 7 - dir.offsetZ, dir.getOpposite()),
                    PortDef.make(x + rot.offsetX * 8 + dir.offsetX, y, z + rot.offsetZ * 8 + dir.offsetZ, dir),
                    PortDef.make(x + rot.offsetX * 8 - dir.offsetX, y, z + rot.offsetZ * 8 - dir.offsetZ, dir.getOpposite()),
                    PortDef.make(x + rot.offsetX * 8, y, z + rot.offsetZ * 8, rot),
            };
        }
        return cachedPorts;
    }

    public DirPos[] getConPos() { return PortDef.connections(getPorts()); }

    public static ExposureChamberRecipe getRecipe(ItemStack particle, ItemStack ingredient) {
        return ExposureChamberRecipes.getRecipe(particle, ingredient);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        // These slots are machine-owned; the menu/sided hooks keep players and automation from inserting.
        if (slot == 1 || slot == 2 || slot == 4) return true;

        if (slot == 3 && !inventory.getStackInSlot(3).isEmpty()) {
            return ItemStacks.sameItemAndData(inventory.getStackInSlot(3), stack);
        }

        ItemStack loadedParticle = inventory.getStackInSlot(1).isEmpty()
                ? inventory.getStackInSlot(0) : inventory.getStackInSlot(1);

        if (slot == 0 && loadedParticle.isEmpty() && !inventory.getStackInSlot(3).isEmpty()) {
            return getRecipe(stack, inventory.getStackInSlot(3)) != null;
        }
        if (slot == 3 && !loadedParticle.isEmpty() && inventory.getStackInSlot(3).isEmpty()) {
            return getRecipe(inventory.getStackInSlot(0), stack) != null;
        }
        if (loadedParticle.isEmpty() && inventory.getStackInSlot(3).isEmpty()) {
            for (ExposureChamberRecipe recipe : ExposureChamberRecipes.recipes) {
                if (slot == 0 && recipe.particle.matchesRecipe(stack, true)) return true;
                if (slot == 3 && recipe.ingredient.matchesRecipe(stack, true)) return true;
            }
        }
        if (slot == 5) return Library.isBattery(stack);
        return (slot == 6 || slot == 7) && Library.isMachineUpgrade(stack);
    }

    @Override
    protected int[] getUpgradeSlotRange() {
        return new int[]{6, 7};
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot == 0 || slot == 3) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 2 || slot == 4;
    }

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public long getReceiverSpeed() { return MAX_POWER; }

    public int getProgressScaled(int scale) {
        return progress * scale / (processTime + 1);
    }

    public int getParticlesScaled(int scale) {
        return savedParticles * scale / MAX_PARTICLES;
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_exposure_chamber.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + level * 25 + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + level * 50 + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION,
                    "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_DELAY, "+" + level * 50 + "%").withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle(IUpgradeInfoProvider.getBlink()
                    ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
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
    public Component getDisplayName() {
        return Component.translatable("container.exposureChamber");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineExposureChamber(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeInt(progress);
        buf.writeInt(processTime);
        buf.writeInt(consumption);
        buf.writeLong(power);
        buf.writeInt(savedParticles);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        progress = buf.readInt();
        processTime = buf.readInt();
        consumption = buf.readInt();
        power = buf.readLong();
        savedParticles = buf.readInt();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("progress", progress);
        output.putLong("power", power);
        output.putInt("savedParticles", savedParticles);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        progress = input.getIntOr("progress", 0);
        power = input.getLongOr("power", 0L);
        savedParticles = input.getIntOr("savedParticles", 0);
    }
}
