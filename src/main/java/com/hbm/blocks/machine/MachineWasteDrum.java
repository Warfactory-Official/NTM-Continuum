package com.hbm.blocks.machine;

import com.hbm.platform.Services;
import com.hbm.tileentity.ModBlockEntities;
import com.hbm.tileentity.machine.TileEntityWasteDrum;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Water-cooled storage for hot ZIRNOX spent fuel. */
public class MachineWasteDrum extends Block implements EntityBlock {

    public MachineWasteDrum(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityWasteDrum(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.MACHINE_WASTE_DRUM.get(), TileEntityWasteDrum::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> actual, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == actual ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            Services.PLATFORM.openMenu(serverPlayer, provider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    //? if >=26.1 {
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos,
                                               boolean movedByPiston) {
    //?} else {
    /*protected void onRemove(BlockState state, net.minecraft.world.level.Level hbm$level, BlockPos pos, BlockState hbm$newState, boolean movedByPiston) {
        if (!(hbm$level instanceof ServerLevel level)) return;
    *///?}
        if (level.getBlockEntity(pos) instanceof TileEntityWasteDrum drum) {
            Containers.dropContents(level, pos, drum);
        }
        //? if >=26.1 {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        //?} else {
        /*super.onRemove(state, hbm$level, pos, hbm$newState, movedByPiston);
        *///?}
    }
}
