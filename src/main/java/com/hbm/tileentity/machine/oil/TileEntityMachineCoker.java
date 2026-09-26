package com.hbm.tileentity.machine.oil;

import dev.golem.core.data.Tags;

import com.hbm.tileentity.MachineCore;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.container.ContainerMachineCoker;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CokerRecipes;
import com.hbm.items.machine.IItemFluidId;
import com.hbm.lib.DirPos;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.TickPhase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

/**
 * Coker — 1:1 port of the 1.12.2 {@code TileEntityMachineCoker}. A 3×3×23 heat-driven tower (pulls heat
 * from the {@link IHeatSource} directly below the core): {@code processTime 20_000}, {@code maxHeat
 * 100_000}, diffusion 0.25, {@code burn = heat/100}. 2 slots (0 fluid identifier input, 1 output
 * take-only). Tanks: {@code [0] HEAVYOIL 16k} input + {@code [1] OIL_COKER 8k} byproduct. Recipe outputs
 * one item + an optional byproduct fluid per {@link CokerRecipes}.
 *
 * <p>Deferred (§20): pollution/soot; the tall {@code HbmEffectNT.Tower} smoke → vanilla
 * {@code CAMPFIRE_COSY_SMOKE} stand-in at y+22; IFluidCopiable.
 */
public class TileEntityMachineCoker extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer {

    public boolean wasOn;
    public int progress;
    public static final int processTime = 20_000;
    public int heat;
    public static final int maxHeat = 100_000;
    public static final double diffusion = 0.25D;

    public FluidTankNTM[] tanks;

    public TileEntityMachineCoker(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_COKER.get(), pos, state, 2);
        tanks = new FluidTankNTM[2];
        tanks[0] = new FluidTankNTM(Fluids.HEAVYOIL, 16_000);
        tanks[1] = new FluidTankNTM(Fluids.OIL_COKER, 8_000);
    }

    @Override public String getDefaultName() { return "container.machineCoker"; }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCoker te) {
        te.tryPullHeat();
        te.setTypeFromFluidId();


        te.wasOn = false;

        if (te.canProcess()) {
            int burn = te.heat / 100;
            if (burn > 0) {
                te.wasOn = true;
                te.progress += burn;
                te.heat -= burn;

                if (te.progress >= processTime) {
                    te.setChanged();
                    te.progress -= processTime;

                    CokerRecipes.CokerRecipe recipe = CokerRecipes.getOutput(te.tanks[0].getTankType());
                    int fillReq = recipe.amount();
                    ItemStack output = recipe.output();
                    FluidStack byproduct = recipe.byproduct();

                    if (output != null && !output.isEmpty()) {
                        if (te.inventory.getStackInSlot(1).isEmpty()) {
                            te.inventory.setStackInSlot(1, output.copy());
                        } else {
                            te.inventory.getStackInSlot(1).grow(output.getCount());
                        }
                    }
                    if (byproduct != null) te.tanks[1].setFill(te.tanks[1].getFill() + byproduct.fill);
                    te.tanks[0].setFill(te.tanks[0].getFill() - fillReq);
                }
            }
        }

        for (DirPos dp : te.getConPos()) {
            if (te.tanks[1].getFill() > 0) te.tryProvide(te.tanks[1], level, dp.getPos(), dp.getDir().toDirection());
        }

        te.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineCoker te) {
        if (te.wasOn && TickPhase.every(level, pos, 2)) {
            level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 22, pos.getZ() + 0.5, 0.0, 0.05, 0.0);
        }
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        return new DirPos[]{
                new DirPos(x + 2, y, z + 1, Library.POS_X),
                new DirPos(x + 2, y, z - 1, Library.POS_X),
                new DirPos(x - 2, y, z + 1, Library.NEG_X),
                new DirPos(x - 2, y, z - 1, Library.NEG_X),
                new DirPos(x + 1, y, z + 2, Library.POS_Z),
                new DirPos(x - 1, y, z + 2, Library.POS_Z),
                new DirPos(x + 1, y, z - 2, Library.NEG_Z),
                new DirPos(x - 1, y, z - 2, Library.NEG_Z)
        };
    }

    public boolean canProcess() {
        CokerRecipes.CokerRecipe recipe = CokerRecipes.getOutput(tanks[0].getTankType());
        if (recipe == null) return false;

        int fillReq = recipe.amount();
        ItemStack output = recipe.output();
        FluidStack byproduct = recipe.byproduct();

        if (byproduct != null) tanks[1].setTankType(byproduct.type);
        if (tanks[0].getFill() < fillReq) return false;
        if (byproduct != null && byproduct.fill + tanks[1].getFill() > tanks[1].getMaxFill()) return false;

        if (output != null && !output.isEmpty() && !inventory.getStackInSlot(1).isEmpty()) {
            if (output.getItem() != inventory.getStackInSlot(1).getItem()) return false;
            return output.getCount() + inventory.getStackInSlot(1).getCount() <= output.getMaxStackSize();
        }
        return true;
    }

    protected void tryPullHeat() {
        if (this.heat >= maxHeat) return;
        BlockPos below = worldPosition.below();
        BlockEntity con = level.getBlockEntity(below);
        if (!(con instanceof IHeatSource)) {
            con = MachineCore.at(level, below, level.getBlockState(below));
        }

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;
            if (diff == 0) return;
            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > maxHeat) this.heat = maxHeat;
                return;
            }
        }
        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    private void setTypeFromFluidId() {
        ItemStack id = inventory.getStackInSlot(0);
        if (id.isEmpty() || !(id.getItem() instanceof IItemFluidId ident)) return;
        FluidType t = ident.getType(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), id);
        if (t != tanks[0].getTankType()) {
            tanks[0].setTankType(t);
            setChanged();
        }
    }

    public int getProgressScaled(int i) { return progress * i / processTime; }
    public int getHeatScaled(int i) { return heat * i / maxHeat; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return stack.getItem() instanceof IItemFluidId;
        return true; // slot 1 is machine-written output
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }

    @Override
    public Component getDisplayName() { return Component.translatable("container.machineCoker"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerMachineCoker(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(wasOn);
        buf.writeInt(heat);
        buf.writeInt(progress);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        wasOn = buf.readBoolean();
        heat = buf.readInt();
        progress = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
        output.putInt("prog", progress);
        output.putInt("heat", heat);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
        progress = input.getIntOr("prog", 0);
        heat = input.getIntOr("heat", 0);
    }
}
