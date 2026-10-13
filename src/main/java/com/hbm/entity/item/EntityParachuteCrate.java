package com.hbm.entity.item;

import dev.golem.core.data.SynchedData;
import dev.golem.core.data.CompatEntity;
import com.hbm.interfaces.StoredItems;
import com.hbm.blocks.machine.storage.ModStorageBlocks;
import com.hbm.tileentity.machine.storage.TileEntityCrateSupply;
import dev.golem.core.world.ItemStacks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import dev.golem.core.world.EntityInterpolation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import dev.golem.core.data.DataIn;
import dev.golem.core.data.DataOut;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.7 entity/item/EntityParachuteCrate: the C130's payload - drifts down at capped speed and converts into a filled supply crate on the first non-air block.
 */
public class EntityParachuteCrate extends CompatEntity implements StoredItems {

    private final EntityInterpolation interpolation = new EntityInterpolation(this);
    public List<ItemStack> items = new ArrayList<>();

    @Override
    public void visitStoredItems(Visitor visitor) {
        boolean changed = false;
        for (ItemStack stack : items) changed |= visitor.visit(stack);
        if (changed) items.removeIf(ItemStack::isEmpty);
    }

    public EntityParachuteCrate(EntityType<? extends EntityParachuteCrate> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineData(SynchedData builder) {
    }

    @Override
    protected EntityInterpolation interpolation() {
        return this.interpolation;
    }

    @Override
    protected boolean onHurt(DamageSource source, float amount) {
        return false; // 1.7: attackEntityFrom inherited - no damage
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true; // 1.7: isInRangeToRenderDist always true
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        super.tick();

        if (level().isClientSide()) return;

        var motion = getDeltaMovement();
        setPos(getX() + motion.x, getY() + motion.y, getZ() + motion.z);

        if (motion.y > -0.2) setDeltaMovement(motion.x, motion.y - 0.02, motion.z);
        if (getY() > 600) setPos(getX(), 600, getZ());

        BlockPos pos = blockPosition();
        if (!level().getBlockState(pos).isAir()) {
            discard();

            BlockPos above = pos.above();
            level().setBlockAndUpdate(above, ModStorageBlocks.crate_supply.get().defaultBlockState());
            if (level().getBlockEntity(above) instanceof TileEntityCrateSupply crate) {
                int slot = 0;
                for (ItemStack stack : this.items) {
                    // EMPTY stack = EMPTY-marker roll (1.7 item unported)
                    if (stack.isEmpty() || slot >= crate.inventory.getSlots()) continue;
                    crate.inventory.setStackInSlot(slot++, stack);
                }
                crate.setChanged();
            }
        }
    }

    @Override
    protected void loadData(DataIn input) {
        this.items = new ArrayList<>(input.read("items", ItemStacks.optionalCodec().listOf()).orElse(List.of()));
    }

    @Override
    protected void saveData(DataOut output) {
        output.store("items", ItemStacks.optionalCodec().listOf(), List.copyOf(this.items));
    }
}
