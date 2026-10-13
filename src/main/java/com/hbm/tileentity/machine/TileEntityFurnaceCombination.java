package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.tileentity.MachineCore;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.container.ContainerFurnaceCombination;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.CombinationRecipes;
import com.hbm.items.machine.ItemFluidBucket;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.tileentity.TileEntityMachinePolluting;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Effects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Combination furnace, ported from 1.12.2 {@code TileEntityFurnaceCombination}.
 *
 * <p>The odd-looking separation between item slots 0/1 and container slots 2/3 is intentional: the
 * original furnace consumes item recipes while its single tank is a byproduct tank that can be emptied
 * into a held container or a fluid network. A recipe with no item output is retained in the recipe table,
 * but remains unprocessable because that is what the old {@code canSmelt()} did.
 */
public class TileEntityFurnaceCombination extends TileEntityMachinePolluting implements MenuProvider, IFluidStandardSenderMK2,
        WorldlyContainer {

    public static final int processTime = 20_000;
    public static final int maxHeat = 100_000;
    public static final double diffusion = 0.25D;

    public boolean wasOn;
    public int progress;
    public int heat;
    public final FluidTankNTM tank;

    public TileEntityFurnaceCombination(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FURNACE_COMBINATION.get(), pos, state, 4, 50);
        this.tank = new FluidTankNTM(Fluids.NONE, 24_000);
    }

    @Override
    public String getDefaultName() {
        return "container.furnaceCombination";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceCombination furnace) {
        furnace.unloadTank();
        furnace.tryPullHeat();

        if (TickPhase.every(level, pos, 20L)) {
            for (DirPos connection : furnace.getConPos()) {
                if (furnace.tank.getFill() > 0) furnace.tryProvide(furnace.tank, level, connection);
                furnace.sendSmoke(connection);
            }
        }

        furnace.wasOn = false;
        if (furnace.canSmelt()) {
            int burn = furnace.heat / 100;
            if (burn > 0) {
                furnace.wasOn = true;
                furnace.progress += burn;
                furnace.heat -= burn;

                if (furnace.progress >= processTime) {
                    furnace.progress -= processTime;
                    CombinationRecipes.CombinationRecipe recipe =
                            CombinationRecipes.getOutput(furnace.inventory.getStackInSlot(0));
                    ItemStack output = recipe.itemOutput();
                    furnace.inventory.insertItem(1, output.copy(), false);
                    if (recipe.fluidOutput() != null) {
                        furnace.tank.setTankType(recipe.fluidOutput().type);
                        furnace.tank.setFill(furnace.tank.getFill() + recipe.fluidOutput().fill);
                    }
                    furnace.inventory.extractItem(0, 1, false);
                    furnace.setChanged();
                }

                AABB flameBox = new AABB(pos.getX() - 0.5D, pos.getY() + 2D, pos.getZ() - 0.5D,
                        pos.getX() + 1.5D, pos.getY() + 4D, pos.getZ() + 1.5D);
                for (Entity entity : level.getEntitiesOfClass(Entity.class, flameBox)) {
                    Effects.ignite(entity, 5);
                }

                if (TickPhase.every(level, pos, 20L)) furnace.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
            } else {
                furnace.progress = 0;
            }
        } else {
            furnace.progress = 0;
        }

        furnace.networkPackNT();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityFurnaceCombination furnace) {
        if (furnace.wasOn && level.getRandom().nextInt(15) == 0) {
            level.addParticle(ParticleTypes.LAVA,
                    pos.getX() + 0.5D + level.getRandom().nextGaussian() * 0.5D, pos.getY() + 2D,
                    pos.getZ() + 0.5D + level.getRandom().nextGaussian() * 0.5D, 0D, 0D, 0D);
        }
    }

    public boolean canSmelt() {
        ItemStack input = inventory.getStackInSlot(0);
        CombinationRecipes.CombinationRecipe recipe = CombinationRecipes.getOutput(input);
        if (recipe == null || recipe.itemOutput() == null) return false;
        if (!inventory.insertItem(1, recipe.itemOutput().copy(), true).isEmpty()) return false;

        if (recipe.fluidOutput() != null) {
            FluidType type = recipe.fluidOutput().type;
            if (tank.getTankType() != type && tank.getFill() > 0) return false;
            return tank.getFill() + recipe.fluidOutput().fill <= tank.getMaxFill();
        }
        return true;
    }

    protected void tryPullHeat() {
        if (heat >= maxHeat) return;
        BlockPos below = worldPosition.below();
        BlockEntity connection = level.getBlockEntity(below);
        if (!(connection instanceof IHeatSource)) {
            connection = MachineCore.at(level, below, level.getBlockState(below));
        }

        if (connection instanceof IHeatSource source) {
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

    /** Fills slot 3 from the tank using the container in slot 2. */
    private void unloadTank() {
        FluidType fluid = tank.getTankType();
        if (fluid == Fluids.NONE || tank.getFill() <= 0) return;
        ItemStack input = inventory.getStackInSlot(2);
        if (input.isEmpty()) return;

        if (input.getItem() instanceof IFillableItem fillable) {
            if (!fillable.acceptsFluid(fluid, input)) return;
            ItemStack work = input.copy();
            work.setCount(1);
            int remainder = fillable.tryFill(fluid, tank.getFill(), work);
            int moved = tank.getFill() - remainder;
            if (moved <= 0 || !inventory.insertItem(3, work.copy(), true).isEmpty()) return;
            tank.setFill(tank.getFill() - moved);
            inventory.extractItem(2, 1, false);
            inventory.insertItem(3, work, false);
        } else if (input.is(Items.BUCKET) && tank.getFill() >= ItemFluidBucket.VOLUME) {
            ItemStack full = ItemFluidBucket.make(fluid);
            if (!inventory.insertItem(3, full.copy(), true).isEmpty()) return;
            tank.setFill(tank.getFill() - ItemFluidBucket.VOLUME);
            inventory.extractItem(2, 1, false);
            inventory.insertItem(3, full, false);
        }
    }

    public int getProgressScaled(int scale) { return progress * scale / processTime; }
    public int getHeatScaled(int scale) { return heat * scale / maxHeat; }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return CombinationRecipes.getOutput(stack) != null;
        if (slot == 2) return stack.getItem() instanceof IFillableItem || stack.is(Items.BUCKET);
        return false;
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 0 && isItemValidForSlot(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1; }

    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tank, smoke, smoke_leaded, smoke_poison}; }
    @Override public FluidTankNTM[] getAllTanks() { return new FluidTankNTM[]{tank}; }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        DirPos[] result = new DirPos[33];
        int index = 0;
        for (int i = 2; i < 6; i++) {
            ForgeDirection direction = ForgeDirection.getOrientation(i);
            ForgeDirection rotation = direction.getRotation(ForgeDirection.UP);
            for (int yy = y; yy <= y + 1; yy++) for (int j = -1; j <= 1; j++) {
                result[index++] = new DirPos(x + direction.offsetX * 2 + rotation.offsetX * j, yy,
                        z + direction.offsetZ * 2 + rotation.offsetZ * j, direction);
            }
        }
        for (int xx = x - 1; xx <= x + 1; xx++) for (int zz = z - 1; zz <= z + 1; zz++) {
            result[index++] = new DirPos(xx, y + 2, zz, ForgeDirection.UP);
        }
        return result;
    }

    @Override public Component getDisplayName() { return Component.translatable("container.furnaceCombination"); }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ContainerFurnaceCombination(containerId, playerInventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(wasOn);
        buf.writeInt(heat);
        buf.writeInt(progress);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        wasOn = buf.readBoolean();
        heat = buf.readInt();
        progress = buf.readInt();
        tank.deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        tank.writeToNBT(output, "tank");
        output.putInt("prog", progress);
        output.putInt("heat", heat);
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        tank.readFromNBT(input, "tank");
        progress = input.getIntOr("prog", 0);
        heat = input.getIntOr("heat", 0);
    }
}
