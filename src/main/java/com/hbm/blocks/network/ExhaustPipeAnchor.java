package com.hbm.blocks.network;

import com.hbm.interfaces.ILookOverlay;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.tileentity.network.TileEntityPipeExhaustAnchor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** 1.7 {@code ExhaustPipeAnchor}: the pipe anchor of the exhaust network, carrying the three smokes. */
public class ExhaustPipeAnchor extends FluidPipeAnchor {

    public ExhaustPipeAnchor(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityPipeExhaustAnchor(pos, state);
    }

    /** 1.7 has no click action on it: an identifier does nothing. */
    @Override
    protected InteractionResult onUseItem(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack, BlockHitResult hit) {
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public ILookOverlay.Data getLookOverlay(Level level, BlockPos pos, Player player) {
        List<String> text = List.of(Fluids.SMOKE.getDisplayNameString(), Fluids.SMOKE_LEADED.getDisplayNameString(),
                Fluids.SMOKE_POISON.getDisplayNameString());
        return new ILookOverlay.Data(getName().getString(), 0xffff00, 0x404000, text);
    }
}
