package com.hbm.tileentity.machine.rbmk;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidProviderMK2;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.neutron.RBMKNeutronHandler.RBMKType;
import com.hbm.inventory.container.MenuRBMKOutgasser;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes.OutgasserRecipe;
import com.hbm.tileentity.ModRBMKBlockEntities;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Flux-fed irradiation channel. Progress is flux, rather than elapsed ticks. */
public class BlockEntityRBMKOutgasser extends BlockEntityRBMKBase implements IRBMKFluxReceiver, IFluidProviderMK2, MenuProvider {
    public static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1;
    public final FluidTankNTM gas = new FluidTankNTM(Fluids.TRITIUM, 64_000);
    public double progress, lastUsedFlux;
    public int duration = OutgasserRecipes.DURATION;

    public BlockEntityRBMKOutgasser(BlockPos pos, BlockState state) { super(ModRBMKBlockEntities.RBMK_OUTGASSER.get(), pos, state, 2); }
    @Override public void tickServer() { if (!canProcess()) { progress = 0D; lastUsedFlux = 0D; } super.tickServer(); }
    @Override public void receiveFlux(NeutronStream stream) {
        if (!canProcess()) return;
        lastUsedFlux = stream.fluxQuantity * Math.min(1D - stream.fluxRatio * .8D, 1D) * RBMKDials.getOutgasserMod(level);
        progress += lastUsedFlux;
        if (progress > duration) { process(); markChanged(); }
    }
    public boolean canProcess() {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (!(OutgasserRecipes.INSTANCE.getRecipe(input) instanceof OutgasserRecipe recipe)) return false;
        FluidStack fluid = recipe.fluidOutput();
        if (fluid != null) {
            if (gas.getFill() > 0 && gas.getTankType() != fluid.type) return false;
            gas.setTankType(fluid.type);
            if (gas.getFill() + fluid.fill > gas.getMaxFill()) return false;
        }
        ItemStack output = recipe.solidOutput(), slot = inventory.getStackInSlot(SLOT_OUTPUT);
        return output.isEmpty() || slot.isEmpty() || dev.golem.core.world.ItemStacks.sameItemAndData(slot, output) && slot.getCount() + output.getCount() <= slot.getMaxStackSize();
    }
    private void process() {
        if (!(OutgasserRecipes.INSTANCE.getRecipe(inventory.getStackInSlot(SLOT_INPUT)) instanceof OutgasserRecipe recipe)) return;
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT); input.shrink(1);
        if (input.isEmpty()) inventory.setStackInSlot(SLOT_INPUT, ItemStack.EMPTY);
        progress = 0D;
        FluidStack fluid = recipe.fluidOutput(); if (fluid != null) gas.setFill(gas.getFill() + fluid.fill);
        ItemStack output = recipe.solidOutput();
        if (!output.isEmpty()) {
            ItemStack slot = inventory.getStackInSlot(SLOT_OUTPUT);
            if (slot.isEmpty()) inventory.setStackInSlot(SLOT_OUTPUT, output.copy()); else slot.grow(output.getCount());
        }
    }
    @Override public void onMelt(int reduce) { for (int i = 0, count = 4 + level.getRandom().nextInt(2); i < count; i++) spawnDebris(EntityRBMKDebris.DebrisType.BLANK); super.onMelt(reduce); }
    @Override public RBMKType getRBMKType() { return RBMKType.OUTGASSER; }
    @Override public RBMKColumnType getConsoleType() { return RBMKColumnType.OUTGASSER; }
    @Override public RBMKColumn getConsoleData() { RBMKColumn.OutgasserColumn data = (RBMKColumn.OutgasserColumn) super.getConsoleData(); data.gas = gas.getFill(); data.maxGas = gas.getMaxFill(); data.progress = progress; data.maxProgress = duration; data.usedFlux = lastUsedFlux; return data; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{gas}; }
    @Override public boolean isItemValidForSlot(int slot, ItemStack stack) { return slot == SLOT_INPUT && OutgasserRecipes.INSTANCE.getRecipe(stack) != null; }
    public int[] getSlotsForFace(Direction side) { return new int[]{SLOT_INPUT, SLOT_OUTPUT}; }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == SLOT_OUTPUT; }
    @Override public long getFluidAvailable(FluidType type, int pressure) { return type == gas.getTankType() && pressure == gas.getPressure() ? gas.getFill() : 0L; }
    @Override public void useUpFluid(FluidType type, int pressure, long amount) { if (type == gas.getTankType() && pressure == gas.getPressure()) { gas.setFill(gas.getFill() - (int) Math.min(amount, gas.getFill())); markChanged(); } }
    @Override protected void saveData(DataOut output) { super.saveData(output); output.putDouble("progress", progress); gas.writeToNBT(output, "gas"); }
    @Override protected void loadData(DataIn input) { super.loadData(input); progress = input.getDoubleOr("progress", progress); gas.readFromNBT(input, "gas"); }
    @Override public void serialize(RegistryFriendlyByteBuf buffer) { super.serialize(buffer); gas.serialize(buffer); buffer.writeDouble(progress); }
    @Override public void deserialize(RegistryFriendlyByteBuf buffer) { super.deserialize(buffer); gas.deserialize(buffer); progress = buffer.readDouble(); }
    @Override public Component getDisplayName() { return Component.translatable("container.rbmkOutgasser"); }
    @Override public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new MenuRBMKOutgasser(id, inventory, this); }
}
