package com.hbm.tileentity.machine;

import dev.golem.core.data.Tags;

import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blocks.ModOreBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.container.ContainerBlastFurnace;
import com.hbm.api.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.BlastFurnaceRecipesNT;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.modules.ModuleBurnTime;
import com.hbm.tileentity.MachineCore;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.TileEntityMachineBase;
import com.hbm.util.TickPhase;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * Coal/airblast furnace, ported from HBM's {@code TileEntityMachineBlastFurnace}. The old airblast
 * cooling and flue-gas overflow behavior are intentionally retained.
 */
public class TileEntityMachineBlastFurnace extends TileEntityMachineBase
        implements MenuProvider, IFluidStandardTransceiverMK2, WorldlyContainer {

    public static final int FUEL_COAL = 200 * 8;
    public static final int FUEL_RATE = 200 * 4;
    public static final int MAX_FUEL = FUEL_COAL * 16;
    public static final int FLUE_GAS = 100;

    public final FluidTankNTM[] tanks = {
            new FluidTankNTM(Fluids.AIRBLAST, 4_000),
            new FluidTankNTM(Fluids.FLUE, 1_000)
    };
    public boolean isProgressing;
    public float progress;
    public float speed;
    public int fuel;

    public final ModuleBurnTime burnModule = new ModuleBurnTime().setWoodHeatMod(0D);

    public TileEntityMachineBlastFurnace(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BLAST_FURNACE.get(), pos, state, 5);
        BlastFurnaceRecipesNT.INSTANCE.registerRecipes();
    }

    @Override
    public String getDefaultName() {
        return "container.blastFurnace";
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityMachineBlastFurnace furnace) {
        for (DirPos connection : furnace.getConPos()) {
            if (furnace.tanks[1].getFill() > 0) furnace.tryProvide(furnace.tanks[1], level, connection.getPos(), connection.getDir().toDirection());
        }

        ItemStack fuelStack = furnace.inventory.getStackInSlot(0);
        int capacity = MAX_FUEL - furnace.fuel;
        int burnValue = furnace.getBurnTime(fuelStack);
        if (burnValue > 0 && burnValue <= capacity) {
            furnace.fuel += burnValue;
            furnace.inventory.extractItem(0, 1, false);
        }

        furnace.speed = 0F;
        GenericRecipe recipe = BlastFurnaceRecipesNT.INSTANCE.getRecipe(
                furnace.inventory.getStackInSlot(1), furnace.inventory.getStackInSlot(2));

        if (recipe != null && furnace.fuel >= FUEL_RATE && furnace.hasQuantities(recipe)
                && furnace.canOutput(recipe)) {
            furnace.speed = MthClamp.clamp(0.5F + furnace.tanks[0].getFill() * 8F / furnace.tanks[0].getMaxFill(), 0.5F, 5F);
            furnace.isProgressing = true;
            furnace.progress += furnace.speed / recipe.duration;

            if (furnace.progress >= 1F) {
                furnace.process(recipe);
                furnace.progress = 0F;
                furnace.fuel -= FUEL_RATE;
                furnace.tanks[1].setFill(Math.min(furnace.tanks[1].getFill() + FLUE_GAS,
                        furnace.tanks[1].getMaxFill()));
            }

            if (level.getRandom().nextInt(10) == 0 && !furnace.muffled) {
                level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                        1.0F, 0.5F + level.getRandom().nextFloat() * 0.25F);
            }
        } else {
            furnace.isProgressing = false;
            furnace.progress = 0F;
        }

        if (furnace.tanks[0].getFill() > 0) furnace.tanks[0].setFill((int) (furnace.tanks[0].getFill() * 0.95F));
        furnace.syncState();
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityMachineBlastFurnace furnace) {
        if (!level.getBlockState(pos.above(7)).isAir() || !furnace.isProgressing || !TickPhase.every(level, pos, 2)) return;
        level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA,
                pos.getX() + 0.25D + level.getRandom().nextDouble() * 0.5D, pos.getY() + 7.25D,
                pos.getZ() + 0.25D + level.getRandom().nextDouble() * 0.5D, 0D, 0D, 0D);
    }

    public DirPos[] getConPos() {
        int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
        ForgeDirection facing = ForgeDirection.getOrientation(MachineCore.facingOrdinal(getBlockState(), 2));
        return new DirPos[]{
                new DirPos(x + 2, y, z, Library.POS_X),
                new DirPos(x - 2, y, z, Library.NEG_X),
                new DirPos(x, y, z + 2, Library.POS_Z),
                new DirPos(x + facing.offsetX * 2, y + 3, z + facing.offsetZ * 2, facing),
                new DirPos(x + facing.offsetX * 2, y + 5, z + facing.offsetZ * 2, facing),
                new DirPos(x, y + 7, z, Library.POS_Y)
        };
    }

    public boolean hasQuantities(GenericRecipe recipe) {
        if (recipe.inputItem == null || recipe.inputItem.length != 2) return false;
        return recipe.inputItem[0].matchesRecipe(inventory.getStackInSlot(1), false)
                && recipe.inputItem[1].matchesRecipe(inventory.getStackInSlot(2), false)
                || recipe.inputItem[1].matchesRecipe(inventory.getStackInSlot(1), false)
                && recipe.inputItem[0].matchesRecipe(inventory.getStackInSlot(2), false);
    }

    public boolean canOutput(GenericRecipe recipe) {
        if (recipe.outputItem == null || recipe.outputItem.length > 2) return false;
        for (int i = 0; i < recipe.outputItem.length; i++) {
            ItemStack out = recipe.outputItem[i];
            ItemStack slot = inventory.getStackInSlot(3 + i);
            int limit = Math.min(inventory.getSlotLimit(3 + i), out.getMaxStackSize());
            if (out.getCount() > limit) return false;
            if (!slot.isEmpty() && (!dev.golem.core.world.ItemStacks.sameItemAndData(slot, out)
                    || slot.getCount() + out.getCount() > limit)) return false;
        }
        return true;
    }

    public void process(GenericRecipe recipe) {
        // canOutput() and hasQuantities() are both called immediately before this method, so no input is
        // consumed until every output slot has already been proven able to accept its complete result.
        for (int i = 0; i < recipe.outputItem.length; i++) {
            int slot = 3 + i;
            ItemStack out = recipe.outputItem[i];
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing.isEmpty()) inventory.setStackInSlot(slot, out.copy());
            else {
                ItemStack grown = existing.copy();
                grown.grow(out.getCount());
                inventory.setStackInSlot(slot, grown);
            }
        }

        if (recipe.inputItem[0].matchesRecipe(inventory.getStackInSlot(1), false)
                && recipe.inputItem[1].matchesRecipe(inventory.getStackInSlot(2), false)) {
            consume(1, recipe.inputItem[0]);
            consume(2, recipe.inputItem[1]);
        } else {
            consume(1, recipe.inputItem[1]);
            consume(2, recipe.inputItem[0]);
        }
    }

    private void consume(int slot, AStack input) {
        ItemStack stack = inventory.getStackInSlot(slot).copy();
        stack.shrink(input.stacksize);
        inventory.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
    }

    public int getBurnTime(ItemStack stack) {
        if (!Compat.craftingRemainder(stack).isEmpty()) return 0;
        return burnModule.getBurnHeat(burnModule.getBurnTime(level, stack), stack);
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return getBurnTime(stack) > 0;
        return slot == 1 || slot == 2;
    }

    public boolean canInsertItem(int slot, ItemStack stack) {
        if (slot == 1 || slot == 2) {
            int other = slot == 1 ? 2 : 1;
            if (!inventory.getStackInSlot(other).isEmpty()
                    && dev.golem.core.world.ItemStacks.sameItemAndData(stack, inventory.getStackInSlot(other))) return false;
            for (GenericRecipe recipe : BlastFurnaceRecipesNT.INSTANCE.recipeOrderedList) {
                for (AStack input : recipe.inputItem) if (input.matchesRecipe(stack, true)) return true;
            }
            return false;
        }
        return isItemValidForSlot(slot, stack);
    }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{1, 2, 0, 3, 4}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canInsertItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 3; }

    @Override public FluidTankNTM[] getReceivingTanks() { return new FluidTankNTM[]{tanks[0]}; }
    @Override public FluidTankNTM[] getSendingTanks() { return new FluidTankNTM[]{tanks[1]}; }
    @Override public FluidTankNTM[] getAllTanks() { return tanks; }
    @Override public long getProviderSpeed(FluidType type, int pressure) {
        return Math.max(tanks[1].getFill() * 50L / tanks[1].getMaxFill(), 1L);
    }

    @Override public Component getDisplayName() { return Component.translatable("block.hbm.machine_blast_furnace"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerBlastFurnace(id, inventory, this);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isProgressing);
        buf.writeFloat(progress);
        buf.writeFloat(speed);
        buf.writeInt(fuel);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isProgressing = buf.readBoolean();
        progress = buf.readFloat();
        speed = buf.readFloat();
        fuel = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    protected void saveData(DataOut output) {
        super.saveData(output);
        output.putFloat("progress", progress);
        output.putInt("fuel", fuel);
        tanks[0].writeToNBT(output, "t0");
        tanks[1].writeToNBT(output, "t1");
    }

    @Override
    protected void loadData(DataIn input) {
        super.loadData(input);
        progress = input.getFloatOr("progress", 0F);
        fuel = input.getIntOr("fuel", 0);
        tanks[0].readFromNBT(input, "t0");
        tanks[1].readFromNBT(input, "t1");
    }

    /** Avoid a version-specific Mth spelling in the shared source. */
    private static final class MthClamp {
        private static float clamp(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
