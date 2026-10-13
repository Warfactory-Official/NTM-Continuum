package com.hbm.blocks.machine;

import com.hbm.tileentity.machine.storage.TileEntitySoyuzCapsule;
import dev.golem.core.gui.Menus;
import dev.golem.core.registry.TickingBlock;
import dev.golem.core.world.CompatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 1.7 {@code SoyuzCapsule}, the "Cargo Landing Capsule": what a Soyuz on a lander mission drops. The capsule
 * entity turns into this block where it lands, holding the eighteen payload stacks and the spent rocket. A plain
 * click opens its storage; breaking it spills everything.
 *
 * <p>1.7 metadata 3 drew the rusted skin; only world generation placed that. {@link #RUSTED} is that bit.
 */
public class SoyuzCapsule extends CompatBlock implements TickingBlock {

    public static final BooleanProperty RUSTED = BooleanProperty.create("rusted");

    public SoyuzCapsule(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(RUSTED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RUSTED);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntitySoyuzCapsule(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                 BlockHitResult hit) {
        if (player.isShiftKeyDown()) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof TileEntitySoyuzCapsule capsule) {
            Menus.openWithPos(serverPlayer, capsule, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
