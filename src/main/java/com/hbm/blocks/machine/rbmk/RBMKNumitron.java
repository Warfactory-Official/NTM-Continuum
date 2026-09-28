package com.hbm.blocks.machine.rbmk;

import com.hbm.api.block.IToolable;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKNumitron;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** 1.7 {@code RBMKNumitron}: a Redstone-over-Radio panel configured through a screwdriver-opened screen. */
public class RBMKNumitron extends RBMKMiniPanelBase implements IToolable, TickingBlock {
    /** Set by the client registrations; a server never opens the screen. */
    public static Consumer<BlockEntityRBMKNumitron> OPEN_SCREEN = panel -> {};

    public RBMKNumitron(Properties props) { super(props); }

    @Override public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityRBMKNumitron panel) OPEN_SCREEN.accept(panel);
        return true;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BlockEntityRBMKNumitron(pos, state); }
}
