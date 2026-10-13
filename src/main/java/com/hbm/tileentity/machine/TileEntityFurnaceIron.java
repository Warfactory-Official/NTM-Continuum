package com.hbm.tileentity.machine;

import com.hbm.api.environment.Environment;
import com.hbm.blocks.machine.ModMachineBlocks;
import com.hbm.inventory.UpgradeManagerNT;
import com.hbm.items.machine.ItemMachineUpgrade;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IUpgradeInfoProvider;
import dev.golem.core.data.Tags;

import com.hbm.inventory.container.ContainerFurnaceIron;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.util.TickPhase;
import com.hbm.module.ModuleBurnTime;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import com.hbm.lib.Library;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import java.util.HashMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Iron furnace: vanilla smelting with two fuel slots and speed upgrades. 1.7.10 timing:
 * {@code baseTime 160}, {@code processingTime = 160 - (160 / 2) * speed / 3} with SPEED capped at 3 by the
 * upgrade manager (so 160 / 134 / 107 / 80 ticks). The 1.12.2 {@code 200 - 15 * speed} is not the spec.
 */
public class TileEntityFurnaceIron extends TileEntityMachineBase implements MenuProvider, WorldlyContainer,
        IUpgradeInfoProvider {

    public static final int baseTime = 160;
    public final UpgradeManagerNT upgradeManager = new UpgradeManagerNT(this);
    public int maxBurnTime;
    public int burnTime;
    public boolean wasOn;
    public int progress;
    public int processingTime;

    public final ModuleBurnTime burnModule = new ModuleBurnTime()
            .setLigniteTimeMod(1.25)
            .setCoalTimeMod(1.25)
            .setCokeTimeMod(1.5)
            .setSolidTimeMod(2)
            .setRocketTimeMod(2)
            .setBalefireTimeMod(2);

    public TileEntityFurnaceIron(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_IRON.get(), pos, state, 5);
    }

    @Override public String getDefaultName() { return "container.furnaceIron"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceIron furnace) {
        furnace.upgradeManager.checkSlots(furnace.inventory, 4, 4);
        furnace.processingTime = baseTime - ((baseTime / 2) * furnace.upgradeManager.getLevel(ItemMachineUpgrade.UpgradeType.SPEED) / 3);
        furnace.wasOn = false;

        if (furnace.burnTime <= 0) {
            for (int i = 1; i < 3; i++) {
                ItemStack fuel = furnace.inventory.getStackInSlot(i);
                int burn = furnace.burnModule.getBurnTime(level, fuel);
                if (burn <= 0) continue;

                furnace.maxBurnTime = furnace.burnTime = burn;
                ItemStack remainder = Compat.craftingRemainder(fuel);
                fuel.shrink(1);
                if (fuel.isEmpty()) furnace.inventory.setStackInSlot(i, remainder);
                break;
            }
        }

        // hbm-space: smelting also needs air, 1 mB of oxygen every 5 ticks (com.hbm.api.environment.Environment)
        if (furnace.canSmelt(level) && Environment.breatheAir(level, pos,
                TickPhase.every(level, pos, 5L) ? 1 : 0)) {
            furnace.wasOn = true;
            furnace.progress++;
            furnace.burnTime--;

            if (furnace.progress % 15 == 0 && !furnace.muffled) {
                level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS,
                        1.0F, 0.5F + level.getRandom().nextFloat() * 0.5F);
            }

            if (furnace.progress >= furnace.processingTime) {
                furnace.smeltItem(level);
                furnace.progress = 0;
                furnace.setChanged();
            }
            if (TickPhase.every(level, pos, 20L)) PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND);
        } else {
            furnace.progress = 0;
        }

        furnace.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceIron furnace) {
        if (furnace.progress <= 0) return;
        ForgeDirection dir = MachineCore.facing(state);
        ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
        double offset = furnace.progress % 2 == 0 ? 1D : 0.5D;
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.5 - dir.offsetX * offset - rotation.offsetX * 0.1875,
                pos.getY() + 2,
                pos.getZ() + 0.5 - dir.offsetZ * offset - rotation.offsetZ * 0.1875,
                0, 0.01, 0);
        if (furnace.progress % 5 == 0) {
            level.addParticle(ParticleTypes.FLAME,
                    pos.getX() + 0.5 + dir.offsetX * 0.25 + rotation.offsetX * level.getRandom().nextDouble(),
                    pos.getY() + 0.25 + level.getRandom().nextDouble() * 0.25,
                    pos.getZ() + 0.5 + dir.offsetZ * 0.25 + rotation.offsetZ * level.getRandom().nextDouble(),
                    0, 0, 0);
        }
    }

    @Override
    public boolean canProvideInfo(ItemMachineUpgrade.UpgradeType type, int level, boolean extendedInfo) {
        return type == ItemMachineUpgrade.UpgradeType.SPEED;
    }

    @Override
    public void provideInfo(ItemMachineUpgrade.UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(IUpgradeInfoProvider.getStandardLabel(ModMachineBlocks.furnace_iron.get()));
        if (type == ItemMachineUpgrade.UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 50 / 3) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public HashMap<ItemMachineUpgrade.UpgradeType, Integer> getValidUpgrades() {
        HashMap<ItemMachineUpgrade.UpgradeType, Integer> upgrades = new HashMap<>();
        upgrades.put(ItemMachineUpgrade.UpgradeType.SPEED, 3);
        return upgrades;
    }

    public boolean canSmelt(Level level) {
        if (burnTime <= 0 || inventory.getStackInSlot(0).isEmpty()) return false;
        ItemStack result = Recipes.smeltingResult(level, inventory.getStackInSlot(0)).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return false;

        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) return true;
        if (!ItemStacks.sameItemAndData(output, result)) return false;
        return output.getCount() < inventory.getSlotLimit(3) && output.getCount() < output.getMaxStackSize()
                || output.getCount() < result.getMaxStackSize();
    }

    private void smeltItem(Level level) {
        if (!canSmelt(level)) return;
        ItemStack result = Recipes.smeltingResult(level, inventory.getStackInSlot(0)).orElse(ItemStack.EMPTY);
        ItemStack output = inventory.getStackInSlot(3);
        if (output.isEmpty()) inventory.setStackInSlot(3, result.copy());
        else output.grow(result.getCount());
        inventory.getStackInSlot(0).shrink(1);
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return level != null && !Recipes.smeltingResult(level, stack).orElse(ItemStack.EMPTY).isEmpty();
        if (slot == 4) return Library.isMachineUpgrade(stack);
        return slot < 3 && level != null && burnModule.getBurnTime(level, stack) > 0;
    }

    @Override protected int[] getUpgradeSlotRange() { return new int[]{4, 4}; }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1, 2, 3}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot != 3 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 3; }

    @Override public Component getDisplayName() { return Component.translatable("container.furnaceIron"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerFurnaceIron(id, inventory, this);
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(maxBurnTime);
        buf.writeInt(burnTime);
        buf.writeInt(progress);
        buf.writeInt(processingTime);
        buf.writeBoolean(wasOn);
    }

    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        maxBurnTime = buf.readInt();
        burnTime = buf.readInt();
        progress = buf.readInt();
        processingTime = buf.readInt();
        wasOn = buf.readBoolean();
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putInt("maxBurnTime", maxBurnTime);
        output.putInt("burnTime", burnTime);
        output.putInt("progress", progress);
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        maxBurnTime = input.getIntOr("maxBurnTime", 0);
        burnTime = input.getIntOr("burnTime", 0);
        progress = input.getIntOr("progress", 0);
    }
}
