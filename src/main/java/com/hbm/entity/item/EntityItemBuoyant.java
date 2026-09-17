package com.hbm.entity.item;

import com.hbm.entity.ModNtmEntities;
import dev.golem.core.data.CompatItemEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * 1.7 EntityItemBuoyant: a dropped item that floats instead of settling - the only delta is an upward nudge over standing (non-falling) water.
 */
public class EntityItemBuoyant extends CompatItemEntity {

    public EntityItemBuoyant(EntityType<? extends EntityItemBuoyant> type, Level level) {
        super(type, level);
    }

    public EntityItemBuoyant(Level level, double x, double y, double z, ItemStack stack) {
        this(ModNtmEntities.ITEM_BUOYANT.get(), level);
        setPos(x, y, z);
        setItem(stack);
        // 26.2: ItemEntity's positional ctor hardcodes EntityTypes.ITEM, so a subclass cannot reach its scatter
        setDeltaMovement(random.nextDouble() * 0.2 - 0.1, 0.2, random.nextDouble() * 0.2 - 0.1);
    }

    @Override
    public void tick() {
        BlockPos below = new BlockPos(Mth.floor(getX()), Mth.floor(getY() - 0.0625), Mth.floor(getZ()));
        BlockState state = level().getBlockState(below);
        FluidState fluid = state.getFluidState();

        // 1.7: meta < 8 == not falling water
        if (state.getBlock() instanceof LiquidBlock && fluid.is(FluidTags.WATER) && !fluid.getValue(
                FlowingFluid.FALLING)) {
            setDeltaMovement(getDeltaMovement().add(0, 0.045D, 0));
        }

        super.tick();
    }
}
