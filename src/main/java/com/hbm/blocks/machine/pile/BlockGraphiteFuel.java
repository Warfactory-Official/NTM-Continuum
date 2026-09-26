package com.hbm.blocks.machine.pile;

import com.hbm.api.block.IBlowable;
import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.items.ModNuclearMiscItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.machine.pile.TileEntityPileFuel;

/** Pile Fuel (LEGACY): a uranium rod in a channel; {@code OUT} means it has bred rich in Pu-239. */
@Deprecated
public class BlockGraphiteFuel extends BlockGraphiteDrilledTE implements IBlowable {

    public BlockGraphiteFuel(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPileFuel(pos, state);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int onAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof TileEntityPileFuel pile)) return 0;
        return Mth.clamp((pile.progress * 15) / (TileEntityPileFuel.maxProgress - 1000), 0, 15);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);

            if (tool == ToolType.SCREWDRIVER && isChannelFace(state, side)) {
                level.setBlock(pos, withChannel(ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get(), state), 3);
                ejectItem(level, pos, side, getInsertedItem(state));
            }

            if (tool == ToolType.HAND_DRILL && level.getBlockEntity(pos) instanceof TileEntityPileFuel pile) {
                player.sendSystemMessage(Component.literal("CP1 FUEL ASSEMBLY " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.literal("HEAT: " + pile.heat + "/" + TileEntityPileFuel.maxHeat).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("DEPLETION: " + pile.progress + "/" + TileEntityPileFuel.maxProgress).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("FLUX: " + pile.lastNeutrons).withStyle(ChatFormatting.YELLOW));
                if (state.getValue(OUT)) {
                    player.sendSystemMessage(Component.literal("PU-239 RICH").withStyle(ChatFormatting.DARK_GREEN));
                }
            }
        }
        return true;
    }

    @Override
    protected ItemStack getInsertedItem(BlockState state) {
        return new ItemStack(state.getValue(OUT) ? ModNuclearMiscItems.PILE_ROD_PU239.get() : ModNuclearMiscItems.PILE_ROD_URANIUM.get());
    }

    @Override
    protected ItemStack getInsertedItem() {
        return new ItemStack(ModNuclearMiscItems.PILE_ROD_URANIUM.get());
    }

    @Override
    public void applyFan(Level level, BlockPos pos, Direction direction, int distance) {
        if (level.getBlockEntity(pos) instanceof TileEntityPileFuel pile) pile.heat -= pile.heat * 0.025;
    }
}
