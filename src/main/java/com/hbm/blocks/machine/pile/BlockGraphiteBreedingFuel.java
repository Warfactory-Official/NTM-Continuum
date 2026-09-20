package com.hbm.blocks.machine.pile;

import com.hbm.blocks.ModNuclearMiscBlocks;
import com.hbm.items.ModNuclearMiscItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.hbm.tileentity.machine.pile.TileEntityPileBreedingFuel;

/** Pile Lithium Fuel (LEGACY): a lithium cell breeding towards tritium. */
@Deprecated
public class BlockGraphiteBreedingFuel extends BlockGraphiteDrilledTE {

    public BlockGraphiteBreedingFuel(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPileBreedingFuel(pos, state);
    }

    @Override
    public boolean onScrew(Level level, Player player, BlockPos pos, Direction side, Vec3 hit, InteractionHand hand, ToolType tool) {
        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);

            if (tool == ToolType.SCREWDRIVER && isChannelFace(state, side)) {
                // 1.7 keeps the full meta here (bit 8 included) — the drilled block ignores it anyway
                level.setBlock(pos, withChannel(ModNuclearMiscBlocks.BLOCK_GRAPHITE_DRILLED.get(), state).setValue(OUT, state.getValue(OUT)), 3);
                ejectItem(level, pos, side, new ItemStack(ModNuclearMiscItems.PILE_ROD_LITHIUM.get()));
            }

            if (tool == ToolType.HAND_DRILL && level.getBlockEntity(pos) instanceof TileEntityPileBreedingFuel pile) {
                player.sendSystemMessage(Component.literal("CP1 FUEL ASSEMBLY " + pos.getX() + " " + pos.getY() + " " + pos.getZ()).withStyle(ChatFormatting.GOLD));
                player.sendSystemMessage(Component.literal("DEPLETION: " + pile.progress + "/" + TileEntityPileBreedingFuel.maxProgress).withStyle(ChatFormatting.YELLOW));
                player.sendSystemMessage(Component.literal("FLUX: " + pile.lastNeutrons).withStyle(ChatFormatting.YELLOW));
            }
        }
        return true;
    }

    @Override
    protected ItemStack getInsertedItem() {
        return new ItemStack(ModNuclearMiscItems.PILE_ROD_LITHIUM.get());
    }
}
