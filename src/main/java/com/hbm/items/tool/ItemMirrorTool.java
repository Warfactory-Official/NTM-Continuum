package com.hbm.items.tool;

import com.hbm.blocks.machine.solar.MachineSolarBoiler;
import com.hbm.blocks.machine.solar.SolarMirror;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.items.solar.SolarDataComponents;
import com.hbm.tileentity.machine.solar.TileEntitySolarMirror;
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

/** The old solar-boiler/mirror coordinate linker. */
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
        boolean shifting = player != null && player.isShiftKeyDown();

        if (level.getBlockState(clicked).getBlock() instanceof MachineSolarBoiler boilerBlock) {
            if (!shifting) return super.useOn(context);
            BlockPos core = MultiblockSurface.coreOfAny(level, clicked);
            if (core != null && !level.isClientSide()) {
                // The legacy mirror aims at one block above the boiler core, then resolves tY - 1.
                SolarDataComponents.MIRROR_TARGET.set(stack, core.above());
                if (player != null) {
                    player.sendSystemMessage(Component.translatable("item.hbm.mirror_tool.linked")
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
            return InteractionResult.SUCCESS;
        }

        if (!shifting && level.getBlockState(clicked).getBlock() instanceof SolarMirror
                && SolarDataComponents.MIRROR_TARGET.has(stack)) {
            if (!level.isClientSide()
                    && level.getBlockEntity(clicked) instanceof TileEntitySolarMirror mirror) {
                BlockPos target = SolarDataComponents.MIRROR_TARGET.getOr(stack, BlockPos.ZERO);
                double dx = clicked.getX() - target.getX();
                double dy = clicked.getY() - target.getY();
                double dz = clicked.getZ() - target.getZ();
                if (Math.sqrt(dx * dx + dy * dy + dz * dz) < 25D) {
                    mirror.setTarget(target);
                }
            }
            return InteractionResult.SUCCESS;
        }

        return super.useOn(context);
    }
}
