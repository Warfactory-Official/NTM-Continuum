package com.hbm.blocks.generic;

import com.hbm.api.block.IToolable;
import com.hbm.tileentity.deco.TileEntityPartEmitter;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** 1.7 {@code PartEmitter} (part_emitter): an iron cube ({@code part_top}) whose effect the hand drill cycles. */
public class PartEmitter extends CompatBlock implements TickingBlock, IToolable {

    public PartEmitter(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPartEmitter(pos, state);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand,
                           ToolType tool) {
        if (tool == ToolType.HAND_DRILL && level.getBlockEntity(pos) instanceof TileEntityPartEmitter te) {
            te.effect = (te.effect + 1) % TileEntityPartEmitter.effectCount;
            te.setChanged();
            return true;
        }
        return false;
    }

    public static void addInformation(List<Component> list) {
        list.add(Component.literal("Use hand drill to cycle special effects").withStyle(ChatFormatting.GOLD));
    }
}
