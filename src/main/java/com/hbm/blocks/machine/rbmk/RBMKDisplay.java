package com.hbm.blocks.machine.rbmk;

import com.hbm.api.block.IToolable;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKDisplay;
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

/** 1.7 {@code RBMKDisplay}: linked with the RBMK console linking device, turned with a screwdriver. */
public class RBMKDisplay extends RBMKMiniPanelBase implements IToolable, TickingBlock {
    public RBMKDisplay(Properties props) { super(props); }

    @Override public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityRBMKDisplay display) display.rotate();
        return true;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BlockEntityRBMKDisplay(pos, state); }
}
