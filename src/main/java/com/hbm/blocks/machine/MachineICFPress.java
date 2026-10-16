package com.hbm.blocks.machine;

import com.hbm.blocks.StandardInfo;
import com.hbm.tileentity.machine.TileEntityICFPress;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.BlockTooltip;
import dev.golem.core.world.CompatBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The ICF fuel pellet maker (1.7 {@code MachineICFPress}): a plain cube, top and bottom {@code machine_icf_press_top},
 * sides {@code machine_icf_press_side}. Right-click opens the menu; sneaking falls through. Its contents spill when
 * it is broken ({@code breakBlock}), which the block entity's removal hook does here.
 */
public class MachineICFPress extends CompatBlock implements TickingBlock, BlockTooltip {

    public MachineICFPress(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityICFPress(pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof TileEntityICFPress press) {
            Menus.openWithPos(serverPlayer, press, pos);
        }
        return InteractionResult.CONSUME;
    }

    /** 1.7 prints these lines with no Shift gate. */
    @Override
    public void appendBlockTooltip(ItemStack stack, List<Component> tooltip, TooltipFlag flag) {
        StandardInfo.addLines(tooltip, getDescriptionId() + ".desc");
    }
}
