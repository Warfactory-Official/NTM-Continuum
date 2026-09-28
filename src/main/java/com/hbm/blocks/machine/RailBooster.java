package com.hbm.blocks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1.7 {@code RailBooster}, the high-speed booster rail: a high-speed rail (cap 1.0) that multiplies a passing
 * cart's motion by 1.15 on every tick the cart spends on it.
 */
public class RailBooster extends RailGeneric {

    public RailBooster(Properties properties) {
        super(properties);
        this.setMaxSpeed(1.0F);
        this.setFlexible(false);
    }

    @Override
    public void onMinecartPass(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        cart.setDeltaMovement(cart.getDeltaMovement().multiply(1.15F, 1.15F, 1.15F));
    }
}
