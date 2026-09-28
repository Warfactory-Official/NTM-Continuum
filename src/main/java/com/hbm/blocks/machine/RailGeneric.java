package com.hbm.blocks.machine;

import dev.golem.core.world.CompatRailBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 1.7 {@code RailGeneric}: a plain cart rail with its own speed cap. The wooden tracks cap carts at 0.2 blocks per
 * tick (half the vanilla 0.4), the narrow-gauge rail keeps 0.4, the high-speed rail allows 1.0.
 *
 * <p>1.7 meant {@code setFlexible(false)} to keep the high-speed rail straight, but its {@code isFlexibleRail}
 * answered {@code !isPowered()}, which is always true for a non-powered rail - so every one of these rails turns
 * corners, and the high-speed rail's curves wear its straight texture (it registers no {@code _turned} icon).
 * That is kept: all of them are flexible rails, and the flag only changes the tooltip and the curve texture.
 * The speed cap and the booster's per-tick push reach the cart through Golem Core's {@link CompatRailBlock}.
 */
public class RailGeneric extends CompatRailBlock {

    protected static final float baseSpeed = 0.4F;
    protected float maxSpeed = 0.4F;
    protected boolean slopable = true;
    protected boolean flexible = true;

    public RailGeneric(Properties properties) {
        super(properties);
    }

    @Override
    public float getRailMaxSpeed(BlockState state, Level level, BlockPos pos, AbstractMinecart cart) {
        return maxSpeed;
    }

    public RailGeneric setMaxSpeed(float speed) {
        this.maxSpeed = speed;
        return this;
    }

    public RailGeneric setFlexible(boolean flexible) {
        this.flexible = flexible;
        return this;
    }

    public RailGeneric setSlopable(boolean slopable) {
        this.slopable = slopable;
        return this;
    }

    public float maxSpeed() {
        return maxSpeed;
    }

    /** 1.7 {@code addInformation} (ITooltipProvider). */
    public void addInformation(List<Component> list) {
        float speed = this.maxSpeed / baseSpeed;

        if (speed != 1F) {
            list.add(Component.literal("Speed: " + ((int) (speed * 100)) + "%")
                    .withStyle(speed > 1 ? ChatFormatting.BLUE : ChatFormatting.RED));
        }
        if (!flexible) list.add(Component.literal("Cannot be used for turns!").withStyle(ChatFormatting.RED));
        if (!slopable) list.add(Component.literal("Cannot be used for slopes!").withStyle(ChatFormatting.RED));
    }
}
