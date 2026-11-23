package com.hbm.items.tool;

import com.hbm.blocks.machine.MachineSolarBoiler;
import com.hbm.blocks.machine.SolarMirror;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.tileentity.machine.TileEntitySolarMirror;
import dev.golem.core.world.CompatItem;
import dev.golem.core.world.CompatTooltipContext;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;
import com.hbm.items.ModDataComponents;

/** 1.7 {@code ItemMirrorTool}: links heliostats to a solar boiler within 100 blocks and at 45° or steeper. */
public class ItemMirrorTool extends CompatItem {

    public ItemMirrorTool(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    protected void appendCompatHoverText(ItemStack stack, CompatTooltipContext context,
                                         List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.hbm.mirror_tool.desc.0").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("item.hbm.mirror_tool.desc.1").withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (level.getBlockState(clicked).getBlock() instanceof MachineSolarBoiler) {
            BlockPos core = MultiblockSurface.coreOfAny(level, clicked);
            if (core != null && !level.isClientSide()) {
                // The mirror aims at one block above the boiler core, then resolves tY - 1.
                ModDataComponents.MIRROR_TARGET.set(stack, core.above());
                message(player, "item.hbm.mirror_tool.linked", ChatFormatting.YELLOW);
            }
            return InteractionResult.SUCCESS;
        }

        if (level.getBlockState(clicked).getBlock() instanceof SolarMirror
                && ModDataComponents.MIRROR_TARGET.has(stack)) {
            if (!level.isClientSide()
                    && level.getBlockEntity(clicked) instanceof TileEntitySolarMirror mirror) {
                BlockPos target = ModDataComponents.MIRROR_TARGET.getOr(stack, BlockPos.ZERO);
                int dx = clicked.getX() - target.getX();
                int dy = clicked.getY() - target.getY();
                int dz = clicked.getZ() - target.getZ();
                boolean withinReach = Math.sqrt(dx * dx + dy * dy + dz * dz) <= 100D;
                boolean withinAngle = dx * dx + dz * dz <= dy * dy;
                if (!withinReach) message(player, "item.hbm.mirror_tool.reach", ChatFormatting.RED);
                else if (!withinAngle) message(player, "item.hbm.mirror_tool.angle", ChatFormatting.RED);
                else mirror.setTarget(target);
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }

    private static void message(Player player, String key, ChatFormatting colour) {
        if (player != null) player.sendSystemMessage(Component.translatable(key).withStyle(colour));
    }
}
