package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IToolable;
import com.hbm.items.ModFuelItems;
import com.hbm.blocks.ModNuclearMiscBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The graphite storage block, given the 1.12 {@code BlockGraphite} hand-drill interaction that turns it
 * into a drilled pile channel along the clicked axis (1.7 upstream dropped that path together with the
 * legacy pile; 1.12 is the reference for it).
 */
public class BlockGraphite extends dev.golem.core.world.CompatBlock implements IToolable {

    public BlockGraphite(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.HAND_DRILL) return false;

        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);
            level.setBlock(pos, ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()
                    .setValue(BlockGraphiteDrilledBase.AXIS, BlockGraphiteDrilledBase.axisOf(side)), 3);
            level.levelEvent(2001, pos, Block.getId(state));
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    getSoundType(state).getStepSound(), SoundSource.BLOCKS,
                    (getSoundType(state).getVolume() + 1.0F) / 2.0F, getSoundType(state).getPitch() * 0.8F);
            BlockGraphiteDrilledBase.ejectItem(level, pos, side, new ItemStack(ModFuelItems.powder_coal.get()));
        }
        return true;
    }
}
