package com.hbm.tileentity.machine.storage;

import com.hbm.blocks.generic.BlockStorageCrate;
import com.hbm.interfaces.ILaserable;
import com.hbm.inventory.container.ContainerCrateTungsten;
import com.hbm.items.ModAbilityToolItems;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemCrucible;
import com.hbm.tileentity.ModDecoCrateBlockEntities;
import dev.golem.core.world.Recipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 1.7 {@code TileEntityCrateTungsten}: 27 slots and a laser target. Every laser hit ({@link #addEnergy}) keeps
 * the crate hot for five ticks and smelts every stack in place when the result still fits one stack; above
 * 10 MHE a polonium billet becomes yharonite and a spent crucible comes back fully charged (a fresh crucible,
 * as 1.7 made a new stack). While hot the block shows its hot state (1.7 metadata 1) and throws flame and
 * smoke off every face.
 *
 * <p>Automation cannot pull anything the furnace could still smelt, a polonium billet, or a spent crucible,
 * so a hopper under a working crate only takes finished goods.
 */
public class TileEntityCrateTungsten extends TileEntityCrateBase implements ILaserable {

    private int heatTimer;

    public TileEntityCrateTungsten(BlockPos pos, BlockState state) {
        super(ModDecoCrateBlockEntities.CRATE_TUNGSTEN.get(), pos, state, 27);
    }

    @Override
    public String getDefaultName() {
        return "container.crateTungsten";
    }

    public boolean isHot() {
        return getBlockState().hasProperty(BlockStorageCrate.HOT) && getBlockState().getValue(BlockStorageCrate.HOT);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TileEntityCrateTungsten crate) {
        if (crate.heatTimer > 0) crate.heatTimer--;
        boolean hot = state.getValue(BlockStorageCrate.HOT);
        if (!hot && crate.heatTimer > 0) level.setBlock(pos, state.setValue(BlockStorageCrate.HOT, true), 3);
        if (hot && crate.heatTimer <= 0) level.setBlock(pos, state.setValue(BlockStorageCrate.HOT, false), 3);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TileEntityCrateTungsten crate) {
        if (!state.getValue(BlockStorageCrate.HOT)) return;
        RandomSource rand = level.getRandom();
        int x = pos.getX(), y = pos.getY(), z = pos.getZ();
        flameAndSmoke(level, x + rand.nextDouble(), y + 1.1, z + rand.nextDouble());
        flameAndSmoke(level, x - 0.1, y + rand.nextDouble(), z + rand.nextDouble());
        flameAndSmoke(level, x + 1.1, y + rand.nextDouble(), z + rand.nextDouble());
        flameAndSmoke(level, x + rand.nextDouble(), y + rand.nextDouble(), z - 0.1);
        flameAndSmoke(level, x + rand.nextDouble(), y + rand.nextDouble(), z + 1.1);
    }

    private static void flameAndSmoke(Level level, double x, double y, double z) {
        level.addParticle(ParticleTypes.FLAME, x, y, z, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (this.isLocked()) return false;
        if (stack.getItem() == ModItems.billet_polonium.get()) return false;
        return level == null || Recipes.smeltingResult(level, stack).isEmpty();
    }

    @Override
    public void addEnergy(Level level, BlockPos pos, long energy, Direction dir) {
        heatTimer = 5;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty()) continue;

            Optional<ItemStack> smelted = Recipes.smeltingResult(level, slot);
            ItemStack result = smelted.map(ItemStack::copy).orElse(ItemStack.EMPTY);

            if (slot.getItem() == ModItems.billet_polonium.get() && energy > 10000000)
                result = new ItemStack(ModItems.billet_yharonite.get());

            // old-hbm TileEntityCrateTungsten: a crucible with fewer than three charges is recharged in place over 10 MHE
            if (slot.getItem() == ModAbilityToolItems.crucible.get() && ItemCrucible.getCharges(slot) < 3 && energy > 10000000) {
                ItemCrucible.charge(slot);
                inventory.setStackInSlot(i, slot); // old-hbm: changed = true
            }

            int size = slot.getCount();

            if (!result.isEmpty() && result.getCount() * size <= result.getMaxStackSize()) {
                ItemStack out = result.copy();
                out.setCount(result.getCount() * size);
                inventory.setStackInSlot(i, out);
            }
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ContainerCrateTungsten(containerId, inventory, this);
    }
}
