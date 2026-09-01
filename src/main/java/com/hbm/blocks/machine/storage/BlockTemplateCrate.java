package com.hbm.blocks.machine.storage;

import com.hbm.tileentity.machine.storage.TileEntityCrateTemplate;
import com.hbm.tileentity.machine.storage.TemplateCrateLayout;
import dev.golem.core.world.CarriedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

/** Ordinary horizontal-facing template crate block; central registration supplies the BE/menu types. */
public class BlockTemplateCrate extends CarriedBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final Supplier<? extends BlockEntityType<TileEntityCrateTemplate>> blockEntityType;
    private final Supplier<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerCrateTemplate>> menuType;

    public BlockTemplateCrate(Properties properties) {
        this(properties, TemplateCrateLayout::blockEntityType, TemplateCrateLayout::menuType);
    }

    public BlockTemplateCrate(Properties properties,
                              Supplier<? extends BlockEntityType<TileEntityCrateTemplate>> blockEntityType,
                              Supplier<net.minecraft.world.inventory.MenuType<com.hbm.inventory.container.ContainerCrateTemplate>> menuType) {
        super(properties);
        this.blockEntityType = blockEntityType;
        this.menuType = menuType;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityCrateTemplate(blockEntityType, menuType, pos, state);
    }

    @Override
    protected InteractionResult onUseWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                  BlockHitResult hit) {
        if (!level.isClientSide() && !player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MenuProvider provider) {
            dev.golem.core.gui.Menus.openWithPos(serverPlayer, provider, pos);
        }
        return level.isClientSide() || !player.isShiftKeyDown()
                ? InteractionResult.SUCCESS : InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

}
