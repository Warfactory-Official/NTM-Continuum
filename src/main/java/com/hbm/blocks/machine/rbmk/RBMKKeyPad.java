package com.hbm.blocks.machine.rbmk;

import com.hbm.api.block.IToolable;
import com.hbm.tileentity.machine.rbmk.BlockEntityRBMKKeyPad;
import dev.golem.core.registry.TickingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/** 1.7 {@code RBMKKeyPad}: a Redstone-over-Radio panel configured through a screwdriver-opened screen. */
public class RBMKKeyPad extends RBMKMiniPanelBase implements IToolable, TickingBlock {
    /** Set by the client registrations; a server never opens the screen. */
    public static Consumer<BlockEntityRBMKKeyPad> OPEN_SCREEN = panel -> {};

    public RBMKKeyPad(Properties props) { super(props); }

    @Override public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;
        if (level.isClientSide() && level.getBlockEntity(pos) instanceof BlockEntityRBMKKeyPad panel) OPEN_SCREEN.accept(panel);
        return true;
    }

    /**
     * 1.7 {@code onBlockActivated}: pressing the front face picks the unit under the cursor. The client
     * always answers success (so a held screwdriver only configures while sneaking, as in 1.7); the server
     * declines a sneak-click and a click on a disabled unit.
     */
    @Override protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        double hitX = hit.getLocation().x - pos.getX(), hitY = hit.getLocation().y - pos.getY(), hitZ = hit.getLocation().z - pos.getZ();
        Direction side = hit.getDirection();
        if (hitX != 0 && hitX != 1 && hitZ != 0 && hitZ != 1 && side != Direction.DOWN && side != Direction.UP
                && level.getBlockEntity(pos) instanceof BlockEntityRBMKKeyPad tile) {
            Direction facing = state.getValue(FACING);
            int indexHit = 0;
            if (facing == Direction.NORTH && hitX < 0.5) indexHit = 1;
            if (facing == Direction.SOUTH && hitX > 0.5) indexHit = 1;
            if (facing == Direction.WEST && hitZ > 0.5) indexHit = 1;
            if (facing == Direction.EAST && hitZ < 0.5) indexHit = 1;
            if (hitY < 0.5) indexHit += 2;
            if (!tile.keys[indexHit].active) return InteractionResult.TRY_WITH_EMPTY_HAND;
            tile.keys[indexHit].click();
        }
        return InteractionResult.SUCCESS;
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BlockEntityRBMKKeyPad(pos, state); }
}
