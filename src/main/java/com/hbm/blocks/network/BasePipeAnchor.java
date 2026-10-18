package com.hbm.blocks.network;

import com.hbm.lib.ForgeDirection;
import dev.golem.core.world.BlockTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * 1.7 {@code BasePipeAnchor}: what the three anchors share. An interface because the fluid anchors are fluid ducts.
 * {@link #FACING} is the face clicked when placing; the anchor conducts through its opposite.
 */
public interface BasePipeAnchor extends BlockTooltip {

    EnumProperty<Direction> FACING = BlockStateProperties.FACING;

    static ForgeDirection openSide(BlockState state) {
        if (!state.hasProperty(FACING)) return ForgeDirection.UNKNOWN;
        return ForgeDirection.getOrientation(state.getValue(FACING)).getOpposite();
    }

    static int faceMask(BlockState state) {
        ForgeDirection side = openSide(state);
        return side == ForgeDirection.UNKNOWN ? 0 : 1 << side.ordinal();
    }

    /** A 8x8x8 core stretched to the mounted face. */
    static VoxelShape shape(BlockState state) {
        float min = 0.25F, max = 0.75F;
        ForgeDirection dir = openSide(state);
        float minX = dir == ForgeDirection.WEST ? 0F : min;
        float maxX = dir == ForgeDirection.EAST ? 1F : max;
        float minY = dir == ForgeDirection.DOWN ? 0F : min;
        float maxY = dir == ForgeDirection.UP ? 1F : max;
        float minZ = dir == ForgeDirection.NORTH ? 0F : min;
        float maxZ = dir == ForgeDirection.SOUTH ? 1F : max;
        return Shapes.box(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    default void appendBlockTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Connection Type: ").withStyle(ChatFormatting.GOLD).append(Component.literal("Single").withStyle(ChatFormatting.YELLOW)));
        tooltip.add(Component.literal("Connection Range: ").withStyle(ChatFormatting.GOLD).append(Component.literal("10m").withStyle(ChatFormatting.YELLOW)));
    }
}
