package com.hbm.tileentity.machine.fusion;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.inventory.container.ContainerFusionBreeder;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.FluidBreederRecipes;
import com.hbm.inventory.recipes.OutgasserRecipes;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.capability.MachineCaps;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Converts irradiated solids or fluids into the breeder's output buffers. */
public class TileEntityFusionBreeder extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, IFusionPowerReceiver, WorldlyContainer {

    public static final int SLOTS = 3;
    public static final double CAPACITY = 10_000D;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.NONE, 16_000),
            new FluidTankNTM(Fluids.NONE, 16_000)
    };

    public double neutronEnergy;
    private double neutronEnergySync;
    public double progress;


    public TileEntityFusionBreeder(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_BREEDER.get(), pos, state, SLOTS);
    }

    @Override
    public String getDefaultName() {
        return "container.fusionBreeder";
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getName());
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFusionBreeder breeder) {
        breeder.updateInputType();

        if (!breeder.canProcessSolid() && !breeder.canProcessLiquid()) breeder.progress = 0;
        breeder.neutronEnergySync = breeder.neutronEnergy;

        for (DirPos port : breeder.getConPos()) {
            if (breeder.tanks[1].getFill() > 0) breeder.tryProvide(breeder.tanks[1], level, port);
        }

        ForgeDirection dir = FusionNodes.facing(state).getOpposite();
        FusionLink.claim(level, breeder, plasmaCell(pos, dir), MachineCaps.PLASMA_IN);

        breeder.networkPackNT();
        breeder.neutronEnergy = 0;
    }

    private void updateInputType() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (stack.isEmpty() || !(stack.getItem() instanceof IItemFluidId identifier)) return;

        FluidType type = identifier.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
        tanks[0].setTankType(type);
    }

    public boolean canProcessSolid() {
        // 1.7: the irradiated meteorite sword is fused before any outgasser recipe is looked up
        if (inventory.getStackInSlot(1).is(com.hbm.items.ModAbilityToolItems.meteorite_sword_irradiated.get())
                && inventory.getStackInSlot(2).isEmpty()) return true;
        GenericRecipe recipe = OutgasserRecipes.INSTANCE.getRecipeForFusion(inventory.getStackInSlot(1));
        if (recipe == null) return false;

        FluidStack fluid = fluidOutput(recipe);
        if (fluid != null) {
            if (tanks[1].getTankType() != fluid.type && tanks[1].getFill() > 0) return false;
            tanks[1].setTankType(fluid.type);
            if (tanks[1].getFill() + fluid.fill > tanks[1].getMaxFill()) return false;
        }

        ItemStack output = itemOutput(recipe);
        if (output.isEmpty()) return true;
        ItemStack existing = inventory.getStackInSlot(2);
        return existing.isEmpty() || (existing.getItem() == output.getItem()
                && existing.getCount() + output.getCount() <= existing.getMaxStackSize());
    }

    public boolean canProcessLiquid() {
        GenericRecipe recipe = FluidBreederRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
        if (recipe == null || recipe.inputFluid == null || recipe.outputFluid == null) return false;

        FluidStack input = recipe.inputFluid[0];
        FluidStack output = recipe.outputFluid[0];
        if (tanks[0].getFill() < input.fill) return false;
        if (tanks[1].getTankType() != output.type && tanks[1].getFill() > 0) return false;
        tanks[1].setTankType(output.type);
        return tanks[1].getFill() + output.fill <= tanks[1].getMaxFill();
    }

    private void processSolid() {
        if (inventory.getStackInSlot(1).is(com.hbm.items.ModAbilityToolItems.meteorite_sword_irradiated.get())) {
            inventory.extractItem(1, 1, false);
            inventory.setStackInSlot(2, new ItemStack(com.hbm.items.ModAbilityToolItems.meteorite_sword_fused.get()));
            this.progress = 0;
            return;
        }
        GenericRecipe recipe = OutgasserRecipes.INSTANCE.getRecipeForFusion(inventory.getStackInSlot(1));
        if (recipe == null) return;

        inventory.extractItem(1, 1, false);
        FluidStack fluid = fluidOutput(recipe);
        if (fluid != null) tanks[1].setFill(tanks[1].getFill() + fluid.fill);

        ItemStack output = itemOutput(recipe);
        if (!output.isEmpty()) {
            ItemStack existing = inventory.getStackInSlot(2);
            if (existing.isEmpty()) inventory.setStackInSlot(2, output);
            else {
                ItemStack combined = existing.copy();
                combined.grow(output.getCount());
                inventory.setStackInSlot(2, combined);
            }
        }
        progress = 0;
    }

    private void processLiquid() {
        GenericRecipe recipe = FluidBreederRecipes.INSTANCE.getRecipe(tanks[0].getTankType());
        if (recipe == null) return;
        tanks[0].setFill(tanks[0].getFill() - recipe.inputFluid[0].fill);
        tanks[1].setFill(tanks[1].getFill() + recipe.outputFluid[0].fill);
        progress = 0;
    }

    public void doProgress() {
        if (canProcessSolid() || canProcessLiquid()) {
            progress += neutronEnergy;
            if (progress > CAPACITY) {
                if (canProcessSolid()) processSolid();
                else processLiquid();
                setChanged();
            }
        } else {
            progress = 0;
        }
    }

    private static FluidStack fluidOutput(GenericRecipe recipe) {
        return recipe.outputFluid != null && recipe.outputFluid.length != 0 ? recipe.outputFluid[0] : null;
    }

    private static ItemStack itemOutput(GenericRecipe recipe) {
        return recipe.outputItem != null && recipe.outputItem.length != 0 ? recipe.outputItem[0].copy() : ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.getItem() instanceof IItemFluidId;
            case 1 -> OutgasserRecipes.INSTANCE.getRecipeForFusion(stack) != null;
            default -> false;
        };
    }

    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return new int[]{1, 2};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == 1 && isItemValidForSlot(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction side) {
        return slot == 2;
    }

    public List<DirPos> getConPos() {
        ForgeDirection dir = FusionNodes.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return List.of(
                new DirPos(x + dir.offsetX * 3, y + 2, z + dir.offsetZ * 3, dir),
                new DirPos(x + rot.offsetX * 2, y, z + rot.offsetZ * 2, rot),
                new DirPos(x - rot.offsetX * 2, y, z - rot.offsetZ * 2, rot.getOpposite()),
                new DirPos(x + dir.offsetX + rot.offsetX * 2, y, z + dir.offsetZ + rot.offsetZ * 2, rot),
                new DirPos(x + dir.offsetX - rot.offsetX * 2, y, z + dir.offsetZ - rot.offsetZ * 2, rot.getOpposite()));
    }

    @Override
    public boolean receivesFusionPower() {
        return false;
    }

    @Override
    public void receiveFusionPower(long fusionPower, double neutronPower, float r, float g, float b) {
        neutronEnergy = neutronPower;
        doProgress();
    }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFusionBreeder(containerId, playerInventory, this);
    }

    @Override
    public void setRemoved() {
        FusionLink.release(level, plasmaCell(worldPosition,
                FusionNodes.facing(getBlockState()).getOpposite()));
        super.setRemoved();
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeDouble(neutronEnergySync);
        buf.writeDouble(progress);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        neutronEnergy = buf.readDouble();
        progress = buf.readDouble();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut out) {
        super.saveData(out);
        out.putDouble("progress", progress);
        tanks[0].writeToNBT(out, "t0");
        tanks[1].writeToNBT(out, "t1");
    }

    @Override
    protected void loadData(DataIn in) {
        super.loadData(in);
        progress = in.getDoubleOr("progress", 0D);
        tanks[0].readFromNBT(in, "t0");
        tanks[1].readFromNBT(in, "t1");
    }

    @Override
    protected boolean syncFullTagOnChunkLoad() {
        return true;
    }

    /** Where this machine claims its end of the torus beam, 2 blocks out the back at +2. */
    private static BlockPos plasmaCell(BlockPos pos, ForgeDirection dir) {
        return new BlockPos(pos.getX() + dir.offsetX * 2, pos.getY() + 2,
                pos.getZ() + dir.offsetZ * 2);
    }
}
