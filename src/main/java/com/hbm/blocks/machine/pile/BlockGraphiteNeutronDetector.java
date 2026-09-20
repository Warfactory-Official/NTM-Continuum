package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.items.ModNuclearMiscItems;
import com.hbm.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.machine.pile.TileEntityPileNeutronDetector;

/** Pile Neutron Detector (LEGACY): trips the control rods in line with it once its flux limit is crossed. */
@Deprecated
public class BlockGraphiteNeutronDetector extends BlockGraphiteDrilledTE {

    public BlockGraphiteNeutronDetector(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPileNeutronDetector(pos, state);
    }

    public void triggerRods(Level level, BlockPos pos) {
        BlockState oldState = level.getBlockState(pos);
        BlockState newState = oldState.setValue(OUT, !oldState.getValue(OUT)); // toggle bit #4
        level.setBlock(pos, newState, 3);

        level.playSound(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                ModSounds.TECH_BLEEP.get(), SoundSource.BLOCKS, 0.02F, 1.0F);

        // 1.7 ForgeDirection.getOrientation(pureMeta * 2): the negative side of the channel axis
        Direction dir = Direction.fromAxisAndDirection(oldState.getValue(AXIS), Direction.AxisDirection.NEGATIVE);
        BlockState rodOld = ModNuclearMiscBlocks.BLOCK_GRAPHITE_ROD.get().defaultBlockState()
                .setValue(AXIS, oldState.getValue(AXIS)).setValue(ALUMINUM, oldState.getValue(ALUMINUM)).setValue(OUT, oldState.getValue(OUT));
        BlockState rodNew = rodOld.setValue(OUT, newState.getValue(OUT));

        for (int i = -1; i <= 1; i += 1) {
            BlockPos ipos = pos.relative(dir, i);
            while (level.getBlockState(ipos) == rodOld) {
                level.setBlock(ipos, rodNew, 3);
                ipos = ipos.relative(dir, i);
            }
        }
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);

            if (tool == ToolType.SCREWDRIVER) {
                if (!player.isShiftKeyDown()) {
                    if (isChannelFace(state, side)) {
                        level.setBlock(pos, withChannel(ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get(), state), 3);
                        ejectItem(level, pos, side, getInsertedItem());
                    }
                } else if (level.getBlockEntity(pos) instanceof TileEntityPileNeutronDetector pile) {
                    player.sendSystemMessage(Component.literal("CP1 FUEL ASSEMBLY " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.GOLD));
                    player.sendSystemMessage(Component.literal("FLUX: " + pile.lastNeutrons + "/" + pile.maxNeutrons).withStyle(ChatFormatting.YELLOW));
                }
            }

            if (tool == ToolType.DEFUSER && level.getBlockEntity(pos) instanceof TileEntityPileNeutronDetector pile) {
                if (player.isShiftKeyDown()) {
                    if (pile.maxNeutrons > 1) pile.maxNeutrons--;
                } else {
                    pile.maxNeutrons++;
                }
                pile.setChanged();
            }
        }
        return true;
    }

    @Override
    protected ItemStack getInsertedItem() {
        return new ItemStack(ModNuclearMiscItems.PILE_ROD_DETECTOR.get());
    }
}
