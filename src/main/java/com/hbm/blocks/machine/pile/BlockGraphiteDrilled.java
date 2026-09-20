package com.hbm.blocks.machine.pile;

import com.hbm.blocks.machine.rbmk.ModRBMKBlocks;
import com.hbm.items.ModFuelItems;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.sound.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The empty drilled channel: rods (and the aluminium shroud) go in by hand through the channel face. */
@Deprecated
public class BlockGraphiteDrilled extends BlockGraphiteDrilledBase {

    public BlockGraphiteDrilled(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (held.isEmpty()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        Direction side = hit.getDirection();
        if (!isChannelFace(state, side)) return InteractionResult.TRY_WITH_EMPTY_HAND;

        BlockState placed = checkInteractions(held);
        if (placed != null) {
            return place(level, pos, player, held, placed.setValue(AXIS, state.getValue(AXIS)).setValue(ALUMINUM, state.getValue(ALUMINUM)));
        }
        if (!state.getValue(ALUMINUM)) {
            // shitty workaround: only the aluminium shell shrouds the channel
            if (isAluminumShell(held)) return place(level, pos, player, held, state.setValue(ALUMINUM, true));
            if (held.getItem() == ModFuelItems.ingot_graphite.get()) {
                return place(level, pos, player, held, ModRBMKBlocks.block_graphite.get().defaultBlockState());
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    private InteractionResult place(Level level, BlockPos pos, Player player, ItemStack held, BlockState placed) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        held.shrink(1);
        level.setBlock(pos, placed, 3);
        level.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (tool != ToolType.SCREWDRIVER) return false;

        BlockState state = level.getBlockState(pos);
        if (!level.isClientSide() && isChannelFace(state, side) && state.getValue(ALUMINUM)) {
            level.setBlock(pos, ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get().defaultBlockState()
                    .setValue(AXIS, state.getValue(AXIS)), 3);
            level.playSound(null, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                    ModSounds.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 0.85F);
            ejectItem(level, pos, side, aluminumShell());
        }
        return true;
    }
}
