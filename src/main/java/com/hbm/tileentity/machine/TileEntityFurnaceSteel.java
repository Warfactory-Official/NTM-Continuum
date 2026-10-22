package com.hbm.tileentity.machine;

import com.hbm.lib.ForgeDirection;
import dev.golem.core.data.Tags;

import com.hbm.api.tile.IHeatSource;
import com.hbm.blocks.RawOres;
import com.hbm.inventory.container.ContainerFurnaceSteel;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.util.TickPhase;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.ItemStacks;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The heat-fed, three-lane steel furnace. */
public class TileEntityFurnaceSteel extends TileEntityMachineBase implements MenuProvider, WorldlyContainer {

    public static final int processTime = 40_000;
    public static final int maxHeat = 100_000;
    public static final double diffusion = 0.05D;

    public final int[] progress = new int[3];
    public final int[] bonus = new int[3];
    public int heat;
    public boolean wasOn;
    private final ItemStack[] lastItems = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};

    public TileEntityFurnaceSteel(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_STEEL.get(), pos, state, 6);
    }

    @Override public String getDefaultName() { return "container.furnaceSteel"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceSteel furnace) {
        furnace.tryPullHeat();
        furnace.wasOn = false;

        int burn = (furnace.heat - maxHeat / 3) / 10;
        for (int i = 0; i < 3; i++) {
            ItemStack input = furnace.inventory.getStackInSlot(i);
            if (input.isEmpty() || lastChanged(input, furnace.lastItems[i])) {
                furnace.progress[i] = 0;
                furnace.bonus[i] = 0;
            }

            if (furnace.canSmelt(i, level)) {
                furnace.progress[i] += burn;
                furnace.heat -= burn;
                furnace.wasOn = true;
                // Per smelting slot, as in 1.7: three busy slots are three times the soot.
                if (TickPhase.every(level, pos, 20L)) PollutionHandler.incrementPollution(level, pos, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 2);
            }

            furnace.lastItems[i] = input.copy();
            if (furnace.progress[i] >= processTime) {
                ItemStack result = Recipes.smeltingResult(level, input).orElse(ItemStack.EMPTY);
                if (result.isEmpty()) continue;
                furnace.addBonus(input, i);
                furnace.insertResult(i + 3, result);
                while (furnace.bonus[i] >= 100) {
                    furnace.insertResult(i + 3, result);
                    furnace.bonus[i] -= 100;
                }
                input.shrink(1);
                furnace.progress[i] = 0;
                furnace.setChanged();
            }
        }
        furnace.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceSteel furnace) {
        if (!furnace.wasOn) return;
        ForgeDirection dir = MachineCore.facing(state);
        ForgeDirection rotation = dir.getRotation(ForgeDirection.UP);
        level.addParticle(ParticleTypes.SMOKE,
                pos.getX() + 0.5 - dir.offsetX * 1.125 - rotation.offsetX * 0.75,
                pos.getY() + 2.625,
                pos.getZ() + 0.5 - dir.offsetZ * 1.125 - rotation.offsetZ * 0.75,
                0, 0.05, 0);
        if (level.getRandom().nextInt(20) == 0) {
            level.addParticle(ParticleTypes.CLOUD,
                    pos.getX() + 0.5 + dir.offsetX * 0.75,
                    pos.getY() + 2,
                    pos.getZ() + 0.5 + dir.offsetZ * 0.75,
                    0, 0.05, 0);
        }
        if (level.getRandom().nextInt(15) == 0) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.5 + dir.offsetX * 1.5 + rotation.offsetX * (level.getRandom().nextDouble() - 0.5),
                    pos.getY() + 0.75,
                    pos.getZ() + 0.5 + dir.offsetZ * 1.5 + rotation.offsetZ * (level.getRandom().nextDouble() - 0.5),
                    dir.offsetX * 0.5D, 0.05, dir.offsetZ * 0.5D);
        }
    }

    private static boolean lastChanged(ItemStack input, ItemStack last) {
        return last.isEmpty() || !ItemStacks.sameItemAndData(input, last);
    }

    private void insertResult(int slot, ItemStack result) {
        ItemStack output = inventory.getStackInSlot(slot);
        if (output.isEmpty()) inventory.setStackInSlot(slot, result.copy());
        else if (output.getCount() < output.getMaxStackSize()) output.grow(result.getCount());
    }

    protected void addBonus(ItemStack stack, int index) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("ore") || path.contains("ore_") || path.endsWith("_ore")) bonus[index] += 25;
        // port: ntm-next's raw ores get the ore bonus at their share of an ore's yield
        else if (path.startsWith("raw_") && !path.endsWith("_block")) {
            if (level.getRandom().nextFloat() < RawOres.SHARE) bonus[index] += 25;
        } else if (stack.is(ItemTags.LOGS) || path.contains("log")) bonus[index] += 50;
        else if (path.contains("tar")) bonus[index] += 50;
    }

    protected void tryPullHeat() {
        if (heat >= maxHeat || level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int diff = source.getHeatStored() - heat;
            if (diff == 0) return;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                heat = Math.min(heat + diff, maxHeat);
                return;
            }
        }
        heat = Math.max(heat - Math.max(heat / 1000, 1), 0);
    }

    public boolean canSmelt(int index, Level level) {
        if (heat < maxHeat / 3) return false;
        ItemStack input = inventory.getStackInSlot(index);
        if (input.isEmpty()) return false;
        ItemStack result = Recipes.smeltingResult(level, input).orElse(ItemStack.EMPTY);
        if (result.isEmpty()) return false;
        ItemStack output = inventory.getStackInSlot(index + 3);
        if (output.isEmpty()) return true;
        if (!ItemStacks.sameItemAndData(output, result)) return false;
        return output.getCount() < inventory.getSlotLimit(index + 3) && output.getCount() < output.getMaxStackSize()
                || output.getCount() < result.getMaxStackSize();
    }

    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 3 && level != null && !Recipes.smeltingResult(level, stack).orElse(ItemStack.EMPTY).isEmpty();
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1, 2, 3, 4, 5}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot < 3 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 3; }

    @Override public Component getDisplayName() { return Component.translatable("container.furnaceSteel"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerFurnaceSteel(id, inventory, this);
    }

    @Override public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        for (int value : progress) buf.writeInt(value);
        for (int value : bonus) buf.writeInt(value);
        buf.writeInt(heat);
        buf.writeBoolean(wasOn);
    }

    @Override public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        for (int i = 0; i < 3; i++) progress[i] = buf.readInt();
        for (int i = 0; i < 3; i++) bonus[i] = buf.readInt();
        heat = buf.readInt();
        wasOn = buf.readBoolean();
    }

    @Override protected void saveData(DataOut output) {
        super.saveData(output);
        output.putIntArray("progress", progress);
        output.putIntArray("bonus", bonus);
        output.putInt("heat", heat);
        var list = output.childrenList("lastItems");
        for (int i = 0; i < lastItems.length; i++) {
            if (lastItems[i].isEmpty()) continue;
            DataOut child = list.addChild();
            child.putInt("slot", i);
            child.store("stack", ItemStack.CODEC, lastItems[i]);
        }
    }

    @Override protected void loadData(DataIn input) {
        super.loadData(input);
        int[] savedProgress = input.getIntArrayOr("progress", new int[3]);
        int[] savedBonus = input.getIntArrayOr("bonus", new int[3]);
        System.arraycopy(savedProgress, 0, progress, 0, Math.min(3, savedProgress.length));
        System.arraycopy(savedBonus, 0, bonus, 0, Math.min(3, savedBonus.length));
        heat = input.getIntOr("heat", 0);
        for (DataIn child : input.childrenListOrEmpty("lastItems")) {
            int slot = child.getIntOr("slot", -1);
            if (slot >= 0 && slot < lastItems.length) child.read("stack", ItemStack.CODEC).ifPresent(stack -> lastItems[slot] = stack);
        }
    }
}
