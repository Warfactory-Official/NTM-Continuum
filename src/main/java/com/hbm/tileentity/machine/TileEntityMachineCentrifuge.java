package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.client.sound.CentrifugeSoundHandler;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.inventory.container.ContainerCentrifuge;
import com.hbm.inventory.recipes.CentrifugeRecipes;
import com.hbm.items.ModItems;
import com.hbm.items.ModFuelItems;
import com.hbm.items.machine.ItemMachineUpgrade.UpgradeType;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
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

/** Standard centrifuge, ported from 1.12.2 {@code TileEntityMachineCentrifuge}. */
public class TileEntityMachineCentrifuge extends TileEntityMachineBase
        implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer, IUpgradeInfoProvider {

    public static final long MAX_POWER = 100_000;
    public static final int PROCESSING_SPEED = 200;
    public static final int BASE_CONSUMPTION = 200;

    private static final int[] IO_SLOTS = {0, 2, 3, 4, 5};

    public long power;
    public int progress;
    public boolean isProgressing;
    /** Client-only sound ramp, matching the old +2/-3 audio-duration update. */
    public int audioDuration;

    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);

    public TileEntityMachineCentrifuge(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CENTRIFUGE.get(), pos, state, 8);
        CentrifugeRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.centrifuge";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCentrifuge te) {
        te.power = Library.chargeTEFromItems(te.inventory, 1, te.power, MAX_POWER);

        int consumption = BASE_CONSUMPTION;
        int speed = 1;
        te.upgradeManager.checkSlots(te.inventory, 6, 7);

        int speedLevel = te.upgradeManager.getLevel(UpgradeType.SPEED);
        int overdriveLevel = te.upgradeManager.getLevel(UpgradeType.OVERDRIVE);
        int powerLevel = te.upgradeManager.getLevel(UpgradeType.POWER);
        speed += speedLevel;
        consumption += speedLevel * BASE_CONSUMPTION;
        speed *= 1 + overdriveLevel * 5;
        consumption += overdriveLevel * BASE_CONSUMPTION * 50;
        consumption /= 1 + powerLevel;

        if (te.hasPower() && te.isProcessing()) {
            te.power -= consumption;
            if (te.power < 0) te.power = 0;
        }

        te.isProgressing = te.hasPower() && te.canProcess();
        if (te.isProgressing) {
            te.progress += speed;
            if (te.progress >= PROCESSING_SPEED) {
                te.progress = 0;
                te.processItem();
            }
        } else {
            te.progress = 0;
        }

        te.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCentrifuge te) {
        te.audioDuration += te.isProgressing ? 2 : -3;
        te.audioDuration = Math.max(0, Math.min(60, te.audioDuration));
        CentrifugeSoundHandler.tickLoop(te);
    }

    public boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(0);
        if (input.isEmpty()) return false;
        ItemStack[] output = CentrifugeRecipes.getOutput(input);
        if (output == null) return false;

        for (int i = 0; i < Math.min(4, output.length); i++) {
            if (output[i] == null || inventory.insertItem(i + 2, output[i], true).isEmpty()) continue;
            return false;
        }
        return true;
    }

    private void processItem() {
        ItemStack[] output = CentrifugeRecipes.getOutput(inventory.getStackInSlot(0));
        if (output == null) return;

        for (int i = 0; i < Math.min(4, output.length); i++) {
            if (output[i] != null) inventory.insertItem(i + 2, output[i], false);
        }
        inventory.extractItem(0, 1, false);
        setChanged();
    }

    public boolean hasPower() {
        return power > 0;
    }

    public boolean isProcessing() {
        return progress > 0;
    }

    public long getPowerScaled(long scale) {
        return (power * scale) / MAX_POWER;
    }

    public int getProgressScaled(int scale) {
        return (progress * scale) / PROCESSING_SPEED;
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 1, y, z, Library.POS_X),
                new DirPos(x - 1, y, z, Library.NEG_X),
                new DirPos(x, y, z + 1, Library.POS_Z),
                new DirPos(x, y, z - 1, Library.NEG_Z),
                new DirPos(x, y + 1, z, Library.POS_Y),
                new DirPos(x, y - 1, z, Library.NEG_Y),
        };
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> CentrifugeRecipes.isAnyRecipeInput(stack);
            case 1 -> Library.isBattery(stack);
            case 2, 3, 4, 5 -> true; // machine insertion; the menu exposes these as take-only
            case 6, 7 -> Library.isMachineUpgrade(stack);
            default -> false;
        };
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
        return slot == 0 && CentrifugeRecipes.getOutput(stack) != null;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot >= 2 && slot <= 5;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_centrifuge");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerCentrifuge(containerId, playerInventory, this);
    }

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.machine_centrifuge.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")
                    .withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%")
                    .withStyle(ChatFormatting.RED));
        } else if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%")
                    .withStyle(ChatFormatting.GREEN));
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

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return MAX_POWER; }
    @Override public long getReceiverSpeed() { return MAX_POWER; }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(progress);
        buf.writeBoolean(isProgressing);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        progress = buf.readInt();
        isProgressing = buf.readBoolean();
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
    }
}
