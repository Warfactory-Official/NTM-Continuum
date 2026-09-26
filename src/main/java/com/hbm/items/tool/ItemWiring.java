package com.hbm.items.tool;

import dev.golem.core.multiblock.MultiblockSurface;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import com.hbm.blocks.ModEnergyNetContent;
import com.hbm.tileentity.network.TileEntityPylonBase;

/**
 * {@code wiring_red_copper}: right-click one pylon, then another, and a wire runs between them (1.7's
 * {@code ItemWiring}). The first pylon's position is the old NBT {@code x/y/z} triple, now the
 * {@link ModEnergyNetContent#WIRE_START} component; every outcome keeps the original chat line.
 */
public class ItemWiring extends CompatItem {

    public ItemWiring(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();

        // A click on any cell of a folded pylon means its core.
        BlockPos core = MultiblockSurface.coreOfAny(level, pos);
        if (core != null) pos = core;

        BlockEntity te = level.getBlockEntity(pos);
        if (!(te instanceof TileEntityPylonBase second)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockPos start = ModEnergyNetContent.WIRE_START.get(stack).orElse(null);
        if (start == null) {
            ModEnergyNetContent.WIRE_START.set(stack, pos.immutable());
            if (!level.isClientSide()) player.sendSystemMessage(Component.literal("Wire start"));
        } else if (!level.isClientSide()) {
            if (level.getBlockEntity(start) instanceof TileEntityPylonBase first) {
                switch (TileEntityPylonBase.canConnect(first, second)) {
                    case 0 -> {
                        first.addConnection(pos);
                        second.addConnection(start);
                        player.sendSystemMessage(Component.literal("Wire end"));
                    }
                    case 1 -> player.sendSystemMessage(Component.literal("Wire error - Pylons are not the same type"));
                    case 2 -> player.sendSystemMessage(Component.literal("Wire error - Cannot connect to the same pylon"));
                    case 3 -> player.sendSystemMessage(Component.literal("Wire error - Pylon is too far away"));
                    default -> { }
                }
            } else {
                player.sendSystemMessage(Component.literal("Wire error"));
            }
            ModEnergyNetContent.WIRE_START.remove(stack);
        }

        player.swing(context.getHand());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        BlockPos start = ModEnergyNetContent.WIRE_START.get(stack).orElse(null);
        if (start != null) {
            tooltip.add(Component.literal("Wire start x: " + start.getX()));
            tooltip.add(Component.literal("Wire start y: " + start.getY()));
            tooltip.add(Component.literal("Wire start z: " + start.getZ()));
        } else {
            tooltip.add(Component.literal("Right-click poles to connect"));
        }
    }
}
