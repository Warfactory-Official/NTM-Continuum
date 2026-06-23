package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.blocks.machine.MachineElectricFurnace;
import com.hbm.inventory.container.ContainerMachineElectricFurnace;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class TileEntityMachineElectricFurnace extends TileEntityMachineBase implements MenuProvider, IEnergyReceiverMK2, WorldlyContainer {

    private static final int[] IO_SLOTS = {0, 1, 2};

    public static final long maxPower = 100000;

    public long power;
    public int progress;
    public int maxProgress = 100;
    public int consumption = 50;

    public boolean canProgress;
    private int cooldown = 0;

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> quickCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    public TileEntityMachineElectricFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_ELECTRIC_FURNACE.get(), pos, state, 4);
    }

    @Override
    public String getDefaultName() {
        return "container.electricFurnace";
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return switch (i) {
            case 0 -> Library.isBattery(stack);
            case 1 -> true;
            default -> false;
        };
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineElectricFurnace te) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (te.cooldown > 0) te.cooldown--;

        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, maxPower);
        if (level.getGameTime() % 40 == 0) for (DirPos dp : te.getConPos()) te.trySubscribe(level, dp);

        te.consumption = 50;
        te.maxProgress = 100;

        boolean hasPower = te.power >= te.consumption;
        if (!hasPower) te.cooldown = 20;

        boolean canProcess = te.canProcess(serverLevel);
        te.canProgress = canProcess && hasPower;

        if (canProcess && hasPower) {
            te.progress++;
            te.power -= te.consumption;
            if (te.progress >= te.maxProgress) {
                te.progress = 0;
                te.processItem(serverLevel);
                te.setChanged();
            }
        } else {
            te.progress = 0;
        }

        boolean trigger = !hasPower || !canProcess || te.progress != 0;
        if (trigger && state.hasProperty(MachineElectricFurnace.LIT)) {
            boolean lit = te.progress > 0;
            if (state.getValue(MachineElectricFurnace.LIT) != lit) {
                level.setBlock(pos, state.setValue(MachineElectricFurnace.LIT, lit), 2);
            }
        }

        te.networkPackNT();
    }

    private @org.jspecify.annotations.Nullable SmeltingRecipe getRecipe(ServerLevel level) {
        ItemStack input = inventory.getStackInSlot(1);
        if (input.isEmpty()) return null;
        return quickCheck.getRecipeFor(new SingleRecipeInput(input), level)
                .map(RecipeHolder::value).orElse(null);
    }

    private boolean canProcess(ServerLevel level) {
        if (cooldown > 0) return false;
        SmeltingRecipe recipe = getRecipe(level);
        if (recipe == null) return false;
        ItemStack result = recipe.assemble(new SingleRecipeInput(inventory.getStackInSlot(1)));
        if (result.isEmpty()) return false;

        ItemStack outSlot = inventory.getStackInSlot(2);
        if (outSlot.isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(outSlot, result)) return false;
        return outSlot.getCount() + result.getCount() <= outSlot.getMaxStackSize();
    }

    private void processItem(ServerLevel level) {
        SmeltingRecipe recipe = getRecipe(level);
        if (recipe == null) return;
        ItemStack result = recipe.assemble(new SingleRecipeInput(inventory.getStackInSlot(1)));
        if (result.isEmpty()) return;

        ItemStack outSlot = inventory.getStackInSlot(2);
        if (outSlot.isEmpty()) inventory.setStackInSlot(2, result.copy());
        else outSlot.grow(result.getCount());
        inventory.getStackInSlot(1).shrink(1);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return (slot == 0 || slot == 1) && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 2 || (slot == 0 && Library.isEmptyBattery(stack));
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

    @Override public long getPower() { return power; }
    @Override public void setPower(long power) { this.power = power; }
    @Override public long getMaxPower() { return maxPower; }
    @Override public long getReceiverSpeed() { return maxPower / 20; }

    public long getPowerScaled(long i) { return (power * i) / maxPower; }
    public int getProgressScaled(int i) { return (progress * i) / maxProgress; }
    public boolean hasPower() { return power >= consumption; }
    public boolean isProcessing() { return progress > 0; }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm.machine_electric_furnace_off");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineElectricFurnace(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(maxProgress);
        buf.writeInt(progress);
        buf.writeBoolean(canProgress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        maxProgress = buf.readInt();
        progress = buf.readInt();
        canProgress = buf.readBoolean();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        power = input.getLongOr("power", 0L);
        progress = input.getIntOr("progress", 0);
    }
}
