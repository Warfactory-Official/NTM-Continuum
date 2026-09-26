package com.hbm.items.tool;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.blocks.machine.rbmk.RBMKConsole;
import com.hbm.blocks.machine.rbmk.RBMKCraneConsole;
import dev.golem.core.multiblock.MultiblockSurface;
import com.hbm.items.ModDataComponents;
import com.hbm.tileentity.machine.rbmk.BlockEntityCraneConsole;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKConsole;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/** RBMK linker (mallet): links a reactor core to a console or crane console. */
public class ItemRBMKTool extends Item {
    public ItemRBMKTool(Properties properties) { super(properties); }

    private static void message(Player player, String key) {
        if (player != null) player.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();

        if (block instanceof RBMKBase) {
            if (!level.isClientSide()) {
                BlockPos core = MultiblockSurface.coreOfAny(level, pos);
                if (core != null) {
                    ModDataComponents.RBMK_TOOL_TARGET.set(stack, core.asLong());
                    message(player, "item.hbm.rbmk_tool.linked");
                }
            }
            return InteractionResult.SUCCESS;
        }

        BlockPos consoleCore = block instanceof RBMKConsole || block instanceof RBMKCraneConsole
                ? pos : MultiblockSurface.coreOfCell(level, pos);
        if (consoleCore == null) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Block coreBlock = level.getBlockState(consoleCore).getBlock();
        if (!(coreBlock instanceof RBMKConsole) && !(coreBlock instanceof RBMKCraneConsole)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!level.isClientSide()) {
            Long target = ModDataComponents.RBMK_TOOL_TARGET.get(stack).orElse(null);
            BlockEntity blockEntity = level.getBlockEntity(consoleCore);
            if (target != null) {
                BlockPos targetPos = BlockPos.of(target);
                if (blockEntity instanceof BlockEntityRBMKConsole console) {
                    console.setTarget(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                    message(player, "item.hbm.rbmk_tool.set");
                } else if (blockEntity instanceof BlockEntityCraneConsole crane) {
                    crane.setTarget(targetPos.getX(), targetPos.getY(), targetPos.getZ());
                    message(player, "item.hbm.rbmk_tool.set");
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
