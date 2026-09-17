package com.hbm.tileentity.machine.albion;

import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.container.ContainerPADetector;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes;
import com.hbm.inventory.recipes.ParticleAcceleratorRecipes.ParticleAcceleratorRecipe;
import com.hbm.lib.DirPos;
import com.hbm.saveddata.satellites.SatelliteDetector;
import com.hbm.saveddata.satellites.SatelliteRayScan;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.MachineCore;
import com.hbm.lib.Library;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.albion.TileEntityPASource.PAState;
import com.hbm.tileentity.machine.albion.TileEntityPASource.Particle;
import dev.golem.core.world.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * End of the beamline: the detector smashes the particle and reads the result off
 * {@link ParticleAcceleratorRecipes}. It is the strictest component in the machine — the beam must
 * arrive perfectly focused, and it must carry at least the recipe's momentum, or the run ends in a
 * crash instead of a product.
 *
 * <p>Ported from 1.12.2's {@code albion/TileEntityPADetector}.
 *
 * <p>Deviation from CE: a successful run does not grant the digamma advancement — the advancement manager
 * is not ported.
 * Sided automation exposes the same slots to every face for the reason given on
 * {@link TileEntityPASource}.
 */
public class TileEntityPADetector extends TileEntityCooledBase
        implements MenuProvider, IParticleUser, WorldlyContainer, IRORValueProvider {

    public static final long usage = 100_000;

    private static final int[] IO_SLOTS = {1, 2, 3, 4};

    public TileEntityPADetector(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PA_DETECTOR.get(), pos, state, 5);
        ParticleAcceleratorRecipes.register();
    }

    @Override
    public String getDefaultName() {
        return "container.paDetector";
    }

    @Override
    public long getMaxPower() {
        return 1_000_000;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityPADetector te) {
        te.power = Library.chargeTEFromItems(te.inventory, 0, te.power, te.getMaxPower());
        te.exchangeCoolant(level);
        te.updateCooling();
        te.networkPackNT();
    }

    private ForgeDirection getBeamDir() {
        return MachineCore.facing(getBlockState())
                .getRotation(ForgeDirection.DOWN);
    }

    @Override
    public List<DirPos> getConPos() {
        ForgeDirection dir = MachineCore.facing(getBlockState());
        ForgeDirection rot = dir.getRotation(ForgeDirection.UP);
        BlockPos p = worldPosition;
        return List.of(
                new DirPos(p.offset(-rot.offsetX * 5, 0, -rot.offsetZ * 5), rot.getOpposite()),
                new DirPos(p.offset(-rot.offsetX * 5, 1, -rot.offsetZ * 5), rot.getOpposite()),
                new DirPos(p.offset(-rot.offsetX * 5, -1, -rot.offsetZ * 5), rot.getOpposite()),
                new DirPos(p.offset(-rot.offsetX * 5 + dir.offsetX, 0, -rot.offsetZ * 5 + dir.offsetZ), rot.getOpposite()),
                new DirPos(p.offset(-rot.offsetX * 5 - dir.offsetX, 0, -rot.offsetZ * 5 - dir.offsetZ), rot.getOpposite()));
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return Library.isBattery(stack);
        return slot == 1 || slot == 2;
    }

    @Override public int[] getSlotsForFace(Direction side) { return IO_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 1 || slot == 2; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot == 3 || slot == 4; }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return IO_SLOTS;
    }

    @Override
    public boolean canParticleEnter(Particle particle, ForgeDirection dir, BlockPos pos) {
        ForgeDirection detectorDir = getBeamDir();
        BlockPos input = worldPosition.offset(-detectorDir.offsetX * 4, -detectorDir.offsetY * 4, -detectorDir.offsetZ * 4);
        return input.equals(pos) && detectorDir == dir;
    }

    @Override
    public void onEnter(Particle particle, ForgeDirection dir) {
        particle.invalid = true;

        if (particle.defocus > 0) {
            particle.crash(PAState.CRASH_DEFOCUS);
            return;
        }
        if (this.power < usage) {
            particle.crash(PAState.CRASH_NOPOWER);
            return;
        }
        if (!isCool()) {
            particle.crash(PAState.CRASH_NOCOOL);
            return;
        }
        this.power -= usage;

        for (ParticleAcceleratorRecipe recipe : ParticleAcceleratorRecipes.recipes) {
            if (!recipe.matchesRecipe(particle.input1, particle.input2)) continue;

            if (particle.momentum < recipe.momentum) {
                particle.crash(PAState.CRASH_UNDERSPEED);
                return;
            }

            if (canAccept(recipe)) {
                if (!Compat.craftingRemainder(recipe.output1).isEmpty()) inventory.extractItem(1, 1, false);
                if (recipe.output2 != null && !Compat.craftingRemainder(recipe.output2).isEmpty()) {
                    inventory.extractItem(2, 1, false);
                }

                if (inventory.getStackInSlot(3).isEmpty()) {
                    inventory.setStackInSlot(3, recipe.output1.copy());
                } else {
                    inventory.insertItem(3, recipe.output1.copy(), false);
                }

                if (recipe.output2 != null) {
                    if (inventory.getStackInSlot(4).isEmpty()) {
                        inventory.setStackInSlot(4, recipe.output2.copy());
                    } else {
                        inventory.insertItem(4, recipe.output2.copy(), false);
                    }
                }
            }

            BlockPos pos = getBlockPos();
            SatelliteDetector.reportEvent(level, SatelliteDetector.DURATION_MEDIUM,
                    SatelliteDetector.BurstIntensity.MEDIUM, pos.getX(), pos.getZ());
            SatelliteRayScan.reportEvent(level, pos.getX(), pos.getY(), pos.getZ(),
                    SatelliteRayScan.RayEvent.INFO_PARTICLE, 600);
            particle.crash(PAState.SUCCESS);
            return;
        }

        particle.crash(PAState.CRASH_NORECIPE);
    }

    public boolean canAccept(ParticleAcceleratorRecipe recipe) {
        return checkSlot(recipe.output1, 1, 3) && checkSlot(recipe.output2, 2, 4);
    }

    /**
     * Whether {@code output} can land in {@code outputSlot}. An output that leaves a container item
     * behind (an empty particle) additionally needs that container already sitting in
     * {@code containerSlot}, since that is the stack the detector consumes to make room for it.
     */
    public boolean checkSlot(@Nullable ItemStack output, int containerSlot, int outputSlot) {
        if (output == null) return true;

        ItemStack inSlot = inventory.getStackInSlot(outputSlot);
        if (!inSlot.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(inSlot, output)) return false;
            if (inSlot.getCount() + output.getCount() > output.getMaxStackSize()) return false;
        }

        ItemStack container = Compat.craftingRemainder(output);
        if (container.isEmpty()) return true;

        ItemStack containerInSlot = inventory.getStackInSlot(containerSlot);
        return !containerInSlot.isEmpty() && ItemStack.isSameItemSameComponents(containerInSlot, container);
    }

    @Override
    public @Nullable BlockPos getExitPos(Particle particle) {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getDefaultName());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ContainerPADetector(id, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[]{
                PREFIX_VALUE + "temperature",
                PREFIX_VALUE + "pfmcold",
                PREFIX_VALUE + "pfm"};
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "temperature").equals(name)) return "" + (int) this.temperature;
        if ((PREFIX_VALUE + "pfmcold").equals(name)) return "" + coolantTanks[0].getFill();
        if ((PREFIX_VALUE + "pfm").equals(name)) return "" + coolantTanks[1].getFill();
        return null;
    }
}
